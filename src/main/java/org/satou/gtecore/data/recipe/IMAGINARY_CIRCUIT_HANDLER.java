package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import net.minecraft.data.recipes.FinishedRecipe;
import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEImaginaryMachines;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.UEV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES;
import static org.satou.gtecore.common.data.GTERecipeTypes.IMAGINARY_CIRCUIT_FABRICATION;
import static org.satou.gtecore.common.data.GTERecipeTypes.TREE_OF_IMAGINARY;

/** Completes the imaginary circuit chain without requiring its own products to build the factory. */
public final class IMAGINARY_CIRCUIT_HANDLER {

    private IMAGINARY_CIRCUIT_HANDLER() {}

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_circuit_fabricator")
                .inputItems(GTEBlocks.IMAGINARY_CASING, 16)
                .inputItems(GTEBlocks.IMAGINARY_CORE_CASING, 2)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 8)
                .inputItems(GTEBlocks.IMAGINARY_COIL, 4)
                .inputItems(GTEBlocks.IMAGINARY_GLASS, 8)
                .inputItems(GTEBlocks.IMAGINARY_ENERGY_CONDUIT, 8)
                .inputItems(GTItems.ROBOT_ARM_UEV, 4)
                .inputItems(GTItems.ELECTRIC_PISTON_UEV, 4)
                .inputItems(GTItems.ELECTRIC_PUMP_UEV, 4)
                .inputItems(CustomTags.UIV_CIRCUITS, 4)
                .inputItems(GTEItems.IMAGINARY_TREE_CPU_CHIP, 4)
                .inputItems(GTEItems.IMAGINARY_TREE_CIRCUIT_CHIP, 8)
                .inputFluids(SolderingAlloy.getFluid(5760), Polybenzimidazole.getFluid(1152),
                        GTEMaterials.UltrapureWater.getFluid(4000))
                .scannerResearch(b -> b.researchStack(GTEItems.IMAGINARY_TREE_CPU_CHIP.asStack())
                        .duration(600)
                        .EUt(VA[UEV]))
                .outputItems(GTEImaginaryMachines.IMAGINARY_CIRCUIT_FABRICATOR)
                .duration(20 * 120)
                .EUt(VA[UEV])
                .save(provider);

        IMAGINARY_CIRCUIT_FABRICATION.recipeBuilder("imaginary_tree_circuit_board")
                .inputItems(plate, ReinforcedEpoxyResin, 4)
                .inputItems(GTEItems.IMAGINARY_GROWTH_MEDIUM, 2)
                .inputItems(foil, Europium, 16)
                .inputFluids(Polybenzimidazole.getFluid(576), GTEMaterials.UltrapureWater.getFluid(2000))
                .outputItems(GTEItems.IMAGINARY_TREE_CIRCUIT_BOARD, 4)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        IMAGINARY_CIRCUIT_FABRICATION.recipeBuilder("imaginary_tree_printed_circuit_board")
                .inputItems(GTEItems.IMAGINARY_TREE_CIRCUIT_BOARD)
                .inputItems(foil, Europium, 8)
                .inputItems(foil, Gold, 8)
                .inputFluids(Iron3Chloride.getFluid(250), GTEMaterials.UltrapureWater.getFluid(1000))
                .outputItems(GTEItems.IMAGINARY_TREE_PRINTED_CIRCUIT_BOARD)
                .duration(20 * 20)
                .EUt(VA[UEV])
                .save(provider);

        IMAGINARY_CIRCUIT_FABRICATION.recipeBuilder("imaginary_tree_soc")
                .inputItems(GTEItems.IMAGINARY_TREE_CPU_CHIP, 2)
                .inputItems(GTEItems.IMAGINARY_TREE_CIRCUIT_CHIP, 4)
                .inputItems(GTEItems.IMAGINARY_TREE_PRINTED_CIRCUIT_BOARD)
                .inputItems(wireFine, Europium, 8)
                .inputFluids(SolderingAlloy.getFluid(288), GTEMaterials.UltrapureWater.getFluid(2000))
                .outputItems(GTEItems.IMAGINARY_TREE_SOC, 2)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        // Circuit tags describe the output grade; the entire imaginary production line runs at UEV.
        TREE_OF_IMAGINARY.recipeBuilder("imaginary_tree_processor_uhv")
                .inputItems(GTEItems.IMAGINARY_TREE_PRINTED_CIRCUIT_BOARD)
                .inputItems(GTEItems.IMAGINARY_TREE_CPU_CHIP, 4)
                .inputItems(GTEItems.IMAGINARY_TREE_CIRCUIT_CHIP, 8)
                // Tritanium has a generated fine-wire form; Neutronium does not.
                .inputItems(wireFine, Tritanium, 16)
                .inputFluids(SolderingAlloy.getFluid(288), GTEMaterials.UltrapureWater.getFluid(1000))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_UHV, 4)
                .duration(20 * 20)
                .EUt(VA[UEV])
                .save(provider);

        TREE_OF_IMAGINARY.recipeBuilder("imaginary_tree_processor_assembly_uev")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_UHV, 4)
                .inputItems(GTEItems.IMAGINARY_TREE_SOC, 4)
                .inputItems(plate, Neutronium, 2)
                .inputFluids(SolderingAlloy.getFluid(576), GTEMaterials.UltrapureWater.getFluid(2000))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_ASSEMBLY_UEV, 2)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        TREE_OF_IMAGINARY.recipeBuilder("imaginary_tree_processor_computer_uiv")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_ASSEMBLY_UEV, 4)
                .inputItems(GTEBlocks.IMAGINARY_CORE_CASING)
                .inputItems(plate, Neutronium, 4)
                .inputFluids(SolderingAlloy.getFluid(1152))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_COMPUTER_UIV)
                .duration(20 * 45)
                .EUt(VA[UEV])
                .save(provider);

        TREE_OF_IMAGINARY.recipeBuilder("imaginary_tree_processor_mainframe_uxv")
                .inputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_COMPUTER_UIV, 4)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 2)
                .inputItems(plate, Neutronium, 8)
                .inputFluids(SolderingAlloy.getFluid(2304))
                .outputItems(GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV)
                .duration(20 * 60)
                .EUt(VA[UEV])
                .save(provider);
    }
}
