package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.common.data.GTItems;
import net.minecraft.data.recipes.FinishedRecipe;
import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEImaginaryExpansionMachines;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.UEV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES;
import static org.satou.gtecore.common.data.GTERecipeTypes.*;

/** Post-Mainframe factories. Their exclusive recipes preserve all upstream consumables. */
public final class IMAGINARY_EXPANSION_HANDLER {

    private IMAGINARY_EXPANSION_HANDLER() {}

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        // Specific Mainframes, not a generic circuit tag: a new use for the Imaginary line's endpoint.
        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_crystal_growth_array")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV, 4)
                .inputItems(GTEBlocks.IMAGINARY_CORE_CASING, 8)
                .inputItems(GTEBlocks.IMAGINARY_BRANCH_CASING, 32)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 16)
                .inputItems(GTEBlocks.IMAGINARY_COIL, 16)
                .inputItems(GTEBlocks.IMAGINARY_GLASS, 32)
                .inputItems(GTEBlocks.IMAGINARY_ENERGY_CONDUIT, 16)
                .inputItems(GTItems.ELECTRIC_PUMP_UEV, 8)
                .inputItems(GTItems.ROBOT_ARM_UEV, 4)
                .inputItems(GTItems.FIELD_GENERATOR_UEV, 4)
                .inputFluids(SolderingAlloy.getFluid(9216), Naquadria.getFluid(8000),
                        GTEMaterials.UltrapureWater.getFluid(16000))
                .scannerResearch(b -> b.researchStack(GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV.asStack())
                        .duration(1200).EUt(VA[UEV]))
                .outputItems(GTEImaginaryExpansionMachines.IMAGINARY_CRYSTAL_GROWTH_ARRAY)
                .duration(20 * 180).EUt(VA[UEV]).save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_circuit_integration_matrix")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV, 8)
                .inputItems(GTEBlocks.IMAGINARY_CORE_CASING, 16)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 32)
                .inputItems(GTEBlocks.IMAGINARY_COIL, 16)
                .inputItems(GTEBlocks.IMAGINARY_ENERGY_CONDUIT, 32)
                .inputItems(GTEBlocks.IMAGINARY_GLASS, 16)
                .inputItems(GTItems.ROBOT_ARM_UEV, 8)
                .inputItems(GTItems.EMITTER_UEV, 8)
                .inputItems(GTItems.SENSOR_UEV, 8)
                .inputItems(GTEItems.IMAGINARY_TREE_SOC, 16)
                .inputFluids(SolderingAlloy.getFluid(18432), Polybenzimidazole.getFluid(4608),
                        GTEMaterials.UltrapureWater.getFluid(16000))
                .scannerResearch(b -> b.researchStack(GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV.asStack())
                        .duration(1200).EUt(VA[UEV]))
                .outputItems(GTEImaginaryExpansionMachines.IMAGINARY_CIRCUIT_INTEGRATION_MATRIX)
                .duration(20 * 240).EUt(VA[UEV]).save(provider);

        // Two ordinary batches' consumables produce three batches' output (50% higher yield).
        IMAGINARY_CRYSTAL_GROWTH.recipeBuilder("imaginary_growth_medium_bulk")
                .inputItems(dust, Silicon, 32)
                .inputItems(GTEItems.YING, 16).inputItems(GTEItems.YANG, 16)
                .inputItems(GTEItems.SYMBOL_PAPER_WOOD, 4).inputItems(GTEItems.SYMBOL_PAPER_WATER, 4)
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(8000))
                .outputItems(GTEItems.IMAGINARY_GROWTH_MEDIUM, 96)
                .duration(20 * 60).EUt(VA[UEV]).save(provider);
        IMAGINARY_CRYSTAL_GROWTH.recipeBuilder("imaginary_tree_boule_bulk")
                .inputItems(dust, Silicon, 128).inputItems(GTEItems.IMAGINARY_GROWTH_MEDIUM, 8)
                .inputFluids(Naquadria.getFluid(2000), GTEMaterials.UltrapureWater.getFluid(8000))
                .outputItems(GTEItems.IMAGINARY_TREE_BOULE, 12)
                .duration(20 * 60).EUt(VA[UEV]).save(provider);
        // Four boules yield 96 wafers instead of 64; both fluids keep the ordinary per-boule cost.
        IMAGINARY_CRYSTAL_GROWTH.recipeBuilder("imaginary_tree_wafer_bulk")
                .inputItems(GTEItems.IMAGINARY_TREE_BOULE, 4)
                .inputFluids(Lubricant.getFluid(4000), GTEMaterials.UltrapureWater.getFluid(4000))
                .outputItems(GTEItems.IMAGINARY_TREE_WAFER, 96)
                .duration(20 * 60).EUt(VA[UEV]).save(provider);

        IMAGINARY_CIRCUIT_INTEGRATION.recipeBuilder("imaginary_tree_processor_bulk")
                .inputItems(GTEItems.IMAGINARY_TREE_PRINTED_CIRCUIT_BOARD, 2)
                .inputItems(GTEItems.IMAGINARY_TREE_CPU_CHIP, 8)
                .inputItems(GTEItems.IMAGINARY_TREE_CIRCUIT_CHIP, 16)
                .inputItems(wireFine, Tritanium, 32)
                .inputFluids(SolderingAlloy.getFluid(576), GTEMaterials.UltrapureWater.getFluid(2000))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_UHV, 12)
                .duration(20 * 40).EUt(VA[UEV]).save(provider);
        IMAGINARY_CIRCUIT_INTEGRATION.recipeBuilder("imaginary_tree_processor_assembly_bulk")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_UHV, 8)
                .inputItems(GTEItems.IMAGINARY_TREE_SOC, 8).inputItems(plate, Neutronium, 4)
                .inputFluids(SolderingAlloy.getFluid(1152), GTEMaterials.UltrapureWater.getFluid(4000))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_ASSEMBLY_UEV, 6)
                .duration(20 * 60).EUt(VA[UEV]).save(provider);
        IMAGINARY_CIRCUIT_INTEGRATION.recipeBuilder("imaginary_tree_processor_computer_bulk")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_ASSEMBLY_UEV, 8)
                .inputItems(GTEBlocks.IMAGINARY_CORE_CASING, 2).inputItems(plate, Neutronium, 8)
                .inputFluids(SolderingAlloy.getFluid(2304))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_COMPUTER_UIV, 3)
                .duration(20 * 90).EUt(VA[UEV]).save(provider);
        IMAGINARY_CIRCUIT_INTEGRATION.recipeBuilder("imaginary_tree_processor_mainframe_bulk")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_COMPUTER_UIV, 8)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 4).inputItems(plate, Neutronium, 16)
                .inputFluids(SolderingAlloy.getFluid(4608))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV, 3)
                .duration(20 * 120).EUt(VA[UEV]).save(provider);
    }
}
