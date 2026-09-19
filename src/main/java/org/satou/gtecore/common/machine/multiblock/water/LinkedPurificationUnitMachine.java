package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 水净化线的分阶净化单元（一级澄清 / 二级紫外氧化 / 三级 EDI 超纯）。
 *
 * <p>
 * 本机自身<b>没有能源仓</b>，内部只有一个仅供配方消费的 EU 缓冲，缓冲里的电力只能由
 * {@link CentralPurificationPlantMachine 中枢净化水厂} 通过内部接口注入。因此：
 * <ul>
 *     <li>必须用数据棒与中枢绑定，未绑定（或中枢未成型）时配方修饰器直接返回 {@code NULL}，机器无法开机；</li>
 *     <li>并行度由中枢统一下发，实际并行还受本机 EU 预算与输入/输出仓容量限制。</li>
 * </ul>
 */
public class LinkedPurificationUnitMachine extends WorkableElectricMultiblockMachine
                                         implements IDataStickInteractable, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LinkedPurificationUnitMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** 中枢每 tick 最多向本机注入的安培数（决定本机的 EU/t 上限 = 电压 × 该值）。 */
    public static final long LINK_AMPERAGE = 256L;
    /** 本机内部缓冲相当于多少 tick 的满额输入。 */
    private static final long BUFFER_TICKS = 20L;

    /** 本机所属科技等级（{@link GTValues} 的 EV / LuV / ZPM 索引）。 */
    @Getter
    private final int unitTier;

    /** 仅用于配方消费的内部 EU 缓冲，不对外暴露任何能源能力。 */
    @Persisted
    @DescSynced
    protected final NotifiableEnergyContainer internalEnergy;

    @Persisted
    @DescSynced
    @Getter
    private @Nullable BlockPos plantPos = null;

    public LinkedPurificationUnitMachine(IMachineBlockEntity holder, int tier) {
        super(holder);
        this.unitTier = tier;
        long voltage = GTValues.V[tier];
        this.internalEnergy = new NotifiableEnergyContainer(this, voltage * LINK_AMPERAGE * BUFFER_TICKS,
                voltage, LINK_AMPERAGE, 0L, 0L);
        // 只有中枢的内部注入可以进入该缓冲，线缆/能源仓无法直接为单元供电。
        this.internalEnergy.setCapabilityValidator(side -> false);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ******* 中枢互联 *******//
    //////////////////////////////////////

    /** 中枢注入能量时使用的电压。 */
    public long getLinkVoltage() {
        return GTValues.V[unitTier];
    }

    /** 中枢注入能量时使用的最大安培。 */
    public long getLinkAmperage() {
        return LINK_AMPERAGE;
    }

    /** 本机 EU/t 预算，用于限制并行度。 */
    public long getEUtBudget() {
        return getLinkVoltage() * LINK_AMPERAGE;
    }

    /** Only the currently registered, operational plant can charge this private buffer. */
    long acceptLinkedEnergy(CentralPurificationPlantMachine source, long amperage) {
        if (amperage <= 0 || !isFormed() || !isWorkingEnabled() || getPlant() != source) return 0L;
        return internalEnergy.acceptEnergyFromNetwork(null, getLinkVoltage(), amperage);
    }

    public long getStoredLinkedEnergy() {
        return internalEnergy.getEnergyStored();
    }

    public long getLinkedEnergyCapacity() {
        return internalEnergy.getEnergyCapacity();
    }

    private @Nullable CentralPurificationPlantMachine findPlant() {
        Level level = getLevel();
        if (level == null || plantPos == null || !level.hasChunkAt(plantPos)) return null;
        return MetaMachine.getMachine(level, plantPos) instanceof CentralPurificationPlantMachine plant ? plant : null;
    }

    /** Resolve both ends without loading chunks. Disabled or incomplete plants cannot run units. */
    public @Nullable CentralPurificationPlantMachine getPlant() {
        CentralPurificationPlantMachine plant = findPlant();
        return plant != null && plant.isFormed() && plant.isWorkingEnabled() && plant.hasUnit(getPos()) ? plant : null;
    }

    /** 中枢下发的并行度；未连接时为 0（表示禁止开机）。 */
    public int getRequestedParallel() {
        CentralPurificationPlantMachine plant = getPlant();
        return plant == null ? 0 : plant.getParallel();
    }

    /**
     * 数据棒绑定：写入中枢坐标，并让中枢登记本机。
     *
     * @return 是否绑定成功
     */
    public boolean bindToPlant(@Nullable BlockPos pos) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return false;

        CentralPurificationPlantMachine oldPlant = findPlant();
        if (pos == null) {
            if (oldPlant != null) oldPlant.removeUnit(getPos());
            this.plantPos = null;
            getRecipeLogic().markLastRecipeDirty();
            onChanged();
            return true;
        }
        if (!level.hasChunkAt(pos) || !(MetaMachine.getMachine(level, pos) instanceof CentralPurificationPlantMachine newPlant)) {
            return false;
        }
        if (oldPlant != null && oldPlant != newPlant) {
            oldPlant.removeUnit(getPos());
        }
        this.plantPos = pos.immutable();
        newPlant.addUnit(getPos());
        getRecipeLogic().markLastRecipeDirty();
        onChanged();
        return true;
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (isRemote()) return InteractionResult.PASS;
        BlockPos pos = WaterPurificationLink.read(dataStick, getLevel(), "plant");
        if (pos == null) return InteractionResult.PASS;
        if (bindToPlant(pos)) {
            player.displayClientMessage(Component.translatable("com.gtecore.chat.water_unit.linked")
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("com.gtecore.chat.water_unit.link_failed")
                .withStyle(ChatFormatting.RED), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (isRemote()) return InteractionResult.SUCCESS;
        WaterPurificationLink.write(dataStick, getLevel(), getPos(), "unit");
        player.displayClientMessage(Component.translatable("com.gtecore.chat.water_unit.copied")
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onMachineRemoved() {
        bindToPlant(null);
    }

    @Override
    public boolean keepSubscribing() {
        return true;
    }

    @Override
    public boolean alwaysTryModifyRecipe() {
        // Recalculate the plant's per-unit parallel ceiling at each cycle boundary.
        return true;
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        return getPlant() != null && recipe != null && acceptsPurificationRecipe(recipe) && super.beforeWorking(recipe);
    }

    protected boolean acceptsPurificationRecipe(GTRecipe recipe) {
        return recipe.data.getInt("waterPurificationTier") == unitTier;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new LinkedRecipeLogic(this);
    }

    /** Subclasses may specialize output IO while retaining link validation before any tick IO. */
    protected static class LinkedRecipeLogic extends RecipeLogic {

        protected final LinkedPurificationUnitMachine unit;

        protected LinkedRecipeLogic(LinkedPurificationUnitMachine unit) {
            super(unit);
            this.unit = unit;
        }

        @Override
        public void serverTick() {
            if (!isSuspend() && (!unit.isWorkingEnabled() || unit.getPlant() == null)) {
                if (getLastRecipe() != null && !isIdle()) {
                    setWaiting(Component.translatable("com.gtecore.tooltips.water_unit.unlinked"));
                }
                return;
            }
            super.serverTick();
        }
    }

    //////////////////////////////////////
    // ******* 配方与并行 *******//
    //////////////////////////////////////

    /**
     * 计算实际并行度：先受中枢下发的上限约束，再受本机 EU 预算、输入库存量与输出仓容量约束。
     *
     * @return 0 表示无法运行（未连接中枢、电力不足或原料不足）
     */
    public int computeParallel(@NotNull GTRecipe recipe) {
        if (!acceptsPurificationRecipe(recipe)) return 0;
        int limit = getRequestedParallel();
        if (limit <= 0) return 0;

        long recipeEUt = Math.max(1L, recipe.getInputEUt().getTotalEU());
        limit = (int) Math.min(limit, getEUtBudget() / recipeEUt);
        if (limit <= 0) return 0;
        if (limit == 1) return 1;

        boolean hasNonEuInput = recipe.inputs.keySet().stream()
                .anyMatch(cap -> cap != EURecipeCapability.CAP && cap.doMatchInRecipe()) ||
                recipe.tickInputs.keySet().stream()
                        .anyMatch(cap -> cap != EURecipeCapability.CAP && cap.doMatchInRecipe());
        int byInput = hasNonEuInput ?
                ParallelLogic.getMaxByInput(this, recipe, limit, List.of(EURecipeCapability.CAP)) : limit;
        if (byInput <= 0) return 0;
        return ParallelLogic.limitByOutputMerging(this, recipe, byInput, this::canVoidRecipeOutputs, List.of());
    }

    /** 净化单元统一的配方修饰器：未连接中枢直接拒绝配方，否则按实际并行度放大输入/输出/耗电。 */
    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof LinkedPurificationUnitMachine unit)) {
            return RecipeModifier.nullWrongType(LinkedPurificationUnitMachine.class, machine);
        }
        int parallel = unit.computeParallel(recipe);
        if (parallel <= 0) return ModifierFunction.NULL;
        if (parallel == 1) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallel))
                .outputModifier(ContentModifier.multiplier(parallel))
                .eutModifier(ContentModifier.multiplier(parallel))
                .parallels(parallel)
                .build();
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    protected ButtonWidget createDisconnectButton(int x, int y, int width) {
        return new ButtonWidget(x, y, width, 18,
                new GuiTextureGroup(new ColorRectTexture(0xFF163E50),
                        new TextTexture("com.gtecore.gui.water_unit.disconnect")),
                click -> { if (!click.isRemote) bindToPlant(null); });
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 190, 149);
        group.addWidget(super.createUIWidget());
        group.addWidget(createDisconnectButton(8, 128, 174));
        return group;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);

        CentralPurificationPlantMachine plant = getPlant();
        if (plant == null) {
            textList.add(Component.translatable("com.gtecore.tooltips.water_unit.unlinked")
                    .withStyle(ChatFormatting.RED));
            textList.add(Component.translatable("com.gtecore.tooltips.water_unit.link_hint")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            textList.add(Component.translatable("com.gtecore.tooltips.water_unit.linked",
                    plant.getPos().getX(), plant.getPos().getY(), plant.getPos().getZ())
                    .withStyle(ChatFormatting.AQUA));
            textList.add(Component.translatable("com.gtecore.tooltips.water_unit.parallel",
                    plant.getParallel()).withStyle(ChatFormatting.GOLD));
        }
        textList.add(Component.translatable("com.gtecore.tooltips.water_unit.energy",
                internalEnergy.getEnergyStored(), internalEnergy.getEnergyCapacity())
                .withStyle(ChatFormatting.YELLOW));
    }
}
