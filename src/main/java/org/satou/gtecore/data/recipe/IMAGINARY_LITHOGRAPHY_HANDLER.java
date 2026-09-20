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
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.wireFine;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES;
import static org.satou.gtecore.common.data.GTERecipeTypes.IMAGINARY_LITHOGRAPHY;

/** The dedicated UEV wafer/chip branch after the first Tree of Imaginary. */
public final class IMAGINARY_LITHOGRAPHY_HANDLER {

    private IMAGINARY_LITHOGRAPHY_HANDLER() {}

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        // Only established construction materials, prior-generation circuits and regular wafers
        // are used here: none of this center's own chip products are needed to build the first one.
        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_lithography_center")
                .inputItems(GTEBlocks.IMAGINARY_CORE_CASING, 2)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 8)
                .inputItems(GTEBlocks.IMAGINARY_GLASS, 16)
                .inputItems(GTEBlocks.IMAGINARY_COIL, 4)
                .inputItems(GTEBlocks.IMAGINARY_ENERGY_CONDUIT, 8)
                .inputItems(GTItems.EMITTER_UEV, 4)
                .inputItems(GTItems.SENSOR_UEV, 4)
                .inputItems(GTItems.ROBOT_ARM_UEV, 2)
                .inputItems(CustomTags.UIV_CIRCUITS, 4)
                .inputItems(GTEItems.IMAGINARY_TREE_WAFER, 4)
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(4000), SolderingAlloy.getFluid(2304),
                        Lubricant.getFluid(2000))
                .scannerResearch(b -> b.researchStack(GTEItems.IMAGINARY_TREE_WAFER.asStack())
                        .duration(600)
                        .EUt(VA[UEV]))
                .outputItems(GTEImaginaryMachines.IMAGINARY_LITHOGRAPHY_CENTER)
                .duration(20 * 120)
                .EUt(VA[UEV])
                .save(provider);

        // Circuit branch: expose the wafer, then dice the patterned circuit chips.
        IMAGINARY_LITHOGRAPHY.recipeBuilder("imaginary_tree_cpu_wafer")
                .inputItems(GTEItems.IMAGINARY_TREE_WAFER)
                .notConsumable(GTEItems.YIN_YANG_GLASS_LENS.asStack())
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(2000), HydrogenPeroxide.getFluid(250))
                .outputItems(GTEItems.IMAGINARY_TREE_CPU_WAFER)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        IMAGINARY_LITHOGRAPHY.recipeBuilder("imaginary_tree_circuit_chip")
                .inputItems(GTEItems.IMAGINARY_TREE_CPU_WAFER)
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(1000), Lubricant.getFluid(250))
                .outputItems(GTEItems.IMAGINARY_TREE_CIRCUIT_CHIP, 16)
                .duration(20 * 10)
                .EUt(VA[UEV])
                .save(provider);

        // CPU branch: prepare individual blanks, engrave them, and finish the interconnects.
        IMAGINARY_LITHOGRAPHY.recipeBuilder("raw_imaginary_tree_chip")
                .inputItems(GTEItems.IMAGINARY_TREE_WAFER)
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(1000), Lubricant.getFluid(250))
                .outputItems(GTEItems.RAW_IMAGINARY_TREE_CHIP, 4)
                .duration(20 * 15)
                .EUt(VA[UEV])
                .save(provider);

        IMAGINARY_LITHOGRAPHY.recipeBuilder("engraved_imaginary_tree_chip")
                .inputItems(GTEItems.RAW_IMAGINARY_TREE_CHIP)
                .notConsumable(GTEItems.YIN_YANG_GLASS_LENS.asStack())
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(500), HydrofluoricAcid.getFluid(100))
                .outputItems(GTEItems.ENGRAVED_IMAGINARY_TREE_CHIP)
                .duration(20 * 20)
                .EUt(VA[UEV])
                .save(provider);

        IMAGINARY_LITHOGRAPHY.recipeBuilder("imaginary_tree_cpu_chip")
                .inputItems(GTEItems.ENGRAVED_IMAGINARY_TREE_CHIP)
                .inputItems(wireFine, Europium, 4)
                .inputFluids(SolderingAlloy.getFluid(144), GTEMaterials.UltrapureWater.getFluid(500))
                .outputItems(GTEItems.IMAGINARY_TREE_CPU_CHIP)
                .duration(20 * 20)
                .EUt(VA[UEV])
                .save(provider);
    }
}
