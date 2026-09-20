package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import net.minecraft.data.recipes.FinishedRecipe;
import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationParts;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;

public class WATER_PURIFICATION_HANDLER {

    /**
     * 净水产线配方。所有流量均为「单次并行」的用量，实际吞吐 = 该数值 × 实际并行度。
     */
    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        // ---------------------------------------------------------------
        // Upstream Precursor Chemical Recipes (虚数/半导体级前置产线)
        // ---------------------------------------------------------------

        // 1. 高纯复合絮凝液 (PAFC/PAM 路线)
        CHEMICAL_RECIPES.recipeBuilder("composite_flocculant")
                .inputItems(dust, Aluminium, 2)
                .inputItems(dust, Iron, 1)
                .inputFluids(HydrochloricAcid.getFluid(3000))
                .inputFluids(DistilledWater.getFluid(2000))
                .outputFluids(GTEMaterials.CompositeFlocculant.getFluid(4000))
                .outputFluids(Hydrogen.getFluid(3000))
                .duration(200)
                .EUt(VA[IV])
                .save(provider);

        // 2. 改性纳米活性炭微球 (酸活化水热球化路线)
        CHEMICAL_RECIPES.recipeBuilder("modified_carbon_microspheres")
                .inputItems(dust, Carbon, 16)
                .inputItems(dust, SiliconDioxide, 2)
                .inputFluids(SulfuricAcid.getFluid(1000))
                .outputItems(GTEItems.MODIFIED_CARBON_MICROSPHERES.asStack(16))
                .outputFluids(DilutedSulfuricAcid.getFluid(1000))
                .duration(200)
                .EUt(VA[LuV])
                .save(provider);

        // 3. 高纯臭氧 (高压电晕放电发生)
        ELECTROLYZER_RECIPES.recipeBuilder("ozone_from_oxygen")
                .inputFluids(Oxygen.getFluid(3000))
                .outputFluids(GTEMaterials.Ozone.getFluid(2000))
                .duration(100)
                .EUt(VA[IV])
                .save(provider);

        // 4. 电子级混床离子交换树脂微球 (苯乙烯悬浮共聚与磺化/季铵化)
        CHEMICAL_RECIPES.recipeBuilder("mixed_bed_resin_beads_chemical")
                .inputItems(dust, SodiumHydroxide, 2)
                .inputFluids(Styrene.getFluid(1000))
                .inputFluids(Toluene.getFluid(500))
                .inputFluids(SulfuricAcid.getFluid(1000))
                .inputFluids(Ammonia.getFluid(500))
                .outputItems(GTEItems.MIXED_BED_RESIN_BEADS.asStack(8))
                .outputFluids(DilutedSulfuricAcid.getFluid(1000))
                .duration(160)
                .EUt(VA[ZPM])
                .save(provider);

        GTERecipeTypes.INTEGRATED_PETROCHEMICAL_PLANT.recipeBuilder("mixed_bed_resin_beads_petro")
                .inputItems(dust, SodiumHydroxide, 4)
                .inputFluids(Styrene.getFluid(2000))
                .inputFluids(Toluene.getFluid(1000))
                .inputFluids(SulfuricAcid.getFluid(2000))
                .inputFluids(Ammonia.getFluid(1000))
                .outputItems(GTEItems.MIXED_BED_RESIN_BEADS.asStack(24))
                .outputFluids(DilutedSulfuricAcid.getFluid(2000))
                .duration(100)
                .EUt(VA[ZPM])
                .save(provider);

