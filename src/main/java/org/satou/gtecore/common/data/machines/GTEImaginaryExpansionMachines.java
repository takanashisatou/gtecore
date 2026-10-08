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

/** Two post-Mainframe factories with exclusive, resource-efficient UEV production recipes. */
public final class GTEImaginaryExpansionMachines {
    static {
        GTECore_REGISTRATE.creativeModeTab(() -> MORE_MACHINES);
    }

    private GTEImaginaryExpansionMachines() {}

    public static void init() {}

    // 19 wide x 13 high x 11 deep. Aisles run rear to front, rows bottom to top.
    // Twin glass growth towers, leaf caps and a shared transfer spine. Only the exposed skid/console A positions accept machine parts.
    public static final MultiblockMachineDefinition IMAGINARY_CRYSTAL_GROWTH_ARRAY = GTECore_REGISTRATE
            .multiblock("imaginary_crystal_growth_array", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTEBlocks.IMAGINARY_CASING)
            .recipeType(GTERecipeTypes.IMAGINARY_CRYSTAL_GROWTH)
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH,
                    GTRecipeModifiers.OC_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................")
                    .aisle("...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................")
                    .aisle("...AAAAA...AAAAA...", "....BBB.....BBB....", "....GRG.....GRG....", "....GRG.....GRG....", "....RRR.....RRR....", "....GRG.....GRG....", "....GRG.....GRG....", "....GRG.....GRG....", "....RRR.....RRR....", "....GRG.....GRG....", "....GRG.....GRG....", "....BBB.....BBB....", "...................")
                    .aisle("..AAAAAAA.AAAAAAA..", "...BBCBB...BBCBB...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...RR#RR...RR#RR...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...RR#RR...RR#RR...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...BBCBB...BBCBB...", "....LLL.....LLL....")
                    .aisle("..AAAAAAA.AAAAAAA..", "..BBCCCBB.BBCCCBB..", "..GG###GG.GG###GG..", "..GGCCCGG.GGCCCGG..", "..RR###RR.RR###RR..", "..GG###GG.GG###GG..", "..GG###GG.GG###GG..", "..GGCCCGG.GGCCCGG..", "..RR###RR.RR###RR..", "..GG###GG.GG###GG..", "..GG###GG.GG###GG..", "..BBCCCBB.BBCCCBB..", "...LLLLL...LLLLL...")
                    .aisle("..AAAAAAAAAAAAAAA..", "..BCCCCCBEBCCCCCB..", "..R##E##R.R##E##R..", "..R#CEC#R.R#CEC#R..", "..R##E##R.R##E##R..", "..R##E##R.R##E##R..", "..R##K##R.R##K##R..", "..R#CEC#R.R#CEC#R..", "..R##E##R.R##E##R..", "..R##E##R.R##E##R..", "..R##E##R.R##E##R..", "..BCCCCCB.BCCCCCB..", "...LLKLL...LLKLL...")
                    .aisle("..AAAAAAAAAAAAAAA..", "..BBCCCBBEBBCCCBB..", "..GG###GG.GG###GG..", "..GGCCCGG.GGCCCGG..", "..RR###RR.RR###RR..", "..GG###GG.GG###GG..", "..GG###GG.GG###GG..", "..GGCCCGG.GGCCCGG..", "..RR###RR.RR###RR..", "..GG###GG.GG###GG..", "..GG###GG.GG###GG..", "..BBCCCBB.BBCCCBB..", "...LLLLL...LLLLL...")
                    .aisle("..AAAAAAAAAAAAAAA..", "...BBCBB.E.BBCBB...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...RR#RR...RR#RR...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...RR#RR...RR#RR...", "...GG#GG...GG#GG...", "...GG#GG...GG#GG...", "...BBCBB...BBCBB...", "....LLL.....LLL....")
                    .aisle("...AAAAAAAAAAAAA...", "....BBB..E..BBB....", "....GRG.....GRG....", "....GRG.....GRG....", "....RRR.....RRR....", "....GRG.....GRG....", "....GRG.....GRG....", "....GRG.....GRG....", "....RRR.....RRR....", "....GRG.....GRG....", "....GRG.....GRG....", "....BBB.....BBB....", "...................")
                    .aisle("........AAA........", ".........E.........", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................")
                    .aisle("......AAAAAAA......", "......AAASAAA......", "......RRRRRRR......", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................", "...................")
                    .where("S", controller(blocks(definition.getBlock())))
                    .where("A", blocks(GTEBlocks.IMAGINARY_CASING.get()).setMinGlobalLimited(32)
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
                    .where("L", blocks(GTEBlocks.IMAGINARY_LEAF_MATRIX.get()))
                    .where("#", air())
                    .where(".", any())
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.imaginary_crystal_growth_array.0"),
                    Component.translatable("com.gtecore.tooltips.imaginary_crystal_growth_array.1"),
                    Component.translatable("com.gtecore.tooltips.imaginary_crystal_growth_array.2"))
            .workableCasingModel(GTECore.id("block/casings/imaginary/imaginary_casing"),
                    GTECore.id("block/multiblock/imaginary_lithography_center"))
            .register();

    // 17 wide x 9 high x 17 deep. Aisles run rear to front, rows bottom to top.
    // An open energy ring surrounds four compute pylons and the integration core. Only the exposed skid/console A positions accept machine parts.
    public static final MultiblockMachineDefinition IMAGINARY_CIRCUIT_INTEGRATION_MATRIX = GTECore_REGISTRATE
            .multiblock("imaginary_circuit_integration_matrix", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTEBlocks.IMAGINARY_CASING)
            .recipeType(GTERecipeTypes.IMAGINARY_CIRCUIT_INTEGRATION)
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH,
                    GTRecipeModifiers.OC_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle(".................", ".................", ".................", ".................", ".................", ".................", ".................", ".................", ".................")
                    .aisle("......AAAAA......", "......BBBBB......", "......GGGGG......", "......RRRRR......", "......CCCCC......", "......BBBBB......", ".................", ".................", ".................")
                    .aisle("....AAAAAAAAA....", "....BBBBBBBBB....", "....GEEEEEEEG....", "....RRRRRRRRR....", "....CCCCCCCCC....", "....BBBBBBBBB....", ".................", ".................", ".................")
                    .aisle("...AAA.....AAA...", "...RGR.....RGR...", "...RGR.....RGR...", "...RCR.....RCR...", "...RGR.....RGR...", "...RGR.....RGR...", "...RCR.....RCR...", "...RGR.....RGR...", "...RRR.....RRR...")
                    .aisle("..AAAA.....AAAA..", "..BGEG.....GEGB..", "..GGEG.....GEGG..", "..RCKC.....CKCR..", "..CGEG.....GEGC..", "..BGEG.....GEGB..", "...CKC.....CKC...", "...GEG.....GEG...", "...RRR.....RRR...")
                    .aisle("..AAAAAAAAAAAAA..", "..BRGR.....RGRB..", "..ERGR.....RGRE..", "..RRCR..E..RCRR..", "..CRGR.....RGRC..", "..BRGR.....RGRB..", "...RCR.....RCR...", "...RGR.....RGR...", "...RRR.....RRR...")
                    .aisle(".AA..AAAAAAA..AA.", ".BB...BBBBB...BB.", ".GE...CCCCC...EG.", ".RR.....E.....RR.", ".CC...........CC.", ".BB...........BB.", ".................", ".................", ".................")
                    .aisle(".AA..AAAAAAA..AA.", ".BB...BBBBB...BB.", ".GE...C###C...EG.", ".RR.....E.....RR.", ".CC...........CC.", ".BB...........BB.", ".................", ".................", ".................")
                    .aisle(".AA..AAAAAAA..AA.", ".BB...BBBBB...BB.", ".GE...C#K#C...EG.", ".RR..EEEEEEE..RR.", ".CC...........CC.", ".BB...........BB.", ".................", ".................", ".................")
                    .aisle(".AA..AAAAAAA..AA.", ".BB...BBBBB...BB.", ".GE...C###C...EG.", ".RR.....E.....RR.", ".CC...........CC.", ".BB...........BB.", ".................", ".................", ".................")
                    .aisle(".AA..AAAAAAA..AA.", ".BB...BBBBB...BB.", ".GE...CCCCC...EG.", ".RR.....E.....RR.", ".CC...........CC.", ".BB...........BB.", ".................", ".................", ".................")
                    .aisle("..AAAAAAAAAAAAA..", "..BRGR.....RGRB..", "..ERGR.....RGRE..", "..RRCR..E..RCRR..", "..CRGR.....RGRC..", "..BRGR.....RGRB..", "...RCR.....RCR...", "...RGR.....RGR...", "...RRR.....RRR...")
                    .aisle("..AAAA.AAA.AAAA..", "..BGEG.....GEGB..", "..GGEG.....GEGG..", "..RCKC.....CKCR..", "..CGEG.....GEGC..", "..BGEG.....GEGB..", "...CKC.....CKC...", "...GEG.....GEG...", "...RRR.....RRR...")
                    .aisle("...AAA.AAA.AAA...", "...RGR.....RGR...", "...RGR.....RGR...", "...RCR.....RCR...", "...RGR.....RGR...", "...RGR.....RGR...", "...RCR.....RCR...", "...RGR.....RGR...", "...RRR.....RRR...")
                    .aisle("....AAAAAAAAA....", "....BBBBBBBBB....", "....GEEEEEEEG....", "....RRRRRRRRR....", "....CCCCCCCCC....", "....BBBBBBBBB....", ".................", ".................", ".................")
                    .aisle("......AAAAA......", "......BBBBB......", "......GGGGG......", "......RRRRR......", "......CCCCC......", "......BBBBB......", ".................", ".................", ".................")
                    .aisle(".....AAAAAAA.....", ".....AAASAAA.....", ".....RRRRRRR.....", ".................", ".................", ".................", ".................", ".................", ".................")
                    .where("S", controller(blocks(definition.getBlock())))
                    .where("A", blocks(GTEBlocks.IMAGINARY_CASING.get()).setMinGlobalLimited(32)
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

                    .where("#", air())
                    .where(".", any())
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.imaginary_circuit_integration_matrix.0"),
                    Component.translatable("com.gtecore.tooltips.imaginary_circuit_integration_matrix.1"),
                    Component.translatable("com.gtecore.tooltips.imaginary_circuit_integration_matrix.2"))
            .workableCasingModel(GTECore.id("block/casings/imaginary/imaginary_casing"),
                    GTECore.id("block/multiblock/imaginary_circuit_fabricator"))
            .register();
}
