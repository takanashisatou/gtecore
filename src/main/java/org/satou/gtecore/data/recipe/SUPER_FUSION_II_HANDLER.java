package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import net.minecraft.data.recipes.FinishedRecipe;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEFusionMachines;
import org.satou.gtecore.common.data.machines.GTEMultiMachine;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.plate;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES;

/** Bootstrap from UHV Eight Trigrams to Super Fusion II, then UEV and Imaginary casings. */
public final class SUPER_FUSION_II_HANDLER {

    private SUPER_FUSION_II_HANDLER() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        // No UEV casings, components or Imaginary products are needed to build the upgrade.
        ASSEMBLY_LINE_RECIPES.recipeBuilder("super_fusion_reactor_ii")
                .inputItems(GTEMultiMachine.Super_Fusion_Reactor)
                .inputItems(GTEBlocks.EIGHT_TRIGMAS_CASING, 16)
                .inputItems(GTEBlocks.YIN_YANG_FIELD_RESTRICTION, 4)
                .inputItems(GTEBlocks.YIN_YANG_COIL, 8)
                .inputItems(GTItems.FIELD_GENERATOR_UHV, 8)
                .inputItems(GTItems.SENSOR_UHV, 4)
                .inputItems(GTItems.EMITTER_UHV, 4)
                .inputItems(GTItems.ELECTRIC_PUMP_UHV, 4)
                .inputItems(GTItems.ROBOT_ARM_UHV, 4)
                .inputItems(CustomTags.UHV_CIRCUITS, 16)
                .inputItems(GTEItems.RUNE_QIAN, 16)
                .inputItems(GTEItems.RUNE_KUN, 16)
                .inputFluids(SolderingAlloy.getFluid(2304), Naquadria.getFluid(2000))
                .scannerResearch(b -> b.researchStack(GTEMultiMachine.Super_Fusion_Reactor.asStack())
                        .duration(1200).EUt(VA[UHV]))
                .outputItems(GTEFusionMachines.SUPER_FUSION_REACTOR_II)
                .duration(20 * 120)
                .EUt(VA[UHV])
                .save(provider);

        GTERecipeTypes.SUPER_FUSION_REACTOR_II_RECIPES.recipeBuilder("yin_yang_fusion_alloy")
                .inputFluids(Tritanium.getFluid(1152), Neutronium.getFluid(576),
                        GTEMaterials.Huo.getFluid(1000), GTEMaterials.ShuiYuanSu.getFluid(1000))
                .outputFluids(GTEMaterials.YinYangFusionAlloy.getFluid(2304))
                .duration(20 * 30)
                .EUt(VA[UHV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("uev_machine_casing")
                .inputItems(GTBlocks.MACHINE_CASING_UHV)
                .inputItems(plate, GTEMaterials.YinYangFusionAlloy, 8)
                .inputItems(plate, Tritanium, 4)
                .inputItems(GTEItems.YIN_YANG_CIRCUIT_CHIP, 4)
                .inputFluids(SolderingAlloy.getFluid(576))
                .circuitMeta(8)
                .outputItems(GTBlocks.MACHINE_CASING_UEV)
                .duration(20 * 30)
                .EUt(VA[UHV])
                .save(provider);
    }
}
