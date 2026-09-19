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
import org.satou.gtecore.common.machine.multiblock.water.ThermalPurificationUnitMachine;
import com.gregtechceu.gtceu.common.data.GTMachines;

import static com.gregtechceu.gtceu.api.pattern.Predicates.abilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.autoAbilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.GCYMBlocks.CASING_WATERTIGHT;
import static com.gregtechceu.gtceu.common.data.GCYMBlocks.ELECTROLYTIC_CELL;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_LAMINATED_GLASS;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_PTFE_INERT;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_STAINLESS_TURBINE;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_STEEL_PIPE;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_TITANIUM_PIPE;
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
                    // Z = 0 背面：外骨骼框架 + 大面积水幕视窗
                    .aisle("..AAA..",
                            ".FFAFF.",
                            ".FGGGF.",
                            ".FGGGF.",
                            ".FGGGF.",
                            ".FFAFF.",
                            "..AAA..")
                    // Z = 1 侧翼立柱与配水横梁
                    .aisle(".AAAAA.",
                            "FA...AF",
                            "F.....F",
                            "F..P..F",
                            "F.....F",
                            "FA...AF",
                            ".AAAAA.")
                    // Z = 2 侧壁与循环水路
                    .aisle("AAAAAAA",
                            "A.P.P.A",
                            "G.P.P.G",
                            "G.PPP.G",
                            "G.P.P.G",
                            "A.P.P.A",
                            "AAAAAAA")
                    // Z = 3 中轴：中央高压泵送涡轮核心 + 垂直主管网
                    .aisle("AAAAAAA",
                            "A.PTP.A",
                            "G.TTT.G",
                            "GPTTTPG",
                            "G.TTT.G",
                            "A.PTP.A",
                            "AAAAAAA")
                    // Z = 4 侧壁与循环水路（对称）
                    .aisle("AAAAAAA",
                            "A.P.P.A",
                            "G.P.P.G",
                            "G.PPP.G",
                            "G.P.P.G",
                            "A.P.P.A",
                            "AAAAAAA")
                    // Z = 5 侧翼立柱与配水横梁（对称）
                    .aisle(".AAAAA.",
                            "FA...AF",
                            "F.....F",
                            "F..P..F",
                            "F.....F",
                            "FA...AF",
                            ".AAAAA.")
                    // Z = 6 正面：控制器（底层居中）+ 观察窗
                    .aisle("..AAA..",
                            ".FFAFF.",
                            ".FGGGF.",
                            ".FGGGF.",
                            ".FGGGF.",
                            ".FFAFF.",
                            "..A#A..")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("A", blocks(CASING_WATERTIGHT.get())
                            .setMinGlobalLimited(24)
                            .or(autoAbilities(true, false, false))
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(4))
                            .or(abilities(PartAbility.INPUT_LASER)))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("P", blocks(CASING_STEEL_PIPE.get()))
                    .where("T", blocks(CASING_STAINLESS_TURBINE.get()))
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
            .multiblock("t1_clarifier_purification_unit", ThermalPurificationUnitMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(GTERecipeTypes.WATER_PURIFICATION_RECIPES)
            .recipeModifiers(ThermalPurificationUnitMachine::recipeModifier)
            .appearanceBlock(CASING_WATERTIGHT)
            .pattern(definition -> FactoryBlockPattern.start()
                    // 9 wide x 8 high x 7 deep: separate evaporation and condensation towers.
                    // Z = 0; rows run from the skid upward.
                    .aisle(".FFFFFFF.",
                            ".........",
                            ".........",
                            ".........",
                            ".........",
                            ".........",
                            ".........",
                            ".........")
                    // Z = 1; rows run from the skid upward.
                    .aisle("FFAAAAAFF",
                            "F.......F",
                            "F.PPPPP.F",
                            "F.P...P.F",
                            "F.PPPPP.F",
                            ".........",
                            ".........",
                            ".........")
                    // Z = 2; rows run from the skid upward.
                    .aisle(".FAAAAAF.",
                            ".AAA.AAA.",
                            ".APA.APA.",
                            ".AGA.AGA.",
                            ".APA.APA.",
                            ".AGA.AAA.",
                            ".AAA.....",
                            ".........")
                    // Z = 3; rows run from the skid upward.
                    .aisle(".FAAAAAF.",
                            ".APA.APA.",
                            ".GPG.GPG.",
                            ".GPG.GPG.",
                            ".GPG.GPG.",
                            ".GPG.AAA.",
                            ".AAA..P..",
                            "..PPPPP..")
                    // Z = 4; rows run from the skid upward.
                    .aisle(".FAAAAAF.",
                            ".AAA.AAA.",
                            ".AGA.AGA.",
                            ".AGA.AGA.",
                            ".AGA.AGA.",
                            ".AGA.AAA.",
                            ".AAA.....",
                            ".........")
                    // Z = 5; rows run from the skid upward.
                    .aisle("FFAAAAAFF",
                            "FAAAAAAAF",
                            "F.......F",
                            "F.......F",
                            "FFFFFFFFF",
                            ".........",
                            ".........",
                            ".........")
                    // Z = 6; rows run from the skid upward.
                    .aisle(".FFFFFFF.",
                            ".AMA#ATAS",
                            ".........",
                            ".........",
                            ".........",
                            ".........",
                            ".........",
                            ".........")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("A", blocks(CASING_WATERTIGHT.get()).setMinGlobalLimited(40)
                            .or(autoAbilities(new GTRecipeType[] { GTERecipeTypes.WATER_PURIFICATION_RECIPES },
                                    false, false, true, true, true, true)))
                    // Exact ordinary hatch: automatic maintenance must not bypass thermal damage.
                    .where("M", blocks(GTMachines.MAINTENANCE_HATCH.getBlock()).setExactLimit(1))
                    .where("T", abilities(GTEWaterPurificationParts.THERMAL_CONTROL).setExactLimit(1))
                    .where("S", abilities(GTEWaterPurificationParts.THERMAL_SIGNAL).setExactLimit(1))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("P", blocks(CASING_STEEL_PIPE.get()))
                    // Outside the vessels is unrestricted, so redstone can reach the thermal hatch.
                    .where(".", Predicates.any())
                    .build())
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.t1_clarifier_purification_unit.0"),
                    Component.translatable("com.gtecore.tooltips.t1_clarifier_purification_unit.1"),
                    Component.translatable("gtecore.water.thermal.rule"),
                    Component.translatable("gtecore.water.thermal.efficiency_hint"),
                    Component.translatable("gtecore.water.thermal_hatch.help"),
                    Component.translatable("gtecore.water.thermal_signal.help"),
                    Component.translatable("gtecore.water.thermal.manual_maintenance"),
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
            .appearanceBlock(CASING_PTFE_INERT)
            .pattern(definition -> FactoryBlockPattern.start()
                    // Z = 0 背面：PTFE 耐蚀倒角 + 滤光层压玻璃通道
                    .aisle(".AAA.",
                            "FALAF",
                            "FGLGF",
                            "FGLGF",
                            "FGLGF",
                            "FALAF",
                            ".AAA.")
                    // Z = 1 侧面光催化区
                    .aisle("AAAAA",
                            "A...A",
                            "G.L.G",
                            "G.L.G",
                            "G.L.G",
                            "A...A",
                            "AAAAA")
                    // Z = 2 中轴：深紫外光反应柱 + PTFE 氧化剂管路
                    .aisle("AALAA",
                            "L.P.L",
                            "LPLPL",
                            "LPPPL",
                            "LPLPL",
                            "L.P.L",
                            "AAAAA")
                    // Z = 3 侧面光催化区（对称）
                    .aisle("AAAAA",
                            "A...A",
                            "G.L.G",
                            "G.L.G",
                            "G.L.G",
                            "A...A",
                            "AAAAA")
                    // Z = 4 正面：控制器（底层居中）+ 紫外滤光视窗
                    .aisle(".AAA.",
                            "FALAF",
                            "FGLGF",
                            "FGLGF",
                            "FGLGF",
                            "FALAF",
                            ".A#A.")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("A", blocks(CASING_PTFE_INERT.get())
                            .setMinGlobalLimited(16)
                            .or(unitAbilities()))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("L", blocks(CASING_LAMINATED_GLASS.get()))
                    .where("P", blocks(CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()))
                    .where(".", Predicates.any())
                    .build())
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.t2_uv_oxidation_purification_unit.0"),
                    Component.translatable("com.gtecore.tooltips.t2_uv_oxidation_purification_unit.1"),
                    Component.translatable("com.gtecore.tooltips.0"))
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_inert_ptfe"),
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
                    // Z = 0 背面：水密倒角 + 框架 + 电解膜板阵列视窗
                    .aisle("..AAA..",
                            ".FAEAF.",
                            ".FEGEF.",
                            ".FEGEF.",
                            ".FEGEF.",
                            ".FAEAF.",
                            "..AAA..")
                    // Z = 1 侧翼外壳与纯化立柱
                    .aisle(".AAAAA.",
                            "FA...AF",
                            "G.....G",
                            "G..P..G",
                            "G.....G",
                            "FA...AF",
                            ".AAAAA.")
                    // Z = 2 EDI 电极堆栈排 1
                    .aisle("AAAAAAA",
                            "A.E.E.A",
                            "E.E.E.E",
                            "E.EPE.E",
                            "E.E.E.E",
                            "A.E.E.A",
                            "AAAAAAA")
                    // Z = 3 中轴：超纯水收集管束与树脂床
                    .aisle("AAAAAAA",
                            "E.PPP.E",
                            "G.PEP.G",
                            "PPEPEPP",
                            "G.PEP.G",
                            "E.PPP.E",
                            "AAAAAAA")
                    // Z = 4 EDI 电极堆栈排 2（对称）
                    .aisle("AAAAAAA",
                            "A.E.E.A",
                            "E.E.E.E",
                            "E.EPE.E",
                            "E.E.E.E",
                            "A.E.E.A",
                            "AAAAAAA")
                    // Z = 5 侧翼外壳与纯化立柱（对称）
                    .aisle(".AAAAA.",
                            "FA...AF",
                            "G.....G",
                            "G..P..G",
                            "G.....G",
                            "FA...AF",
                            ".AAAAA.")
                    // Z = 6 正面：控制器（底层居中）+ 离子膜堆视窗
                    .aisle("..AAA..",
                            ".FAEAF.",
                            ".FEGEF.",
                            ".FEGEF.",
                            ".FEGEF.",
                            ".FAEAF.",
                            "..A#A..")
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("F", easy("gtceu:stainless_steel_frame"))
                    .where("A", blocks(CASING_WATERTIGHT.get())
                            .setMinGlobalLimited(24)
                            .or(unitAbilities()))
                    .where("G", blocks(CLEANROOM_GLASS.get()))
                    .where("E", blocks(ELECTROLYTIC_CELL.get()))
                    .where("P", blocks(CASING_TITANIUM_PIPE.get()))
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
