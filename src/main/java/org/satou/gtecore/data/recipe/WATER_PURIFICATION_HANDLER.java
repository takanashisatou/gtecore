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
import org.satou.gtecore.common.data.machines.GTEMultiMachines2;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

public class WATER_PURIFICATION_HANDLER {

    /**
     * 净水产线配方。所有流量均为「单次并行」的用量，实际吞吐 = 该数值 × 实际并行度。
     */
    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        // Stage 1a: Raw Water -> Distilled Purified Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("distilled_purified_water_from_water")
                .inputFluids(Water.getFluid(1000))
                .outputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(800))
                .chancedOutput(dust, Salt, 1000, 500)
                .duration(80)
                .EUt(VA[EV])
                .save(provider);

        // Stage 1b: Distilled Water -> Distilled Purified Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("distilled_purified_water_from_distilled")
                .inputFluids(DistilledWater.getFluid(1000))
                .outputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(950))
                .chancedOutput(dust, Salt, 500, 250)
                .duration(40)
                .EUt(VA[EV])
                .save(provider);

        // Stage 2a: Distilled Purified Water + Oxygen -> UV Purified Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("uv_purified_water_from_oxygen")
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(800), Oxygen.getFluid(100))
                .outputFluids(GTEMaterials.UvPurifiedWater.getFluid(800))
                .duration(60)
                .EUt(VA[LuV])
                .save(provider);

        // Stage 2b: Distilled Purified Water + Hydrogen Peroxide -> UV Purified Water (Faster AOP Route)
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("uv_purified_water_from_peroxide")
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(800), HydrogenPeroxide.getFluid(50))
                .outputFluids(GTEMaterials.UvPurifiedWater.getFluid(800), Oxygen.getFluid(25))
                .duration(30)
                .EUt(VA[LuV])
                .save(provider);

        // Stage 3: UV Purified Water -> Ultrapure Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("ultrapure_water")
                .inputFluids(GTEMaterials.UvPurifiedWater.getFluid(800))
                .outputFluids(GTEMaterials.UltrapureWater.getFluid(800))
                .duration(30)
                .EUt(VA[ZPM])
                .save(provider);

        // ZLD (Zero Liquid Discharge) Regeneration: Recover spent UV water back to Ultrapure Water
        GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("ultrapure_water_regeneration")
                .inputFluids(GTEMaterials.UvPurifiedWater.getFluid(1000))
                .outputFluids(GTEMaterials.UltrapureWater.getFluid(950))
                .duration(20)
                .EUt(VA[ZPM])
                .save(provider);

        // ---------------------------------------------------------------
        // Controller assembly recipes
        // ---------------------------------------------------------------

        // Placeholder all-in-one machine
        ASSEMBLER_RECIPES.recipeBuilder("ultrapure_water_refinery")
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack(4))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(4))
                .inputItems(GTBlocks.CASING_STEEL_PIPE.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_EV.asStack(2))
                .inputItems(CustomTags.EV_CIRCUITS, 4)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(2))
                .inputFluids(DistilledWater.getFluid(4000))
                .outputItems(GTEMultiMachines2.ULTRA_PURE_WATER_REFINERY)
                .duration(20 * 20)
                .EUt(VA[EV])
                .save(provider);

        // Central Water Purification Plant
        ASSEMBLER_RECIPES.recipeBuilder("central_water_purification_plant")
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack(8))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(8))
                .inputItems(GTBlocks.CASING_STEEL_PIPE.asStack(8))
                .inputItems(GTEBlocks.IMAGINARY_GLASS.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_IV.asStack(4))
                .inputItems(CustomTags.IV_CIRCUITS, 8)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(4))
                .inputFluids(DistilledWater.getFluid(16000))
                .outputItems(GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT)
                .duration(20 * 40)
                .EUt(VA[IV])
                .save(provider);

        // T1 Clarifier Purification Unit
        ASSEMBLER_RECIPES.recipeBuilder("t1_clarifier_purification_unit")
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack(4))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(4))
                .inputItems(GTBlocks.CASING_STEEL_PIPE.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_EV.asStack(2))
                .inputItems(CustomTags.EV_CIRCUITS, 4)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(1))
                .inputFluids(Water.getFluid(8000))
                .outputItems(GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT)
                .duration(20 * 20)
                .EUt(VA[EV])
                .save(provider);

        // T2 UV-Oxidation Purification Unit
        ASSEMBLER_RECIPES.recipeBuilder("t2_uv_oxidation_purification_unit")
                .inputItems(GTEBlocks.KAN_SHUI_CASING.asStack(4))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(6))
                .inputItems(GTEBlocks.IMAGINARY_GLASS.asStack(6))
                .inputItems(GTItems.ELECTRIC_PUMP_LuV.asStack(2))
                .inputItems(CustomTags.LuV_CIRCUITS, 6)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(2))
                .inputFluids(HydrogenPeroxide.getFluid(4000))
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(8000))
                .outputItems(GTEWaterPurificationMachines.T2_UV_OXIDATION_PURIFICATION_UNIT)
                .duration(20 * 40)
                .EUt(VA[LuV])
                .save(provider);

        // T3 EDI Ultrapure Purification Unit
        ASSEMBLER_RECIPES.recipeBuilder("t3_edi_ultrapure_purification_unit")
                .inputItems(GCYMBlocks.CASING_WATERTIGHT.asStack(6))
                .inputItems(GTBlocks.CLEANROOM_GLASS.asStack(6))
                .inputItems(GTEBlocks.IMAGINARY_CASING.asStack(6))
                .inputItems(GTBlocks.CASING_STEEL_PIPE.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_ZPM.asStack(2))
                .inputItems(CustomTags.ZPM_CIRCUITS, 6)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER.asStack(4))
                .inputFluids(GTEMaterials.UvPurifiedWater.getFluid(8000))
                .outputItems(GTEWaterPurificationMachines.T3_EDI_ULTRAPURE_PURIFICATION_UNIT)
                .duration(20 * 60)
                .EUt(VA[ZPM])
                .save(provider);
    }
}