        // 5. 电子级酸碱脱附再生液 (超纯洗脱试剂)
        CHEMICAL_RECIPES.recipeBuilder("electronic_acid_base_reagent")
                .inputItems(dust, SodiumHydroxide, 3)
                .inputFluids(HydrochloricAcid.getFluid(1000))
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(2000))
                .outputFluids(GTEMaterials.ElectronicAcidBaseReagent.getFluid(3000))
                .duration(100)
                .EUt(VA[LuV])
                .save(provider);

        // ---------------------------------------------------------------
        // Water Purification Recipes (三级多段净水与闭环回收)
        // All stages operate at UEV; legacy EV/LuV/ZPM markers only select the purification process.
        // ---------------------------------------------------------------

        // Stage 1a: Raw Water + Flocculant + Carbon Microspheres -> Distilled Purified Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("distilled_purified_water_from_water")
                .addData("waterPurificationTier", EV)
                .inputFluids(Water.getFluid(1000), GTEMaterials.CompositeFlocculant.getFluid(50))
                .inputItems(GTEItems.MODIFIED_CARBON_MICROSPHERES.asStack(1))
                .outputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(900))
                .chancedOutput(dust, Salt, 1000, 500)
                .chancedOutput(dust, RareEarth, 500, 250)
                .duration(60)
                .EUt(VA[UEV])
                .save(provider);

        // Stage 1b: Distilled Water + Flocculant + Carbon Microspheres -> Distilled Purified Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("distilled_purified_water_from_distilled")
                .addData("waterPurificationTier", EV)
                .inputFluids(DistilledWater.getFluid(1000), GTEMaterials.CompositeFlocculant.getFluid(25))
                .inputItems(GTEItems.MODIFIED_CARBON_MICROSPHERES.asStack(1))
                .outputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(1000))
                .chancedOutput(dust, Salt, 500, 250)
                .duration(30)
                .EUt(VA[UEV])
                .save(provider);

        // Stage 2a: Distilled Purified Water + Ozone -> UV Purified Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("uv_purified_water_from_ozone")
                .addData("waterPurificationTier", LuV)
                .addData("uvDose", 40)
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(800), GTEMaterials.Ozone.getFluid(50))
                .outputFluids(GTEMaterials.UvPurifiedWater.getFluid(800), Oxygen.getFluid(25))
                .duration(40)
                .EUt(VA[UEV])
                .save(provider);

        // Stage 2b: Distilled Purified Water + Hydrogen Peroxide -> UV Purified Water (High Speed AOP)
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("uv_purified_water_from_peroxide")
                .addData("waterPurificationTier", LuV)
                .addData("uvDose", 40)
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(800), HydrogenPeroxide.getFluid(50))
                .outputFluids(GTEMaterials.UvPurifiedWater.getFluid(800), Oxygen.getFluid(25))
                .duration(20)
                .EUt(VA[UEV])
                .save(provider);

        // Stage 3: UV Purified Water + Acid-Base Reagent + Mixed Bed Resin Beads -> Ultrapure Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("ultrapure_water")
                .addData("waterPurificationTier", ZPM)
                .addData("ediLoad", 800)
                .addData("ediRegeneration", false)
                .inputFluids(GTEMaterials.UvPurifiedWater.getFluid(800), GTEMaterials.ElectronicAcidBaseReagent.getFluid(20))
                .inputItems(GTEItems.MIXED_BED_RESIN_BEADS.asStack(1))
                .outputFluids(GTEMaterials.UltrapureWater.getFluid(800))
                .duration(30)
                .EUt(VA[UEV])
                .save(provider);

        // Cleaning removes stored ion load; it never produces water.
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("ultrapure_water_regeneration")
                .addData("waterPurificationTier", ZPM)
                .addData("ediLoad", 800)
                .addData("ediRegeneration", true)
                .inputFluids(GTEMaterials.UvPurifiedWater.getFluid(100), GTEMaterials.ElectronicAcidBaseReagent.getFluid(1))
                .duration(2)
                .EUt(VA[UEV])
                .save(provider);

        // ---------------------------------------------------------------
        // Controller assembly recipes
        // ---------------------------------------------------------------

        // Central Water Purification Plant
        ASSEMBLER_RECIPES.recipeBuilder("central_water_purification_plant")
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack(8))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(8))
                .inputItems(GTBlocks.CASING_STEEL_PIPE.asStack(8))
                .inputItems(GTBlocks.CASING_STAINLESS_TURBINE.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_UEV.asStack(4))
                .inputItems(CustomTags.UEV_CIRCUITS, 8)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(4))
                .inputFluids(DistilledWater.getFluid(16000))
                .outputItems(GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT)
                .duration(20 * 40)
                .EUt(VA[UEV])
                .save(provider);

        // Redstone-controlled heating / cooling hatch for T1
        ASSEMBLER_RECIPES.recipeBuilder("thermal_control_hatch")
                .inputItems(GTItems.ELECTRIC_PUMP_UEV.asStack(2))
                .inputItems(GTItems.SENSOR_UEV.asStack())
                .inputItems(CustomTags.UEV_CIRCUITS, 2)
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack())
                .inputItems(dust, Redstone, 4)
                .outputItems(GTEWaterPurificationParts.THERMAL_CONTROL_HATCH)
                .duration(200)
                .EUt(VA[UEV])
                .save(provider);

        // T1 Clarifier Purification Unit
        ASSEMBLER_RECIPES.recipeBuilder("thermal_signal_hatch")
                .inputItems(GTItems.SENSOR_UEV.asStack())
                .inputItems(GTItems.EMITTER_UEV.asStack())
                .inputItems(CustomTags.UEV_CIRCUITS, 2)
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack())
                .inputItems(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPARATOR))
                .inputItems(dust, Redstone, 4)
                .outputItems(GTEWaterPurificationParts.THERMAL_SIGNAL_HATCH)
                .duration(200)
                .EUt(VA[UEV])
                .save(provider);

        // T1 Clarifier Purification Unit
        ASSEMBLER_RECIPES.recipeBuilder("t1_clarifier_purification_unit")
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack(4))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(4))
                .inputItems(GTBlocks.CASING_STEEL_PIPE.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_UEV.asStack(2))
                .inputItems(CustomTags.UEV_CIRCUITS, 4)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(1))
                .inputFluids(Water.getFluid(8000))
                .outputItems(GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT)
                .duration(20 * 20)
                .EUt(VA[UEV])
                .save(provider);

        // Modular UV lamp: shares the unit's central-plant power supply.
        ASSEMBLER_RECIPES.recipeBuilder("uv_lamp_hatch")
                .inputItems(GTBlocks.CASING_PTFE_INERT.asStack())
                .inputItems(GTBlocks.CASING_LAMINATED_GLASS.asStack(2))
                .inputItems(GTItems.EMITTER_UEV.asStack(2))
                .inputItems(GTItems.SENSOR_UEV.asStack())
                .inputItems(CustomTags.UEV_CIRCUITS, 2)
                .outputItems(GTEWaterPurificationParts.UV_LAMP_HATCH)
                .duration(200)
                .EUt(VA[UEV])
                .save(provider);

        // T2 UV-Oxidation Purification Unit for the UEV Imaginary-series water supply.
        ASSEMBLER_RECIPES.recipeBuilder("t2_uv_oxidation_purification_unit")
                .inputItems(GTBlocks.CASING_PTFE_INERT.asStack(4))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(6))
                .inputItems(GTBlocks.CASING_LAMINATED_GLASS.asStack(6))
                .inputItems(GTItems.ELECTRIC_PUMP_UEV.asStack(2))
                .inputItems(CustomTags.UEV_CIRCUITS, 6)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(2))
                .inputFluids(HydrogenPeroxide.getFluid(4000))
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(8000))
                .outputItems(GTEWaterPurificationMachines.T2_UV_OXIDATION_PURIFICATION_UNIT)
                .duration(20 * 40)
                .EUt(VA[UEV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("edi_load_signal_hatch")
                .circuitMeta(1)
                .inputItems(GTItems.SENSOR_UEV.asStack())
                .inputItems(GTItems.EMITTER_UEV.asStack())
                .inputItems(CustomTags.UEV_CIRCUITS, 2)
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack())
                .inputItems(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPARATOR))
                .inputItems(dust, Redstone, 4)
                .outputItems(GTEWaterPurificationParts.EDI_LOAD_SIGNAL_HATCH)
                .duration(200)
                .EUt(VA[UEV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("edi_regeneration_control_hatch")
                .circuitMeta(2)
                .inputItems(GTItems.SENSOR_UEV.asStack())
                .inputItems(GTItems.EMITTER_UEV.asStack())
                .inputItems(CustomTags.UEV_CIRCUITS, 2)
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack())
                .inputItems(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPARATOR))
                .inputItems(dust, Redstone, 4)
                .outputItems(GTEWaterPurificationParts.EDI_REGENERATION_CONTROL_HATCH)
                .duration(200)
                .EUt(VA[UEV])
                .save(provider);

        // T3 EDI Ultrapure Purification Unit
        ASSEMBLER_RECIPES.recipeBuilder("t3_edi_ultrapure_purification_unit")
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack(6))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(6))
                .inputItems(GCYMBlocks.ELECTROLYTIC_CELL.asStack(6))
                .inputItems(GTBlocks.CASING_STEEL_PIPE.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_UEV.asStack(2))
                .inputItems(CustomTags.UEV_CIRCUITS, 6)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(4))
                .inputFluids(GTEMaterials.UvPurifiedWater.getFluid(8000))
                .outputItems(GTEWaterPurificationMachines.T3_EDI_ULTRAPURE_PURIFICATION_UNIT)
                .duration(20 * 60)
                .EUt(VA[UEV])
                .save(provider);
    }
}
