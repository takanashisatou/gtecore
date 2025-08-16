package org.satou.gtecore;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderHelper;
import com.gregtechceu.gtceu.common.block.BoilerFireboxType;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;
import com.gregtechceu.gtceu.common.registry.GTRegistration;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.satou.gtecore.common.registry.GTECoreRegistration;

import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.GTBlocks.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeModifiers.BATCH_MODE;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;
import static org.satou.gtecore.CreativeModeTabs.*;
import static org.satou.gtecore.GTERecipeTypes.Component_Factory;

public class GTEMultiMachine {
    public static void init() {

    }
    public static TraceabilityPredicate easy(String a) {
        return Predicates.blocks(new Block[]{(Block)ForgeRegistries.BLOCKS.getValue(new ResourceLocation(a))});
    }
    public static BlockEntry<Block> steel_pipe_casing = createCasingBlock("steel_pipe_casing",new ResourceLocation("gtceu","steel_pipe_casing"));
    //public static BlockEntry<Block> aluminium_frame = createCasingBlock("aluminium_frame",new ResourceLocation("gtceu","aluminium_frame"));
    public static BlockEntry<Block> industral = createCasingBlock("industrial_steam_casing",new ResourceLocation("gtceu","industrial_steam_casing"));
    public static BlockEntry<Block> steam_machine_casing = createCasingBlock("steam_machine_casing", GTCEu.id("block/casings/steam/bronze"));
    public static ResourceLocation id = new ResourceLocation("gtceu", "steam_machine_casing");
    public static final MultiblockMachineDefinition big_steam_alloy_smelter = REGISTRATE
            .multiblock("big_alloy", SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .recipeTypes(GTRecipeTypes.ALLOY_SMELTER_RECIPES)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBB","BBB","BBB")
                    .aisle("BBB","BAB","BBB")
                    .aisle("BBB","B#B","BBB")
                    .where("A", Predicates.air())
                    .where("#",controller(blocks(definition.getBlock())))
                    .where("B",blocks(steam_machine_casing.get()).setMinGlobalLimited(6)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                    )

                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
            .modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE)
            .model(createWorkableCasingMachineModel(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/steam_oven"))
                    .andThen(b -> b.addDynamicRenderer(
                            () -> DynamicRenderHelper.makeBoilerPartRender(
                                    BoilerFireboxType.BRONZE_FIREBOX, CASING_BRONZE_BRICKS))))
            .recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .register();

    public static final MultiblockMachineDefinition big_steam_compressor = REGISTRATE
            .multiblock("big_compressor", SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .recipeTypes(GTRecipeTypes.COMPRESSOR_RECIPES)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBB","BBB","BBB")
                    .aisle("BBB","BAB","BBB")
                    .aisle("BBB","B#B","BBB")
                    .where("A", Predicates.air())
                    .where("#",controller(blocks(definition.getBlock())))
                    .where("B",blocks(steam_machine_casing.get()).setMinGlobalLimited(6)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                    )
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
            .modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE)
            .model(createWorkableCasingMachineModel(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/steam_oven"))
                    .andThen(b -> b.addDynamicRenderer(
                            () -> DynamicRenderHelper.makeBoilerPartRender(
                                    BoilerFireboxType.BRONZE_FIREBOX, CASING_BRONZE_BRICKS))))
            .recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .register();
    public static final MultiblockMachineDefinition Easy_Box = REGISTRATE
            .multiblock("easy_box",SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .recipeTypes(GTERecipeTypes.EASY_BOX)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","................","................")
                    .aisle("ACDEFGHIJKLMNOPA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ACDEFGHIJKLMNOPA","................","................")
                    .aisle("ADEFGHIJKLMNOPOA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ADEFGHIJKLMNOPOA","................","................")
                    .aisle("AEFGHIJKLMNOPONA","A..............A","A..............A","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AEFGHIJKLMNOPONA","................","................")
                    .aisle("AFGHIJKLMNOPONMA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AFGHIJKLMNOPONMA","................","................")
                    .aisle("AGHIJKLMNOPONMLA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AGHIJKLMNOPONMLA","................","................")
                    .aisle("AHIJKLMNOPONMLKA","A..............A","A..............A","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AHIJKLMNOPONMLKA",".....AAAAA......","................")
                    .aisle("AIJKLMNOPONMLKJA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AIJKLMNOPONMLKJA",".....AAAAA......",".......?........")
                    .aisle("AJKLMNOPONMLKJIA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AJKLMNOPONMLKJIA",".....AAAAA......","......Q#R.......")
                    .aisle("AKLMNOPONMLKJIHA","A..............A","A..............A","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBAB.ABBA","AKLMNOPONMLKJIHA",".....AAAAA......","................")
                    .aisle("ALMNOPONMLKJIHGA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ALMNOPONMLKJIHGA",".....AAAAA......","................")
                    .aisle("AMNOPONMLKJIHGFA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AMNOPONMLKJIHGFA","................","................")
                    .aisle("ANOPONMLKJIHGFEA","A..............A","A..............A","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","ASOPONMLKJIHGFEA","................","................")
                    .aisle("AOPONMLKJIHGFEDA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AOPONMLKJIHGFEDA","................","................")
                    .aisle("APONMLKJIHGFEDCA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","APONMLKJIHGFEDCA","................","................")
                    .aisle("AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","................","................")
                    .where(".", Predicates.any())
                    .where("A", blocks(industral.get()))
                    .where("?",Predicates.abilities(PartAbility.IMPORT_ITEMS))
                    .where("B", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:glass"))))
                    .where("C", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:lapis_block"))))
                    .where("D", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:emerald_block"))))
                    .where("E", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:redstone_block"))))
                    .where("F", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:smooth_stone"))))
                    .where("G", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:gold_block"))))
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("I",blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:gold_block"))))
                    .where("J", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:diamond_block"))))
                    .where("H", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:diamond_block"))))
                    .where("K",blocks(industral.get()))
                    .where("L", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:waxed_copper_block"))))
                    .where("M", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("gtceu:bronze_pipe_casing"))))
                    .where("N", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:copper_block"))))
                    .where("O", blocks(steam_machine_casing.get()))
                    .where("P",blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:iron_block"))))
                    .where("Q", Predicates.abilities(PartAbility.EXPORT_ITEMS))
                    .where("R", Predicates.abilities(PartAbility.STEAM))
                    .where("S", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:copper_block"))))
                    .build())
            .modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE)
            .model(createWorkableCasingMachineModel(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/steam_oven"))
                    .andThen(b -> b.addDynamicRenderer(
                            () -> DynamicRenderHelper.makeBoilerPartRender(
                                    BoilerFireboxType.BRONZE_FIREBOX, CASING_BRONZE_BRICKS))))

            .tooltips(Component.translatable("com.gtecore.tooltips.1"),Component.translatable("com.gtecore.tooltips.0"))
            .recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .register();
    public static final MultiblockMachineDefinition Component_Factory_Multiblock = REGISTRATE
            .multiblock("component_factory", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(Component_Factory)
            .recipeModifiers(GTRecipeModifiers.OC_PERFECT_SUBTICK, BATCH_MODE)
            .appearanceBlock(CASING_ALUMINIUM_FROSTPROOF)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("AAAAAAA","AAAAAAA","BBBCBBB","BBBCBBB","BBBCBBB","BBBCBBB","BBBCBBB","BBBABBB")
                    .aisle("AAAAAAA","ADDCDDA","B.....B","B.....B","B.....B","B.....B","B.....B","BBBABBB")
                    .aisle("AAAAAAA","ADDCDDA","B.....B","B.....B","B.....B","B.....B","B.....B","BBBABBB")
                    .aisle("AAAAAAA","ACCCCCA","C..C..C","C..C..C","C..C..C","C..C..C","C..C..C","AAAAAAA")
                    .aisle("AAAAAAA","ADDCDDA","B.....B","B.....B","B.....B","B.....B","B.....B","BBBABBB")
                    .aisle("AAAAAAA","ADDCDDA","B.....B","B.....B","B.....B","B.....B","B.....B","BBBABBB")
                    .aisle("AAAAAAA","AAAEAAA","BBBCBBB","BBBCBBB","BBBCBBB","BBBCBBB","BBBCBBB","BBBABBB")
                    .where(".", Predicates.air())
                    .where('A',blocks(CASING_ALUMINIUM_FROSTPROOF.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.autoAbilities(true, false, false)))
                    .where("B", blocks(CLEANROOM_GLASS.get()))
                    .where("C", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("gtceu:aluminium_frame"))))
                    .where("D",blocks(CASING_STEEL_PIPE.get()))
                    .where("E", controller(blocks(definition.getBlock())))
                    .build()
            )
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.3"),
                    Component.translatable("com.gtecore.tooltips.0"))
            .register();
    public static final MultiblockMachineDefinition Big_Forge_Hammer = REGISTRATE
            .multiblock("big_forge_hammer", SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(GTRecipeTypes.FORGE_HAMMER_RECIPES).
            recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .pattern((definition) ->
        FactoryBlockPattern.start()
                        .aisle(new String[]{"AAAAA", "AAAAA", "B...B", "B...B", "AAAAA"})
                        .aisle(new String[]{"ACDCA", "A...A", "..E..", "..C..", "AAAAA"})
                        .aisle(new String[]{"ADCDA", "A.E.A", ".EEE.", ".CEC.", "AAAAA"})
                        .aisle(new String[]{"ACDCA", "A...A", "..E..", "..C..", "AAAAA"})
                        .aisle(new String[]{"AAAAA", "AAFAA", "B...B", "B...B", "AAAAA"})
                        .where(".", Predicates.air())
                        .where("A", Predicates.blocks(new Block[]{(Block)steam_machine_casing.get()})
                                .or(Predicates.abilities(new PartAbility[]{PartAbility.STEAM_IMPORT_ITEMS})
                                        .setPreviewCount(1)).or(Predicates.abilities(new PartAbility[]{PartAbility.STEAM_EXPORT_ITEMS})
                                        .setPreviewCount(1)).or(Predicates.abilities(new PartAbility[]{PartAbility.STEAM}).setExactLimit(1)))
                        .where("B", Predicates.blocks(new Block[]{(Block)ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:glass"))}))
                        .where("C", Predicates.blocks(new Block[]{(Block)GTBlocks.CASING_BRONZE_GEARBOX.get()})).where("D", Predicates.blocks(new Block[]{(Block)GTBlocks.CASING_BRONZE_PIPE.get()}))
                        .where("E", Predicates.blocks(new Block[]{(Block)steel_pipe_casing.get()}))
                .where("F", Predicates.controller(Predicates.blocks(new Block[]{definition.getBlock()})))
                .build())
            .register();

    public static final MultiblockMachineDefinition Big_Steam_Extractor = GTRegistration.REGISTRATE.multiblock("big_steam_extractor", (x$0) -> {
        return new SteamParallelMultiblockMachine(x$0, new Object[0]);
    }).rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .recipeType(GTRecipeTypes.EXTRACTOR_RECIPES)
            .recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .pattern((definition) -> {
        return FactoryBlockPattern.start()
                .aisle(new String[]{"........", "........", "........", "..AAAA..", ".AAAAAA.", ".BBBBBB.", ".BBBBBB."})
                .aisle(new String[]{"..BBBB..", "...AA...", "..AAAA..", ".A....A.", "A......A", "B......B", "B......B"})
                .aisle(new String[]{".BDEEDB.", "........", ".A....A.", "A......A", "A......A", "B......B", "B......B"})
                .aisle(new String[]{".BDEEDB.", ".A....A.", ".A....A.", "A......A", "A......A", "B......B", "B......B"})
                .aisle(new String[]{".BDEEDB.", ".A....A.", ".A....A.", "A......A", "A......A", "B......B", "B......B"})
                .aisle(new String[]{".BDEEDB.", "........", ".A....A.", "A......A", "A......A", "B......B", "B......B"})
                .aisle(new String[]{"..BCBB..", "...AA...", ".AAAAA..", "A.....A.", "A......A", "B......B", "B......B"})
                .aisle(new String[]{"........", "........", "........", ".AAAAA..", ".AAAAAA.", ".BBBBBB.", ".BBBBBB."})
                .where(".", Predicates.air()).where("A", easy("minecraft:glass"))
                .where("B", Predicates.blocks(new Block[]{(Block)steam_machine_casing.get()})
                        .or(Predicates.abilities(new PartAbility[]{PartAbility.STEAM_IMPORT_ITEMS})
                                .setPreviewCount(1)).or(Predicates.abilities(new PartAbility[]{PartAbility.STEAM_EXPORT_ITEMS}).setPreviewCount(1)).or(Predicates.abilities(new PartAbility[]{PartAbility.STEAM}).setExactLimit(1))).where("C", Predicates.controller(Predicates.blocks(new Block[]{definition.getBlock()}))).where("D", Predicates.blocks(new Block[]{(Block)GTBlocks.CASING_BRONZE_GEARBOX.get()})).where("E", Predicates.blocks(new Block[]{(Block)GTBlocks.CASING_BRONZE_PIPE.get()})).build();
    }).tooltips(new Component[]{Component.translatable("com.gtecore.tooltips.0")})
            .register();
    static {
        //GTECoreRegistration.GTECore_REGISTRATE.creativeModeTab(() -> MORE_MACHINES);
    }
}
