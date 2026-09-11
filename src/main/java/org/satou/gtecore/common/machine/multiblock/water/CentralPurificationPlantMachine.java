package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
            refreshLinkedUnits();
        }
    }

    public void removeUnit(@NotNull BlockPos pos) {
        if (unitLinks.remove(pos.asLong())) {
            refreshLinkedUnits();
        }
    }

    /** 清理已消失的单元并刷新连接数量。 */
    public void refreshLinkedUnits() {
        Level level = getLevel();
        if (level == null) return;
        int count = 0;
        LongIterator iterator = unitLinks.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            if (MetaMachine.getMachine(level, BlockPos.of(packed)) instanceof LinkedPurificationUnitMachine unit) {
                if (unit.isFormed()) count++;
            } else {
                iterator.remove();
            }
        }
        this.linkedUnits = count;
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (isRemote()) return InteractionResult.PASS;
        BlockPos pos = readStickPos(dataStick);
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
        dataStick.getOrCreateTag().putIntArray("pos",
                new int[] { getPos().getX(), getPos().getY(), getPos().getZ() });
        player.displayClientMessage(Component.translatable("com.gtecore.chat.water_plant.copied")
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static BlockPos readStickPos(ItemStack dataStick) {
        if (!dataStick.hasTag() || !dataStick.getOrCreateTag().contains("pos", Tag.TAG_INT_ARRAY)) return null;
        int[] posArray = dataStick.getOrCreateTag().getIntArray("pos");
        if (posArray.length < 3) return null;
        return new BlockPos(posArray[0], posArray[1], posArray[2]);
    }

    //////////////////////////////////////
    // ******* 供电与并行 *******//
    //////////////////////////////////////

    public void setParallel(int parallel) {
        this.parallel = Mth.clamp(parallel, MIN_PARALLEL, MAX_PARALLEL);
    }

    private void plantServerTick() {
        if (getLevel() == null || getLevel().isClientSide) return;

        if (getOffsetTimer() % 20 == 0) {
            refreshLinkedUnits();
            getRecipeLogic().setStatus(
                    transferredThisSecond > 0 ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
            transferredPerSecond = transferredThisSecond;
            transferredThisSecond = 0;
        }

        if (!isFormed() || !isWorkingEnabled() || linkedUnits <= 0) return;
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

        long available = hatches.getEnergyStored();
        if (available <= 0) return 0L;

        long moved = 0L;
        LongIterator iterator = unitLinks.iterator();
        while (iterator.hasNext()) {
            if (moved >= available) break;
            long packed = iterator.nextLong();
            if (!(MetaMachine.getMachine(level, BlockPos.of(packed)) instanceof LinkedPurificationUnitMachine unit) ||
                    !unit.isFormed()) {
                continue;
            }
            long voltage = unit.getLinkVoltage();
            if (voltage <= 0) continue;
            long amperage = Math.min(unit.getLinkAmperage(), (available - moved) / voltage);
            if (amperage <= 0) break;
            moved += unit.acceptLinkedEnergy(amperage) * voltage;
        }
        if (moved > 0) {
            hatches.changeEnergy(-moved);
        }
        return moved;
    }

    //////////////////////////////////////
    // ********** 持久化 ***********//
    //////////////////////////////////////

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        tag.putLongArray(NBT_LINKED_UNITS, unitLinks.toLongArray());
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
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
        var group = new WidgetGroup(0, 0, 182 + 8, 117 + 26);
        var scroll = new DraggableScrollableWidgetGroup(4, 4, 182, 117).setBackground(getScreenTexture());
        scroll.addWidget(new LabelWidget(4, 5, self().getBlockState().getBlock().getDescriptionId()));
        scroll.addWidget(new ComponentPanelWidget(4, 17, this::addDisplayText)
                .textSupplier(this.getLevel().isClientSide ? null : this::addDisplayText)
                .setMaxWidthLimit(200)
                .clickHandler(this::handleDisplayClick));
        group.addWidget(scroll);
        group.addWidget(new LabelWidget(8, 125, "com.gtecore.gui.water_plant.parallel"));
        group.addWidget(new IntInputWidget(118, 122, 64, 20, this::getParallel, this::setParallel)
                .setMin(MIN_PARALLEL)
                .setMax(MAX_PARALLEL));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);

        textList.add(Component.translatable("com.gtecore.tooltips.water_plant.parallel", parallel)
                .withStyle(ChatFormatting.GOLD));
        textList.add(Component.translatable("com.gtecore.tooltips.water_plant.units", linkedUnits)
                .withStyle(linkedUnits > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        textList.add(Component.translatable("com.gtecore.tooltips.water_plant.throughput", transferredPerSecond)
                .withStyle(ChatFormatting.YELLOW));
        if (linkedUnits <= 0) {
            textList.add(Component.translatable("com.gtecore.tooltips.water_plant.link_hint")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
