package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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
    private final NotifiableEnergyContainer internalEnergy;

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
        this.internalEnergy.setCapabilityValidator(Objects::isNull);
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

    /**
     * 由中枢调用，向本机内部缓冲注入能量。
     *
     * @param amperage 中枢希望注入的安培数
     * @return 实际接受的安培数
     */
    public long acceptLinkedEnergy(long amperage) {
        if (amperage <= 0) return 0L;
        return internalEnergy.acceptEnergyFromNetwork(null, getLinkVoltage(), amperage);
    }

    /** 解析当前绑定的中枢，未绑定、方块消失或未成型时返回 {@code null}。 */
    public @Nullable CentralPurificationPlantMachine getPlant() {
        Level level = getLevel();
        if (level == null || plantPos == null) return null;
        if (MetaMachine.getMachine(level, plantPos) instanceof CentralPurificationPlantMachine plant &&
                plant.isFormed()) {
            return plant;
        }
        return null;
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
        if (level == null) return false;

        CentralPurificationPlantMachine oldPlant = getPlant();
        if (pos == null) {
            if (oldPlant != null) oldPlant.removeUnit(getPos());
            this.plantPos = null;
            onChanged();
            return true;
        }
        if (!(MetaMachine.getMachine(level, pos) instanceof CentralPurificationPlantMachine newPlant)) {
            return false;
        }
        if (oldPlant != null && oldPlant != newPlant) {
            oldPlant.removeUnit(getPos());
        }
        this.plantPos = pos;
        newPlant.addUnit(getPos());
        onChanged();
        return true;
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (isRemote()) return InteractionResult.PASS;
        BlockPos pos = readStickPos(dataStick);
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
        dataStick.getOrCreateTag().putIntArray("pos",
                new int[] { getPos().getX(), getPos().getY(), getPos().getZ() });
        player.displayClientMessage(Component.translatable("com.gtecore.chat.water_unit.copied")
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onMachineRemoved() {
        CentralPurificationPlantMachine plant = getPlant();
        if (plant != null) plant.removeUnit(getPos());
    }

    @Nullable
    private static BlockPos readStickPos(ItemStack dataStick) {
        if (!dataStick.hasTag() || !dataStick.getOrCreateTag().contains("pos", Tag.TAG_INT_ARRAY)) return null;
        int[] posArray = dataStick.getOrCreateTag().getIntArray("pos");
        if (posArray.length < 3) return null;
        return new BlockPos(posArray[0], posArray[1], posArray[2]);
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
