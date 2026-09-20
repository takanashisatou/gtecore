package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import net.minecraft.data.recipes.FinishedRecipe;
import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEMultiMachines2;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES;
import static org.satou.gtecore.common.data.GTERecipeTypes.*;

/** UEV construction and crystal growth, bootstrapped by the existing Yin-Yang machines. */
public final class IMAGINARY_FOUNDATION_HANDLER {

    private IMAGINARY_FOUNDATION_HANDLER() {}

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        // Construction materials must be obtainable before the first Tree of Imaginary exists.
        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_casing")
                .inputItems(GTEBlocks.EIGHT_TRIGMAS_CASING, 8)
                .inputItems(plate, Neutronium, 8)
                .inputItems(GTItems.ELECTRIC_PUMP_UEV)
                .inputItems(GTEItems.YING, 16)
                .inputItems(GTEItems.YANG, 16)
                .inputItems(CustomTags.UEV_CIRCUITS, 2)
                .inputFluids(SolderingAlloy.getFluid(1152), Naquadria.getFluid(1000))
                .outputItems(GTEBlocks.IMAGINARY_CASING, 32)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_branch_casing")
                .inputItems(GTEBlocks.IMAGINARY_CASING, 4)
                .inputItems(frameGt, Neutronium, 4)
                .inputItems(rodLong, Tritanium, 8)
                .inputItems(GTItems.ELECTRIC_PISTON_UEV)
                .inputItems(GTEItems.RUNE_XUN, 8)
                .inputItems(GTEItems.YING, 8)
                .inputItems(GTEItems.YANG, 8)
                .inputFluids(SolderingAlloy.getFluid(1152))
                .outputItems(GTEBlocks.IMAGINARY_BRANCH_CASING, 32)
                .duration(20 * 20)
                .EUt(VA[UEV])
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_containment_casing")
                .inputItems(GTEBlocks.YIN_YANG_FIELD_RESTRICTION)
                .inputItems(GTEBlocks.IMAGINARY_CASING, 8)
                .inputItems(plate, Neutronium, 8)
                .inputItems(GTItems.FIELD_GENERATOR_UEV)
                .inputItems(GTEItems.RUNE_KUN, 8)
                .inputItems(GTEItems.RUNE_QIAN, 8)
                .inputFluids(SolderingAlloy.getFluid(2304), Naquadria.getFluid(1000))
                .outputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 16)
                .duration(20 * 45)
                .EUt(VA[UEV])
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_energy_conduit")
                .inputItems(GTEBlocks.IMAGINARY_CASING, 4)
                .inputItems(wireGtHex, Europium, 8)
                .inputItems(GTItems.EMITTER_UEV)
                .inputItems(GTEItems.YIN_YANG_CIRCUIT_CHIP, 8)
                .inputItems(GTEItems.RUNE_ZHEN, 8)
                .inputFluids(SolderingAlloy.getFluid(2304))
                .outputItems(GTEBlocks.IMAGINARY_ENERGY_CONDUIT, 16)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        KUN_GEN_STAR_HUB.recipeBuilder("imaginary_core_casing")
                .inputItems(GTEBlocks.IMAGINARY_CASING, 4)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 2)
                .inputItems(GTEItems.YIN_YANG_PROCESSOR_MAINFRAME_UIV, 2)
                .inputItems(GTItems.FIELD_GENERATOR_UEV, 2)
                .inputItems(GTItems.SENSOR_UEV, 2)
                .inputItems(GTEItems.GEN_CHIP, 8)
                .inputItems(GTEItems.DUI_CHIP, 8)
                .inputFluids(Naquadria.getFluid(2000), SolderingAlloy.getFluid(2304),
                        GTEMaterials.UltrapureWater.getFluid(2000))
                .outputItems(GTEBlocks.IMAGINARY_CORE_CASING, 4)
                .duration(20 * 60)
                .EUt(VA[UEV])
                .save(provider);

        RED_SUN_STAR_CORE.recipeBuilder("imaginary_glass")
                .inputItems(GTBlocks.CASING_LAMINATED_GLASS, 16)
                .inputItems(dust, SiliconDioxide, 16)
                .inputItems(GTEItems.YING, 16)
                .inputItems(GTEItems.YANG, 16)
                .inputItems(GTEItems.RUNE_QIAN, 8)
                .notConsumable(GTEItems.YIN_YANG_GLASS_LENS.asStack())
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(4000), Helium.getFluid(1000))
                .outputItems(GTEBlocks.IMAGINARY_GLASS, 32)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        KUN_GEN_STAR_HUB.recipeBuilder("imaginary_coil_block")
                .inputItems(GTEBlocks.YIN_YANG_COIL, 4)
                .inputItems(wireFine, Europium, 64)
                .inputItems(plate, Neutronium, 8)
                .inputItems(GTEItems.RUNE_LI, 8)
                .inputItems(GTEItems.RUNE_KAN, 8)
                .inputItems(GTItems.FIELD_GENERATOR_UEV)
                .inputFluids(Naquadria.getFluid(2000), SolderingAlloy.getFluid(1152))
                .outputItems(GTEBlocks.IMAGINARY_COIL, 8)
                .duration(20 * 45)
                .EUt(VA[UEV])
                .save(provider);

        // This renewable consumable is made outside the tree and is distinct from structural leaves.
        RED_SUN_STAR_CORE.recipeBuilder("imaginary_growth_medium")
                .inputItems(dust, Silicon, 16)
                .inputItems(GTEItems.YING, 8)
                .inputItems(GTEItems.YANG, 8)
                .inputItems(GTEItems.SYMBOL_PAPER_WOOD, 2)
                .inputItems(GTEItems.SYMBOL_PAPER_WATER, 2)
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(4000))
                .outputItems(GTEItems.IMAGINARY_GROWTH_MEDIUM, 32)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        // The fractal canopy uses thousands of leaves, so it is fabricated in full stacks.
        ASSEMBLY_LINE_RECIPES.recipeBuilder("imaginary_leaf_matrix")
                .inputItems(GTEBlocks.IMAGINARY_GLASS, 4)
                .inputItems(GTEBlocks.IMAGINARY_BRANCH_CASING, 4)
                .inputItems(GTEItems.IMAGINARY_GROWTH_MEDIUM, 8)
                .inputItems(GTEItems.RUNE_XUN, 4)
                .inputItems(GTEItems.RUNE_ZHEN, 4)
                .inputFluids(GTEMaterials.UltrapureWater.getFluid(4000), Naquadria.getFluid(1000))
                .outputItems(GTEBlocks.IMAGINARY_LEAF_MATRIX, 64)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("tree_of_imaginary")
                .inputItems(GTEBlocks.IMAGINARY_CORE_CASING, 4)
                .inputItems(GTEBlocks.IMAGINARY_BRANCH_CASING, 16)
                .inputItems(GTEBlocks.IMAGINARY_CONTAINMENT_CASING, 16)
                .inputItems(GTEBlocks.IMAGINARY_ENERGY_CONDUIT, 16)
                .inputItems(GTEBlocks.IMAGINARY_COIL, 8)
                .inputItems(CustomTags.UIV_CIRCUITS, 8)
                .inputItems(GTItems.ROBOT_ARM_UEV, 4)
                .inputItems(GTItems.SENSOR_UEV, 4)
                .inputItems(plate, Neutronium, 16)
                .inputItems(rod, NaquadahAlloy, 16)
                .inputFluids(Naquadria.getFluid(4000), SolderingAlloy.getFluid(5760))
                .scannerResearch(b -> b.researchStack(GTEBlocks.IMAGINARY_CORE_CASING.asStack())
                        .duration(600)
                        .EUt(VA[UEV]))
                .outputItems(GTEMultiMachines2.TREE_OF_IMAGINARY)
                .duration(20 * 600)
                .EUt(VA[UEV])
                .save(provider);

        TREE_OF_IMAGINARY.recipeBuilder("imaginary_tree_boule")
                .inputItems(dust, Silicon, 64)
                .inputItems(GTEItems.IMAGINARY_GROWTH_MEDIUM, 4)
                .inputFluids(Naquadria.getFluid(1000), GTEMaterials.UltrapureWater.getFluid(4000))
                .outputItems(GTEItems.IMAGINARY_TREE_BOULE, 4)
                .duration(20 * 30)
                .EUt(VA[UEV])
                .save(provider);

        TREE_OF_IMAGINARY.recipeBuilder("imaginary_tree_wafer")
                .inputItems(GTEItems.IMAGINARY_TREE_BOULE)
                .inputFluids(Lubricant.getFluid(1000), GTEMaterials.UltrapureWater.getFluid(1000))
                .outputItems(GTEItems.IMAGINARY_TREE_WAFER, 16)
                .duration(20 * 15)
                .EUt(VA[UEV])
                .save(provider);
    }
}
