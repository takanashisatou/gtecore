package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import net.minecraft.network.chat.Component;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTERecipeTypes;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_MACHINES;

/** UHV Eight Trigrams upgrade of the existing 15 x 3 x 15 Super Fusion ring. */
public final class GTEFusionMachines {

    static {
        GTECore_REGISTRATE.creativeModeTab(() -> MORE_MACHINES);
    }

    private GTEFusionMachines() {}

    public static void init() {}

    public static final MultiblockMachineDefinition SUPER_FUSION_REACTOR_II = GTECore_REGISTRATE
            .multiblock("super_fusion_reactor_ii", WorkableElectricMultiblockMachine::new)
            .tier(GTValues.UHV)
            .rotationState(RotationState.ALL)
            .appearanceBlock(GTEBlocks.EIGHT_TRIGMAS_CASING)
            .recipeTypes(GTERecipeTypes.SUPER_FUSION_REACTOR_II_RECIPES,
                    GTERecipeTypes.SUPER_FUSION_REACTOR_RECIPE)
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH,
                    GTRecipeModifiers.OC_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            .pattern(definition -> {
                var casing = blocks(GTEBlocks.EIGHT_TRIGMAS_CASING.get()).setMinGlobalLimited(40);
                var ports = casing
                        .or(abilities(PartAbility.IMPORT_ITEMS))
                        .or(abilities(PartAbility.EXPORT_ITEMS))
                        .or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(4).setPreviewCount(4))
                        .or(abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                        .or(abilities(PartAbility.INPUT_ENERGY, PartAbility.INPUT_LASER)
                                .setMinGlobalLimited(1).setPreviewCount(1))
                        .or(abilities(PartAbility.MULTI_PARALLEL_HATCH).setMaxGlobalLimited(1));
                // Same occupied coordinates and controller position as Super Fusion I.
                return FactoryBlockPattern.start()
                        .aisle("###############", "######OGO######", "###############")
                        .aisle("######ICI######", "####GGAAAGG####", "######ICI######")
                        .aisle("####CC###CC####", "###EAAOGOAAE###", "####CC###CC####")
                        .aisle("###C#######C###", "##EKEG###GEKE##", "###C#######C###")
                        .aisle("##C#########C##", "#GAE#######EAG#", "##C#########C##")
                        .aisle("##C#########C##", "#GAG#######GAG#", "##C#########C##")
                        .aisle("#I###########I#", "OAO#########OAO", "#I###########I#")
                        .aisle("#C###########C#", "GAG#########GAG", "#C###########C#")
                        .aisle("#I###########I#", "OAO#########OAO", "#I###########I#")
                        .aisle("##C#########C##", "#GAG#######GAG#", "##C#########C##")
                        .aisle("##C#########C##", "#GAE#######EAG#", "##C#########C##")
                        .aisle("###C#######C###", "##EKEG###GEKE##", "###C#######C###")
                        .aisle("####CC###CC####", "###EAAOGOAAE###", "####CC###CC####")
                        .aisle("######ICI######", "####GGAAAGG####", "######ICI######")
                        .aisle("###############", "######OSO######", "###############")
                        .where('S', controller(blocks(definition.getBlock())))
                        .where('G', blocks(GTBlocks.FUSION_GLASS.get()).or(ports))
                        .where('C', ports)
                        .where('E', ports)
                        .where('O', ports)
                        .where('I', ports)
                        .where('K', blocks(GTEBlocks.YIN_YANG_COIL.get()))
                        .where('A', air())
                        .where('#', any())
                        .build();
            })
            .workableCasingModel(GTECore.id("block/casings/eight_trigmas/eight_trigmas_casing"),
                    GTCEu.id("block/multiblock/fusion_reactor"))
            .langValue("Super Fusion Reactor II")
            .tooltips(Component.translatable("com.gtecore.tooltips.super_fusion_reactor"),
                    Component.translatable("com.gtecore.tooltips.super_fusion_reactor_ii.0"),
                    Component.translatable("com.gtecore.tooltips.super_fusion_reactor_ii.1"),
                    Component.translatable("com.gtecore.tooltips.addbygtecore"))
            .register();
}
