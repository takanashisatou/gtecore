package org.satou.gtecore.common.data.machines;

import appeng.api.config.Actionable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;

import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MEPatternBufferProxyPlusPartMachine extends MEBusPartPlusMachine
        implements IMachineLife, IDataStickInteractable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEPatternBufferProxyPlusPartMachine.class, MEBusPartPlusMachine.MANAGED_FIELD_HOLDER);

    @Getter
    private final ProxySlotRecipeHandlerPlus proxySlotRecipeHandler;

    @Persisted
    @Getter
    @DescSynced
    private @Nullable BlockPos bufferPos;

    private @Nullable MEPatternBufferPlusPartMachine buffer = null;
    @Persisted
    private KeyStorage internalBufferFluid;
    @Persisted
    private KeyStorage internalBufferItem;
    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        internalBufferItem = new KeyStorage();
        return MEPatternBufferOutputHandlers.items(this, internalBufferItem);
    }

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        internalBufferFluid = new KeyStorage();
        return MEPatternBufferOutputHandlers.fluids(this, internalBufferFluid);
    }

    public MEPatternBufferProxyPlusPartMachine(IMachineBlockEntity holder) {
        super(holder, IO.BOTH);
        proxySlotRecipeHandler = new ProxySlotRecipeHandlerPlus(this, MEPatternBufferPlusPartMachine.MAX_PATTERN_COUNT);
    }
    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel level) {
            level.getServer().tell(new TickTask(0, () -> {
                if (!isInValid()) setBuffer(bufferPos);
            }));
        }
    }
    @Override
    protected boolean shouldSubscribe() {
        return isWorkingEnabled() && isOnline() && (!internalBufferItem.storage.isEmpty() || !internalBufferFluid.storage.isEmpty());
    }
    @Override
    public void autoIO() {
        if (!this.shouldSyncME()) return;
        if (this.updateMEStatus()) {
            var grid = getMainNode().getGrid();
            if (grid != null && !internalBufferItem.isEmpty()) {
                internalBufferItem.insertInventory(grid.getStorageService().getInventory(), actionSource);
            }

            if (grid != null && !internalBufferFluid.isEmpty()) {
                internalBufferFluid.insertInventory(grid.getStorageService().getInventory(), actionSource);
            }
            this.updateInventorySubscription();
        }
    }
    @Override
    public List<RecipeHandlerList> getRecipeHandlers() {
        var slotHandlers = proxySlotRecipeHandler.getProxySlotHandlers();
        var superHandlers = super.getRecipeHandlers();
        List<RecipeHandlerList> allHandlers = new ArrayList<>(slotHandlers.size() + 1 + superHandlers.size());
        allHandlers.addAll(slotHandlers);
        allHandlers.add(proxySlotRecipeHandler.getProxySharedHandlerList());
        allHandlers.addAll(superHandlers);
        return allHandlers;
    }

    public void setBuffer(@Nullable BlockPos pos) {
        boolean bindingChanged = !java.util.Objects.equals(bufferPos, pos);
        if (buffer != null && bindingChanged) buffer.removeProxy(this);
        bufferPos = pos;
        var level = getLevel();
        MEPatternBufferPlusPartMachine resolved = null;
        if (level != null && pos != null && level.hasChunkAt(pos) &&
                MetaMachine.getMachine(level, pos) instanceof MEPatternBufferPlusPartMachine machine &&
                machine.isBufferAvailable()) {
            resolved = machine;
        }
        if (buffer != resolved) {
            if (buffer != null) buffer.unloadProxy(this);
            buffer = resolved;
            if (!isRemote()) {
                if (resolved == null) proxySlotRecipeHandler.clearProxy();
                else proxySlotRecipeHandler.updateProxy(resolved);
            }
        }
        if (buffer != null) buffer.addProxy(this);
        if (bindingChanged) onChanged();
    }

    @Nullable
    public MEPatternBufferPlusPartMachine getBuffer() {
        if (buffer == null || !buffer.isBufferAvailable()) setBuffer(bufferPos);
        return buffer;
    }

    @Override
    public void onUnload() {
        proxySlotRecipeHandler.clearProxy();
        if (buffer != null) buffer.unloadProxy(this);
        buffer = null;
        super.onUnload();
    }

    void onBufferUnload(MEPatternBufferPlusPartMachine machine) {
        if (buffer != machine) return;
        proxySlotRecipeHandler.clearProxy();
        machine.unloadProxy(this);
        buffer = null;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return getBuffer() != null;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        assert getBuffer() != null; // UI should never be able to be opened when buffer is null
        return getBuffer().createUI(entityPlayer);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onMachineRemoved() {
        var buf = getBuffer();
        if (buf != null) {
            buf.removeProxy(this);
            proxySlotRecipeHandler.clearProxy();
        }
        buffer = null;
        bufferPos = null;
        var grid = getMainNode().getGrid();
        if (grid != null && !internalBufferItem.isEmpty()) {
            for (var entry : internalBufferItem) {
                grid.getStorageService().getInventory().insert(entry.getKey(), entry.getLongValue(),
                        Actionable.MODULATE, actionSource);
            }
        }
        if (grid != null && !internalBufferFluid.isEmpty()) {
            for (var entry : internalBufferFluid) {
                grid.getStorageService().getInventory().insert(entry.getKey(), entry.getLongValue(),
                        Actionable.MODULATE, actionSource);
            }
        }
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (dataStick.hasTag()) {
            assert dataStick.getTag() != null;
            if (dataStick.getTag().contains("pos", Tag.TAG_INT_ARRAY)) {
                var posArray = dataStick.getOrCreateTag().getIntArray("pos");
                if (posArray.length != 3) return InteractionResult.PASS;
                var bufferPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                setBuffer(bufferPos);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
