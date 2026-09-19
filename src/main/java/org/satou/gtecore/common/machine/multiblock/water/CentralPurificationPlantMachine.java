package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Arrays;

/**
 * 中枢净化水厂：整条净水产线的控制与供电核心（GTNH / GTO 同款定位）。
 *
 * <p>
 * 行为：
 * <ul>
 *     <li>用<b>数据棒</b>把一级/二级/三级净化单元绑定到本机（右键单元写入中枢坐标，或先复制中枢坐标再右键单元）；</li>
 *     <li>把能源仓里的电力实时转发给所有已连接且已成型的净化单元；</li>
 *     <li>在 GUI 中统一设定并行度，并下发给全部已连接单元；</li>
 *     <li>未连接的净化单元无法开机。</li>
 * </ul>
 */
public class CentralPurificationPlantMachine extends WorkableElectricMultiblockMachine
                                            implements IDataStickInteractable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CentralPurificationPlantMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final int MIN_PARALLEL = 1;
    public static final int MAX_PARALLEL = 65536;

    private static final String NBT_LINKED_UNITS = "gteWaterLinkedUnits";

    /** 下发给所有已连接净化单元的并行度。 */
    @Persisted
    @DescSynced
    @Getter
    private int parallel = 8;

    /** 已连接且已成型的净化单元数量（仅用于显示）。 */
    @DescSynced
    @Getter
    private int linkedUnits = 0;

    /** 上一秒实际转发出去的 EU（仅用于显示）。 */
    @DescSynced
    @Getter
    private long transferredPerSecond = 0;

    private final LongSet unitLinks = new LongOpenHashSet();
    private long transferredThisSecond = 0;
    private int nextUnitIndex;
    private int tierOneUnits;
    private int tierTwoUnits;
    private int tierThreeUnits;

    @Nullable
    private TickableSubscription tickSubs;

    public CentralPurificationPlantMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ******* 生命周期 *******//
    //////////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            tickSubs = subscribeServerTick(tickSubs, this::plantServerTick);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        refreshLinkedUnits();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        linkedUnits = 0;
        transferredThisSecond = 0;
        transferredPerSecond = 0;
    }

    //////////////////////////////////////
    // ******* 单元互联 *******//
    //////////////////////////////////////

    public void addUnit(@NotNull BlockPos pos) {
        if (unitLinks.add(pos.asLong())) {
            onChanged();
            refreshLinkedUnits();
        }
    }

    public void removeUnit(@NotNull BlockPos pos) {
        if (unitLinks.remove(pos.asLong())) {
            onChanged();
            refreshLinkedUnits();
        }
    }

    public boolean hasUnit(BlockPos pos) {
        return unitLinks.contains(pos.asLong());
    }

    /** Retain unloaded addresses, prune only loaded stale or one-sided connections. */
    public void refreshLinkedUnits() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        int count = 0;
        tierOneUnits = tierTwoUnits = tierThreeUnits = 0;
        boolean changed = false;
        LongIterator iterator = unitLinks.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = BlockPos.of(iterator.nextLong());
            if (!level.hasChunkAt(pos)) continue;
            if (MetaMachine.getMachine(level, pos) instanceof LinkedPurificationUnitMachine unit &&
                    getPos().equals(unit.getPlantPos())) {
                if (unit.isFormed()) {
                    count++;
                    switch (unit.getUnitTier()) {
                        case GTValues.EV -> tierOneUnits++;
                        case GTValues.LuV -> tierTwoUnits++;
                        case GTValues.ZPM -> tierThreeUnits++;
                    }
                }
            } else {
                iterator.remove();
                changed = true;
            }
        }
        linkedUnits = count;
        if (changed) onChanged();
    }

    public void disconnectAll() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        for (long packed : unitLinks.toLongArray()) {
            BlockPos pos = BlockPos.of(packed);
            if (level.hasChunkAt(pos) && MetaMachine.getMachine(level, pos) instanceof LinkedPurificationUnitMachine unit &&
                    getPos().equals(unit.getPlantPos())) unit.bindToPlant(null);
        }
        // Unloaded units also fail the reverse-membership check when they return.
        unitLinks.clear();
        refreshLinkedUnits();
        onChanged();
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (isRemote()) return InteractionResult.PASS;
        BlockPos pos = WaterPurificationLink.read(dataStick, getLevel(), "unit");
        if (pos == null) return InteractionResult.PASS;

        if (MetaMachine.getMachine(getLevel(), pos) instanceof LinkedPurificationUnitMachine unit &&
                unit.bindToPlant(getPos())) {
            refreshLinkedUnits();
            player.displayClientMessage(Component.translatable("com.gtecore.chat.water_plant.bound")
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("com.gtecore.chat.water_plant.bind_failed")
                .withStyle(ChatFormatting.RED), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (isRemote()) return InteractionResult.SUCCESS;
        WaterPurificationLink.write(dataStick, getLevel(), getPos(), "plant");
        player.displayClientMessage(Component.translatable("com.gtecore.chat.water_plant.copied")
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    //////////////////////////////////////
    // ******* 供电与并行 *******//
    //////////////////////////////////////

    public void setParallel(int parallel) {
        this.parallel = Mth.clamp(parallel, MIN_PARALLEL, MAX_PARALLEL);
        onChanged();
    }

    private void plantServerTick() {
        if (getLevel() == null || getLevel().isClientSide) return;

        if (getOffsetTimer() % 20 == 0) {
            refreshLinkedUnits();
            if (isWorkingEnabled()) {
                getRecipeLogic().setStatus(isFormed() && transferredThisSecond > 0 ?
                        RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
            }
            transferredPerSecond = transferredThisSecond;
            transferredThisSecond = 0;
        }

        if (!isFormed() || !isWorkingEnabled()) return;
        transferredThisSecond += transferEnergyToUnits();
    }

    /**
     * 把能源仓中的电力转发给已连接的净化单元。
     *
     * @return 本次转发出去的 EU
     */
    private long transferEnergyToUnits() {
        EnergyContainerList hatches = this.energyContainer;
        Level level = getLevel();
        if (hatches == null || level == null) return 0L;

        long[] links = unitLinks.toLongArray();
        if (links.length == 0) return 0L;
        Arrays.sort(links);
        int start = Math.floorMod(nextUnitIndex, links.length);
        nextUnitIndex = (start + 1) % links.length;
        long moved = 0L;
        // Rotate first service each tick so a busy low-index unit cannot starve its siblings.
        for (int offset = 0; offset < links.length; offset++) {
            BlockPos pos = BlockPos.of(links[(start + offset) % links.length]);
            if (!level.hasChunkAt(pos) ||
                    !(MetaMachine.getMachine(level, pos) instanceof LinkedPurificationUnitMachine unit) ||
                    !unit.isFormed() || !unit.isWorkingEnabled() || unit.getPlant() != this) continue;
            long voltage = unit.getLinkVoltage();
            long amps = Math.min(unit.getLinkAmperage(), Math.min(hatches.getEnergyStored() / voltage,
                    (unit.getLinkedEnergyCapacity() - unit.getStoredLinkedEnergy()) / voltage));
            // Insufficient voltage for one unit must not prevent cheaper units receiving power.
            if (amps <= 0) continue;
            long removed = -hatches.changeEnergy(-amps * voltage);
            long accepted = unit.acceptLinkedEnergy(this, removed / voltage) * voltage;
            // Debit the source before crediting the destination, and refund any rejection.
            if (removed > accepted) hatches.changeEnergy(removed - accepted);
            moved += accepted;
        }
        return moved;
    }

    //////////////////////////////////////
    // ********** 持久化 ***********//
    //////////////////////////////////////

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (!forDrop) tag.putLongArray(NBT_LINKED_UNITS, unitLinks.toLongArray());
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        parallel = Mth.clamp(parallel, MIN_PARALLEL, MAX_PARALLEL);
        unitLinks.clear();
        for (long packed : tag.getLongArray(NBT_LINKED_UNITS)) {
            unitLinks.add(packed);
        }
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 300, 234);
        group.setBackground(new GuiTextureGroup(new ColorRectTexture(0xFF091724),
                new ColorBorderTexture(1, 0xFF267A93)));
        group.addWidget(new WidgetGroup(6, 6, 288, 23).setBackground(new ColorRectTexture(0xFF123C50)));
        group.addWidget(new LabelWidget(12, 13, "com.gtecore.gui.water_plant.dashboard").setTextColor(0xFF8CEBF2));
        group.addWidget(new ComponentPanelWidget(12, 37, this::addDisplayText)
                .textSupplier(isRemote() ? null : this::addDisplayText).setMaxWidthLimit(274));
        group.addWidget(new ProgressWidget(this::energyFill, 12, 129, 276, 5)
                .setProgressTexture(new ColorRectTexture(0xFF173449), new ColorRectTexture(0xFF35CFDD)));
        group.addWidget(new WidgetGroup(8, 145, 284, 31).setBackground(new ColorRectTexture(0xFF102C3D)));
        group.addWidget(new LabelWidget(14, 157, "com.gtecore.gui.water_plant.parallel").setTextColor(0xFFB7DCE8));
        group.addWidget(new IntInputWidget(190, 151, 94, 20, this::getParallel, this::setParallel)
                .setMin(MIN_PARALLEL).setMax(MAX_PARALLEL));
        group.addWidget(new ComponentPanelWidget(12, 184, List.of(
                Component.translatable("com.gtecore.gui.water_plant.link_instruction").withStyle(ChatFormatting.GRAY)))
                .setMaxWidthLimit(272));
        group.addWidget(new ButtonWidget(188, 210, 100, 18,
                new GuiTextureGroup(new ColorRectTexture(0xFF163E50), new ColorBorderTexture(1, 0xFF42869B),
                        new TextTexture("com.gtecore.gui.water_plant.disconnect_all")),
                click -> { if (!click.isRemote) disconnectAll(); }));
        return group;
    }

    private double energyFill() {
        return energyContainer == null || energyContainer.getEnergyCapacity() == 0 ? 0 :
                (double) energyContainer.getEnergyStored() / energyContainer.getEnergyCapacity();
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.add(Component.translatable(!isFormed() ? "com.gtecore.gui.water_plant.incomplete" :
                !isWorkingEnabled() ? "com.gtecore.gui.water_plant.disabled" :
                        "com.gtecore.gui.water_plant.online").withStyle(isFormed() && isWorkingEnabled() ?
                                ChatFormatting.AQUA : ChatFormatting.RED));
        textList.add(Component.translatable("com.gtecore.gui.water_plant.network", linkedUnits, unitLinks.size())
                .withStyle(ChatFormatting.WHITE));
        textList.add(Component.translatable("com.gtecore.gui.water_plant.tiers", tierOneUnits, tierTwoUnits, tierThreeUnits)
                .withStyle(ChatFormatting.AQUA));
        textList.add(Component.translatable("com.gtecore.tooltips.water_plant.throughput", transferredPerSecond)
                .withStyle(ChatFormatting.YELLOW));
        textList.add(Component.translatable("com.gtecore.gui.water_plant.storage",
                energyContainer == null ? 0 : energyContainer.getEnergyStored(),
                energyContainer == null ? 0 : energyContainer.getEnergyCapacity()).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("com.gtecore.gui.water_plant.parallel_scope", parallel)
                .withStyle(ChatFormatting.GOLD));
    }
}
