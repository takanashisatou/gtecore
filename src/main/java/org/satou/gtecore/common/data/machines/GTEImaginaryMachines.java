package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;

import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTERecipeTypes;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_MACHINES;

/** Dedicated wafer processing equipment for the imaginary circuit line. */
public final class GTEImaginaryMachines {

    static {
        GTECore_REGISTRATE.creativeModeTab(() -> MORE_MACHINES);
    }

    private GTEImaginaryMachines() {}

    public static void init() {}

    public static final MultiblockMachineDefinition IMAGINARY_LITHOGRAPHY_CENTER = GTECore_REGISTRATE
            .multiblock("imaginary_lithography_center", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTEBlocks.IMAGINARY_CASING)
            .recipeType(GTERecipeTypes.IMAGINARY_LITHOGRAPHY)
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH,
                    GTRecipeModifiers.OC_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            // 15 wide x 9 high x 13 deep. Default axes: LEFT, UP, FRONT.
            // Aisles run rear to front; every aisle lists rows bottom to top.
            // The sole controller is in the last aisle, at (7, 1), facing outward.
            // Twin glass capsules flank two octagonal optical frames above a stepped base.
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("...............", "...............", "...............", "...............", "...............", "...............", "...............", "...............", "...............")
                    .aisle("..AAAAAAAAAAA..", ".....BEEEB.....", "...............", "...............", "...............", "...............", "...............", "...............", "...............")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "RGGGR..E..RGGGR", "RGGGR..E..RGGGR", "RGGGR..E..RGGGR", "RRRRR..K..RRRRR", "...............", "...............", "...............")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "G#E#G#####G#E#G", "G###G#####G###G", "G###G#####G###G", "RBBBR#####RBBBR", ".....#####.....", ".....#####.....", "...............")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "GCECGR###RGCECG", "G#K#GRRRRRG#K#G", "G###GRCCCRG###G", "RBBBRRCGCRRBBBR", ".....RCCCR.....", ".....#RRR#.....", ".......K.......")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "G#E#G#####G#E#G", "G###G#####G###G", "G###G#####G###G", "RBBBR##G##RBBBR", ".....#####.....", ".....#####.....", ".......E.......")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "R#E#R#####R#E#R", "R###R#####R###R", "R###R#####R###R", "RBBBR##G##RBBBR", ".....#####.....", ".....#####.....", ".......E.......")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "G#E#G#####G#E#G", "G###G#####G###G", "G###G#####G###G", "RBBBR##G##RBBBR", ".....#####.....", ".....#####.....", ".......E.......")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "GCECGR###RGCECG", "G#K#GRRRRRG#K#G", "G###GRCCCRG###G", "RBBBRRCGCRRBBBR", ".....RCCCR.....", ".....#RRR#.....", ".......K.......")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "G#E#G#####G#E#G", "G###G#####G###G", "G###G#####G###G", "RBBBR#####RBBBR", ".....#####.....", ".....#####.....", "...............")
                    .aisle("ABBBBBBBBBBBBBA", "BBBBBBEEEBBBBBB", "RGGGR.....RGGGR", "RGGGR.....RGGGR", "RGGGR.....RGGGR", "RRRRR.....RRRRR", "...............", "...............", "...............")
                    .aisle("..AAAAAAAAAAA..", ".....BEEEB.....", "...............", "...............", "...............", "...............", "...............", "...............", "...............")
                    .aisle(".....AAAAA.....", ".....AASAA.....", "...............", "...............", "...............", "...............", "...............", "...............", "...............")
                    .where("S", controller(blocks(definition.getBlock())))
                    // Only exposed base edges and the front console accept parts.
                    .where("A", blocks(GTEBlocks.IMAGINARY_CASING.get()).setMinGlobalLimited(24)
                            .or(autoAbilities(definition.getRecipeTypes(), false, false, true, true, true, true))
                            .or(abilities(PartAbility.INPUT_ENERGY, PartAbility.INPUT_LASER)
                                    .setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                            .or(autoAbilities(true, false, false))
                            .or(abilities(PartAbility.MULTI_PARALLEL_HATCH).setMaxGlobalLimited(1)))
                    .where("B", blocks(GTEBlocks.IMAGINARY_CONTAINMENT_CASING.get()))
                    .where("R", blocks(GTEBlocks.IMAGINARY_BRANCH_CASING.get()))
                    .where("G", blocks(GTEBlocks.IMAGINARY_GLASS.get()))
                    .where("C", blocks(GTEBlocks.IMAGINARY_COIL.get()))
                    .where("K", blocks(GTEBlocks.IMAGINARY_CORE_CASING.get()))
                    .where("E", blocks(GTEBlocks.IMAGINARY_ENERGY_CONDUIT.get()))
                    .where("#", air()) // Process chambers and optical clearance must stay empty.
                    .where(".", any()) // Space outside the machine is unrestricted.
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.imaginary_lithography_center.0"),
                    Component.translatable("com.gtecore.tooltips.imaginary_lithography_center.1"))
            .workableCasingModel(GTECore.id("block/casings/imaginary/imaginary_casing"),
                    GTECore.id("block/multiblock/imaginary_lithography_center"))
            .register();

    public static final MultiblockMachineDefinition IMAGINARY_CIRCUIT_FABRICATOR = GTECore_REGISTRATE
            .multiblock("imaginary_circuit_fabricator", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTEBlocks.IMAGINARY_CASING)
            .recipeType(GTERecipeTypes.IMAGINARY_CIRCUIT_FABRICATION)
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH,
                    GTRecipeModifiers.OC_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            // 13 wide x 9 high x 13 deep. Default axes: LEFT, UP, FRONT.
            // Aisles run rear to front, rows bottom to top; controller at (6, 1, 12).
            // An octagonal lamination tower rises inside a U-shaped wet-processing gallery.
            // The low front console packages circuits; its edges and gallery feet accept parts.
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("AAAAAAAAAAAAA", "GGGGGGGGGGGGG", "GGGGGGGGGGGGG", "RRRRRRRRRRRRR", ".............", ".............", ".............", ".............", ".............")
                    .aisle("ABBBBBBBBBBBA", "GEEEEEEEEEEEG", "G###########G", "RBBBBBBBBBBBR", ".............", ".............", ".............", ".............", ".............")
                    .aisle("ABBAABBBAABBA", "GEEGGGGGGGEEG", "G##GGGGGGG##G", "RBBBBBBBBBBBR", ".............", ".............", ".............", ".............", ".............")
                    .aisle("ABA..BBB..ABA", "GEG..RRR..GEG", "G#G..GGG..G#G", "RBB..GGG..BBR", ".....RRR.....", ".....GGG.....", ".....GGG.....", ".....RRR.....", ".....BBB.....")
                    .aisle("ABA.BBBBB.ABA", "GEG.R###R.GEG", "G#G.GCCCG.G#G", "RBB.G###G.BBR", "....RCCCR....", "....G###G....", "....GCCCG....", "....R###R....", "....BRRRB....")
                    .aisle("ABA.BBBBB.ABA", "GEG.R#E#R.GEG", "G#G.GCKCG.G#G", "RRR.G#E#G.RRR", "....RCKCR....", "....G#E#G....", "....GCKCG....", "....R#E#R....", "....BRKRB....")
                    .aisle("ABA.BBBBB.ABA", "GEG.R###R.GEG", "G#G.GCCCG.G#G", "RBB.G###G.BBR", "....RCCCR....", "....G###G....", "....GCCCG....", "....R###R....", "....BRRRB....")
                    .aisle("ABA..BBB..ABA", "GEG..RRR..GEG", "G#G..GGG..G#G", "RBB..GGG..BBR", ".....RRR.....", ".....GGG.....", ".....GGG.....", ".....RRR.....", ".....BBB.....")
                    .aisle("ABA..BBB..ABA", "GEG..RER..GEG", "G#G.......G#G", "RBB.......BBR", ".............", ".............", ".............", ".............", ".............")
                    .aisle("ABA..BBB..ABA", "GEG..RER..GEG", "G#G.......G#G", "RBB.......BBR", ".............", ".............", ".............", ".............", ".............")
                    .aisle("AAAAABBBAAAAA", "GGGAABBBAAGGG", "GGGRRRRRRRGGG", "RRR.......RRR", ".............", ".............", ".............", ".............", ".............")
                    .aisle("...ABBBBBA...", "...ABBBBBA...", "...REEKEER...", ".............", ".............", ".............", ".............", ".............", ".............")
                    .aisle("...AAAAAAA...", "...AAASAAA...", "...RRRRRRR...", ".............", ".............", ".............", ".............", ".............", ".............")
                    .where("S", controller(blocks(definition.getBlock())))
                    // Only exposed gallery feet and packaging-console edges accept parts.
                    .where("A", blocks(GTEBlocks.IMAGINARY_CASING.get()).setMinGlobalLimited(24)
                            .or(autoAbilities(definition.getRecipeTypes(), false, false, true, true, true, true))
                            .or(abilities(PartAbility.INPUT_ENERGY, PartAbility.INPUT_LASER)
                                    .setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                            .or(autoAbilities(true, false, false))
                            .or(abilities(PartAbility.MULTI_PARALLEL_HATCH).setMaxGlobalLimited(1)))
                    .where("B", blocks(GTEBlocks.IMAGINARY_CONTAINMENT_CASING.get()))
                    .where("R", blocks(GTEBlocks.IMAGINARY_BRANCH_CASING.get()))
                    .where("G", blocks(GTEBlocks.IMAGINARY_GLASS.get()))
                    .where("C", blocks(GTEBlocks.IMAGINARY_COIL.get()))
                    .where("K", blocks(GTEBlocks.IMAGINARY_CORE_CASING.get()))
                    .where("E", blocks(GTEBlocks.IMAGINARY_ENERGY_CONDUIT.get()))
                    .where("#", air()) // Wet-processing channels and lamination clearance must stay empty.
                    .where(".", any()) // Space outside the machine is unrestricted.
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.imaginary_circuit_fabricator.0"),
                    Component.translatable("com.gtecore.tooltips.imaginary_circuit_fabricator.1"))
            .workableCasingModel(GTECore.id("block/casings/imaginary/imaginary_casing"),
                    GTECore.id("block/multiblock/imaginary_circuit_fabricator"))
            .register();
}
