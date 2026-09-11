package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.machine.multiblock.water.CentralPurificationPlantMachine;
import org.satou.gtecore.common.machine.multiblock.water.LinkedPurificationUnitMachine;

import static com.gregtechceu.gtceu.api.pattern.Predicates.abilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.autoAbilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.GCYMBlocks.CASING_WATERTIGHT;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_STEEL_PIPE;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CLEANROOM_GLASS;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_MACHINES;
import static org.satou.gtecore.utils.GTEUtils.easy;

/**
 * 净水产线的四台多方块设备：
 * <ol>
 *     <li>{@code central_water_purification_plant} 中枢净化水厂 —— 数据棒连接各级净化单元、统一供电并下发并行度；</li>
 *     <li>{@code t1_clarifier_purification_unit} 一级澄清净化装置（EV）；</li>
 *     <li>{@code t2_uv_oxidation_purification_unit} 二级紫外氧化净化装置（LuV）；</li>
 *     <li>{@code t3_edi_ultrapure_purification_unit} 三级 EDI 超纯净化装置（ZPM）。</li>
 * </ol>
 * 三个等级单元自身没有能源仓，必须连接中枢才能开机。
 */
public class GTEWaterPurificationMachines {

    static {
        GTECore_REGISTRATE.creativeModeTab(() -> MORE_MACHINES);
    }

    public static void init() {}

    /** 三个等级单元共用的仓口谓词：没有能源仓，电力只能来自中枢。 */
    private static TraceabilityPredicate unitAbilities() {
        return autoAbilities(new GTRecipeType[] { GTERecipeTypes.WATER_PURIFICATION_RECIPES },
                false, false, true, true, true, true)
                .or(autoAbilities(true, false, false))
                .or(abilities(PartAbility.MUFFLER).setMaxGlobalLimited(1));
    }

    //////////////////////////////////////
    // ******** 中枢净化水厂 ********//
    //////////////////////////////////////

    public static final MultiblockMachineDefinition CENTRAL_WATER_PURIFICATION_PLANT = GTECore_REGISTRATE
            .multiblock("central_water_purification_plant", CentralPurificationPlantMachine::new)
            .rotationState(RotationState.ALL)
            .recipeType(GTRecipeTypes.DUMMY_RECIPES)
            .appearanceBlock(CASING_WATERTIGHT)
            .pattern(definition -> FactoryBlockPattern.start()
                    // Z = 0 背面：玻璃水幕 + 集水主管
                    .aisle("..AAA..",
                            "..AAA..",
                            "..GGG..",
                            "..GGG..",
                            "..GGG..",
                            "..GGG..",
                            "..APA..",
                            "..AAA..",
                            "..AAA..")
                    // Z = 1 对角立柱
                    .aisle(".AAAAA.",
                            ".A...A.",
                            ".F...F.",
                            ".F...F.",
                            ".F...F.",
                            ".F...F.",
                            ".A...A.",
                            ".A...A.",
                            ".AAAAA.")
                    // Z = 2 侧面外壳
                    .aisle("AAAAAAA",
                            "A.....A",
                            "F.....F",
                            "F.....F",
                            "F.....F",
                            "F.....F",
                            "A.....A",
                            "A.....A",
                            "AAAAAAA")
                    // Z = 3 中轴：虚数光核 + 双列管道
                    .aisle("AAAAAAA",
                            "A.....A",
                            "G..C..G",
                            "G..C..G",
                            "G..C..G",
                            "G..C..G",
                            "P..C..P",
                            "A.....A",
                            "AAAAAAA")
                    // Z = 4 侧面外壳
                    .aisle("AAAAAAA",
                            "A.....A",
                            "F.....F",
                            "F.....F",
                            "F.....F",
                            "F.....F",
                            "A.....A",
                            "A.....A",
                            "AAAAAAA")
                    // Z = 5 对角立柱
                    .aisle(".AAAAA.",
                            ".A...A.",
                            ".F...F.",
                            ".F...F.",
                            ".F...F.",
                            ".F...F.",
                            ".A...A.",
                            ".A...A.",
                            ".AAAAA.")
                    // Z = 6 正面：控制器 + 观察窗
                    .aisle("..A#A..",
                            "..AAA..",
                            "..GGG..",
                            "..GGG..",
                            "..GGG..",
                            "..GGG..",
                            "..APA..",
                            "..AAA..",
                            "..AAA..")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("A", blocks(CASING_WATERTIGHT.get())
                            .setMinGlobalLimited(24)
                            .or(autoAbilities(true, false, false))
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(4))
                            .or(abilities(PartAbility.INPUT_LASER)))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("P", blocks(CASING_STEEL_PIPE.get()))
                    .where("C", blocks(GTEBlocks.IMAGINARY_GLASS.get()))
                    .where(".", Predicates.any())
                    .build())
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.central_water_purification_plant.0"),
                    Component.translatable("com.gtecore.tooltips.central_water_purification_plant.1"),
                    Component.translatable("com.gtecore.tooltips.central_water_purification_plant.2"),
                    Component.translatable("com.gtecore.tooltips.0"))
            .workableCasingModel(GTCEu.id("block/casings/gcym/watertight_casing"),
                    GTCEu.id("block/multiblock/distillation_tower"))
            .register();

    //////////////////////////////////////
    // ******* 一级澄清净化装置 *******//
    //////////////////////////////////////

    public static final MultiblockMachineDefinition T1_CLARIFIER_PURIFICATION_UNIT = GTECore_REGISTRATE
            .multiblock("t1_clarifier_purification_unit",
                    holder -> new LinkedPurificationUnitMachine(holder, GTValues.EV))
            .rotationState(RotationState.ALL)
            .recipeType(GTERecipeTypes.WATER_PURIFICATION_RECIPES)
            .recipeModifiers(LinkedPurificationUnitMachine::recipeModifier)
            .appearanceBlock(CASING_WATERTIGHT)
            .pattern(definition -> FactoryBlockPattern.start()
                    // Z = 0 背面
                    .aisle("FAAAF",
                            "FAAAF",
                            "FGGGF",
                            "FGGGF",
                            "FGGGF",
                            "FAAAF",
                            "FAAAF")
                    // Z = 1 侧壁
                    .aisle("AAAAA",
                            "A...A",
                            "G...G",
                            "G...G",
                            "G...G",
                            "A...A",
                            "AAAAA")
                    // Z = 2 中轴闪蒸柱
                    .aisle("AAAAA",
                            "A...A",
                            "G.P.G",
                            "G.P.G",
                            "G.P.G",
                            "A...A",
                            "AAAAA")
                    // Z = 3 侧壁
                    .aisle("AAAAA",
                            "A...A",
                            "G...G",
                            "G...G",
                            "G...G",
                            "A...A",
                            "AAAAA")
                    // Z = 4 正面：控制器
                    .aisle("FA#AF",
                            "FAAAF",
                            "FGGGF",
                            "FGGGF",
                            "FGGGF",
                            "FAAAF",
                            "FAAAF")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("A", blocks(CASING_WATERTIGHT.get())
                            .setMinGlobalLimited(12)
                            .or(unitAbilities()))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("P", blocks(CASING_STEEL_PIPE.get()))
                    .where(".", Predicates.any())
                    .build())
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.t1_clarifier_purification_unit.0"),
                    Component.translatable("com.gtecore.tooltips.t1_clarifier_purification_unit.1"),
                    Component.translatable("com.gtecore.tooltips.0"))
            .workableCasingModel(GTCEu.id("block/casings/gcym/watertight_casing"),
                    GTCEu.id("block/multiblock/evaporation_plant"))
            .register();

    //////////////////////////////////////
    // ***** 二级紫外氧化净化装置 *****//
    //////////////////////////////////////

    public static final MultiblockMachineDefinition T2_UV_OXIDATION_PURIFICATION_UNIT = GTECore_REGISTRATE
            .multiblock("t2_uv_oxidation_purification_unit",
                    holder -> new LinkedPurificationUnitMachine(holder, GTValues.LuV))
            .rotationState(RotationState.ALL)
            .recipeType(GTERecipeTypes.WATER_PURIFICATION_RECIPES)
            .recipeModifiers(LinkedPurificationUnitMachine::recipeModifier)
            .appearanceBlock(GTEBlocks.KAN_SHUI_CASING)
            .pattern(definition -> FactoryBlockPattern.start()
                    // Z = 0 背面：紫外灯管阵列
                    .aisle("FAAAF",
                            "FAAAF",
                            "FACAF",
                            "FACAF",
                            "FACAF",
                            "FACAF",
                            "FACAF",
                            "FAAAF",
                            "FAAAF")
                    // Z = 1 对角观察窗
                    .aisle("AAAAA",
                            "A...A",
                            "G...G",
                            "G...G",
                            "G...G",
                            "G...G",
                            "G...G",
                            "A...A",
                            "AAAAA")
                    // Z = 2 侧面灯管 + 中轴灯核
                    .aisle("AAAAA",
                            "A...A",
                            "C.C.C",
                            "C.C.C",
                            "C.C.C",
                            "C.C.C",
                            "C.C.C",
                            "A...A",
                            "AAAAA")
                    // Z = 3 对角观察窗
                    .aisle("AAAAA",
                            "A...A",
                            "G...G",
                            "G...G",
                            "G...G",
                            "G...G",
                            "G...G",
                            "A...A",
                            "AAAAA")
                    // Z = 4 正面：控制器 + 灯管阵列
                    .aisle("FA#AF",
                            "FAAAF",
                            "FACAF",
                            "FACAF",
                            "FACAF",
                            "FACAF",
                            "FACAF",
                            "FAAAF",
                            "FAAAF")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("A", blocks(GTEBlocks.KAN_SHUI_CASING.get())
                            .setMinGlobalLimited(12)
                            .or(unitAbilities()))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("C", blocks(GTEBlocks.IMAGINARY_GLASS.get()))
                    .where(".", Predicates.any())
                    .build())
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.t2_uv_oxidation_purification_unit.0"),
                    Component.translatable("com.gtecore.tooltips.t2_uv_oxidation_purification_unit.1"),
                    Component.translatable("com.gtecore.tooltips.0"))
            .workableCasingModel(GTECore.id("block/casings/uhv/kan_shui_casing"),
                    GTCEu.id("block/multiblock/large_chemical_reactor"))
            .register();

    //////////////////////////////////////
    // **** 三级 EDI 超纯净化装置 ****//
    //////////////////////////////////////

    public static final MultiblockMachineDefinition T3_EDI_ULTRAPURE_PURIFICATION_UNIT = GTECore_REGISTRATE
            .multiblock("t3_edi_ultrapure_purification_unit",
                    holder -> new LinkedPurificationUnitMachine(holder, GTValues.ZPM))
            .rotationState(RotationState.ALL)
            .recipeType(GTERecipeTypes.WATER_PURIFICATION_RECIPES)
            .recipeModifiers(LinkedPurificationUnitMachine::recipeModifier)
            .appearanceBlock(CASING_WATERTIGHT)
            .pattern(definition -> FactoryBlockPattern.start()
                    // Z = 0 背面：离子交换膜组
                    .aisle("..AAA..",
                            "..AAA..",
                            "..GCG..",
                            "..GCG..",
                            "..GCG..",
                            "..GCG..",
                            "..GCG..",
                            "..AAA..",
                            "..AAA..")
                    // Z = 1 对角立柱 + 观察窗
                    .aisle(".AFAFA.",
                            ".F...F.",
                            ".G...G.",
                            ".G...G.",
                            ".G...G.",
                            ".G...G.",
                            ".G...G.",
                            ".F...F.",
                            ".AFAFA.")
                    // Z = 2 侧面膜组
                    .aisle("AAAAAAA",
                            "A.....A",
                            "C.....C",
                            "C.....C",
                            "C.....C",
                            "C.....C",
                            "C.....C",
                            "A.....A",
                            "AAAAAAA")
                    // Z = 3 中轴 EDI 极板堆栈
                    .aisle("AAAAAAA",
                            "A.....A",
                            "G..P..G",
                            "G..P..G",
                            "G..P..G",
                            "G..P..G",
                            "G..P..G",
                            "A.....A",
                            "AAAAAAA")
                    // Z = 4 侧面膜组
                    .aisle("AAAAAAA",
                            "A.....A",
                            "C.....C",
                            "C.....C",
                            "C.....C",
                            "C.....C",
                            "C.....C",
                            "A.....A",
                            "AAAAAAA")
                    // Z = 5 对角立柱 + 观察窗
                    .aisle(".AFAFA.",
                            ".F...F.",
                            ".G...G.",
                            ".G...G.",
                            ".G...G.",
                            ".G...G.",
                            ".G...G.",
                            ".F...F.",
                            ".AFAFA.")
                    // Z = 6 正面：控制器 + 膜组
                    .aisle("..A#A..",
                            "..AAA..",
                            "..GCG..",
                            "..GCG..",
                            "..GCG..",
                            "..GCG..",
                            "..GCG..",
                            "..AAA..",
                            "..AAA..")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("A", blocks(CASING_WATERTIGHT.get())
                            .setMinGlobalLimited(24)
                            .or(unitAbilities()))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("C", blocks(GTEBlocks.IMAGINARY_CASING.get()))
                    .where("P", blocks(CASING_STEEL_PIPE.get()))
                    .where(".", Predicates.any())
                    .build())
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.t3_edi_ultrapure_purification_unit.0"),
                    Component.translatable("com.gtecore.tooltips.t3_edi_ultrapure_purification_unit.1"),
                    Component.translatable("com.gtecore.tooltips.0"))
            .workableCasingModel(GTCEu.id("block/casings/gcym/watertight_casing"),
                    GTCEu.id("block/multiblock/gcym/large_electrolyzer"))
            .register();
}
