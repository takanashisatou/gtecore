package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraftforge.event.level.PistonEvent;
import org.satou.gtecore.common.data.GTERecipeTypes;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderHelper;
import com.gregtechceu.gtceu.common.block.BoilerFireboxType;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;
import com.gregtechceu.gtceu.common.registry.GTRegistration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.util.entry.BlockEntry;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.frameGt;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.gear;
import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.GTBlocks.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeModifiers.BATCH_MODE;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;
import static org.satou.gtecore.common.data.GTERecipeTypes.Component_Factory;

public class GTEMultiMachine {

    public static void init() {}

    public static BlockEntry<Block> steel_pipe_casing = createCasingBlock("steel_pipe_casing", new ResourceLocation("gtceu", "steel_pipe_casing"));
    // public static BlockEntry<Block> aluminium_frame = createCasingBlock("aluminium_frame",new
    // ResourceLocation("gtceu","aluminium_frame"));
    public static BlockEntry<Block> industral = createCasingBlock("industrial_steam_casing", new ResourceLocation("gtceu", "industrial_steam_casing"));
    public static BlockEntry<Block> steam_machine_casing = createCasingBlock("steam_machine_casing", GTCEu.id("block/casings/steam/bronze"));
    public static ResourceLocation id = new ResourceLocation("gtceu", "steam_machine_casing");
    public static final MultiblockMachineDefinition big_steam_alloy_smelter = REGISTRATE
            .multiblock("big_alloy", SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .recipeTypes(GTRecipeTypes.ALLOY_SMELTER_RECIPES)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBB", "BBB", "BBB")
                    .aisle("BBB", "BAB", "BBB")
                    .aisle("BBB", "B#B", "BBB")
                    .where("A", Predicates.air())
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("B", blocks(steam_machine_casing.get()).setMinGlobalLimited(6)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS)
                                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))))
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
                    .aisle("BBB", "BBB", "BBB")
                    .aisle("BBB", "BAB", "BBB")
                    .aisle("BBB", "B#B", "BBB")
                    .where("A", Predicates.air())
                    .where("#", controller(blocks(definition.getBlock())))
                    .where("B", blocks(steam_machine_casing.get()).setMinGlobalLimited(6)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS)
                                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))))
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
            .multiblock("easy_box", SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .recipeTypes(GTERecipeTypes.EASY_BOX)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "................", "................")
                    .aisle("ACDEFGHIJKLMNOPA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ACDEFGHIJKLMNOPA", "................", "................")
                    .aisle("ADEFGHIJKLMNOPOA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ADEFGHIJKLMNOPOA", "................", "................")
                    .aisle("AEFGHIJKLMNOPONA", "A..............A", "A..............A", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AEFGHIJKLMNOPONA", "................", "................")
                    .aisle("AFGHIJKLMNOPONMA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "AFGHIJKLMNOPONMA", "................", "................")
                    .aisle("AGHIJKLMNOPONMLA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "AGHIJKLMNOPONMLA", "................", "................")
                    .aisle("AHIJKLMNOPONMLKA", "A..............A", "A..............A", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AHIJKLMNOPONMLKA", ".....AAAAA......", "................")
                    .aisle("AIJKLMNOPONMLKJA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "AIJKLMNOPONMLKJA", ".....AAAAA......", ".......?........")
                    .aisle("AJKLMNOPONMLKJIA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "AJKLMNOPONMLKJIA", ".....AAAAA......", "......Q#R.......")
                    .aisle("AKLMNOPONMLKJIHA", "A..............A", "A..............A", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBAB.ABBA", "AKLMNOPONMLKJIHA", ".....AAAAA......", "................")
                    .aisle("ALMNOPONMLKJIHGA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ALMNOPONMLKJIHGA", ".....AAAAA......", "................")
                    .aisle("AMNOPONMLKJIHGFA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "AMNOPONMLKJIHGFA", "................", "................")
                    .aisle("ANOPONMLKJIHGFEA", "A..............A", "A..............A", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "ASOPONMLKJIHGFEA", "................", "................")
                    .aisle("AOPONMLKJIHGFEDA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "AOPONMLKJIHGFEDA", "................", "................")
                    .aisle("APONMLKJIHGFEDCA", "B..............B", "B..............B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "ABBABBABBABBABBA", "B..B..B..B..B..B", "B..B..B..B..B..B", "APONMLKJIHGFEDCA", "................", "................")
                    .aisle("AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "ABBABBABBABBABBA", "ABBABBABBABBABBA", "AAAAAAAAAAAAAAAA", "................", "................")
                    .where(".", Predicates.any())
                    .where("A", Predicates.blocks(industral.get()))
                    .where("?", Predicates.abilities(PartAbility.IMPORT_ITEMS))
                    .where("B", Predicates.blocks(Blocks.GLASS))
                    .where("C", Predicates.blocks(Blocks.LAPIS_BLOCK))
                    .where("D", Predicates.blocks(Blocks.EMERALD_BLOCK))
                    .where("E", Predicates.blocks(Blocks.REDSTONE_BLOCK))
                    .where("F", Predicates.blocks(Blocks.SMOOTH_STONE))
                    .where("G", Predicates.blocks(Blocks.GOLD_BLOCK))
                    .where("#", Predicates.controller(blocks(definition.getBlock())))
                    .where("I", Predicates.blocks(Blocks.GOLD_BLOCK))
                    .where("J", Predicates.blocks(Blocks.DIAMOND_BLOCK))
                    .where("H", Predicates.blocks(Blocks.DIAMOND_BLOCK))
                    .where("K", Predicates.blocks(industral.get()))
                    .where("L", Predicates.blocks(Blocks.WAXED_COPPER_BLOCK))
                    .where("M", Predicates.blocks(CASING_BRONZE_PIPE.get()))
                    .where("N", Predicates.blocks(Blocks.WAXED_COPPER_BLOCK))
                    .where("O", Predicates.blocks(steam_machine_casing.get()))
                    .where("P", Predicates.blocks(Blocks.IRON_BLOCK))
                    .where("Q", Predicates.abilities(PartAbility.EXPORT_ITEMS))
                    .where("R", Predicates.abilities(PartAbility.STEAM))
                    .where("S", Predicates.blocks(Blocks.COPPER_BLOCK))
                    .build())
            .modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE)
            .model(createWorkableCasingMachineModel(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/steam_oven"))
                    .andThen(b -> b.addDynamicRenderer(
                            () -> DynamicRenderHelper.makeBoilerPartRender(
                                    BoilerFireboxType.BRONZE_FIREBOX, CASING_BRONZE_BRICKS))))
            .tooltips(Component.translatable("com.gtecore.tooltips.1"), Component.translatable("com.gtecore.tooltips.0"))
            .recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .register();
    public static final MultiblockMachineDefinition Component_Factory_Multiblock = REGISTRATE
            .multiblock("component_factory", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(Component_Factory)
            .recipeModifiers(GTRecipeModifiers.OC_PERFECT_SUBTICK, BATCH_MODE)
            .appearanceBlock(CASING_ALUMINIUM_FROSTPROOF)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("AAAAAAA", "AAAAAAA", "BBBCBBB", "BBBCBBB", "BBBCBBB", "BBBCBBB", "BBBCBBB", "BBBABBB")
                    .aisle("AAAAAAA", "ADDCDDA", "B.....B", "B.....B", "B.....B", "B.....B", "B.....B", "BBBABBB")
                    .aisle("AAAAAAA", "ADDCDDA", "B.....B", "B.....B", "B.....B", "B.....B", "B.....B", "BBBABBB")
                    .aisle("AAAAAAA", "ACCCCCA", "C..C..C", "C..C..C", "C..C..C", "C..C..C", "C..C..C", "AAAAAAA")
                    .aisle("AAAAAAA", "ADDCDDA", "B.....B", "B.....B", "B.....B", "B.....B", "B.....B", "BBBABBB")
                    .aisle("AAAAAAA", "ADDCDDA", "B.....B", "B.....B", "B.....B", "B.....B", "B.....B", "BBBABBB")
                    .aisle("AAAAAAA", "AAAEAAA", "BBBCBBB", "BBBCBBB", "BBBCBBB", "BBBCBBB", "BBBCBBB", "BBBABBB")
                    .where(".", Predicates.air())
                    .where("A", Predicates.blocks(CASING_ALUMINIUM_FROSTPROOF.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.autoAbilities(true, false, false)))
                    .where("B", Predicates.blocks(CLEANROOM_GLASS.get()))
                    .where("C", Predicates.blocks(ChemicalHelper.getBlock(frameGt,Aluminium)))
                    .where("D", Predicates.blocks(CASING_STEEL_PIPE.get()))
                    .where("E", Predicates.controller(blocks(definition.getBlock())))
                    .build())
            .tooltips(
                    Component.translatable("com.gtecore.tooltips.3"),
                    Component.translatable("com.gtecore.tooltips.0"))
            .register();
    public static final MultiblockMachineDefinition Big_Forge_Hammer = REGISTRATE.multiblock("big_forge_hammer", SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(GTRecipeTypes.FORGE_HAMMER_RECIPES).recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .pattern((definition) -> FactoryBlockPattern.start()
                    .aisle("AAAAA", "AAAAA", "B...B", "B...B", "AAAAA")
                    .aisle("ACDCA", "A...A", "..E..", "..C..", "AAAAA")
                    .aisle("ADCDA", "A.E.A", ".EEE.", ".CEC.", "AAAAA")
                    .aisle("ACDCA", "A...A", "..E..", "..C..", "AAAAA")
                    .aisle("AAAAA", "AAFAA", "B...B", "B...B", "AAAAA")
                    .where(".", Predicates.air())
                    .where("A", Predicates.blocks(steam_machine_casing.get())
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS)))
                    .where("B", Predicates.blocks(Blocks.GLASS))
                    .where("C", Predicates.blocks(GTBlocks.CASING_BRONZE_GEARBOX.get())).where("D", Predicates.blocks(GTBlocks.CASING_BRONZE_PIPE.get()))
                    .where("E", Predicates.blocks(steel_pipe_casing.get()))
                    .where("F", Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
            .register();

    public static final MultiblockMachineDefinition Big_Steam_Extractor = GTRegistration.REGISTRATE.multiblock("big_steam_extractor", SteamParallelMultiblockMachine::new).rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .recipeType(GTRecipeTypes.EXTRACTOR_RECIPES)
            .recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .pattern((definition) -> FactoryBlockPattern.start()
                    .aisle("........", "........", "........", "..AAAA..", ".AAAAAA.", ".BBBBBB.", ".BBBBBB.")
                    .aisle("..BBBB..", "...AA...", "..AAAA..", ".A....A.", "A......A", "B......B", "B......B")
                    .aisle(".BDEEDB.", "........", ".A....A.", "A......A", "A......A", "B......B", "B......B")
                    .aisle(".BDEEDB.", ".A....A.", ".A....A.", "A......A", "A......A", "B......B", "B......B")
                    .aisle(".BDEEDB.", ".A....A.", ".A....A.", "A......A", "A......A", "B......B", "B......B")
                    .aisle(".BDEEDB.", "........", ".A....A.", "A......A", "A......A", "B......B", "B......B")
                    .aisle("..BCBB..", "...AA...", ".AAAAA..", "A.....A.", "A......A", "B......B", "B......B")
                    .aisle("........", "........", "........", ".AAAAA..", ".AAAAAA.", ".BBBBBB.", ".BBBBBB.")
                    .where(".", Predicates.air())
                    .where("A", Predicates.blocks(Blocks.GLASS))
                    .where("B", Predicates.blocks(steam_machine_casing.get())
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS)))
                    .where("C", Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .where("D", Predicates.blocks(GTBlocks.CASING_BRONZE_GEARBOX.get())).where("E", Predicates.blocks(GTBlocks.CASING_BRONZE_PIPE.get())).build())
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
            .register();
    public static final MultiblockMachineDefinition Big_Bender = REGISTRATE
            .multiblock("big_bender", WorkableElectricMultiblockMachine::new)
            .appearanceBlock(CASING_STEEL_SOLID)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(GTRecipeTypes.BENDER_RECIPES)
            .recipeModifiers(GTRecipeModifiers.OC_PERFECT_SUBTICK, BATCH_MODE)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBB", "BBB", "BBB")
                    .aisle("BBB", "BAB", "BBB")
                    .aisle("BBB", "B#B", "BBB")
                    .where("B", blocks(CASING_STEEL_SOLID.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where("A", Predicates.air())
                    .where("#", controller(blocks(definition.getBlock())))
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
            .register();
    public static final MultiblockMachineDefinition Big_Wiremill = REGISTRATE
            .multiblock("big_wiremill", WorkableElectricMultiblockMachine::new)
            .appearanceBlock(CASING_STEEL_SOLID)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(GTRecipeTypes.WIREMILL_RECIPES)
            .recipeModifiers(GTRecipeModifiers.OC_PERFECT_SUBTICK, BATCH_MODE)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBB", "BBB", "BBB")
                    .aisle("BBB", "BAB", "BBB")
                    .aisle("BBB", "B#B", "BBB")
                    .where("B", blocks(CASING_STEEL_SOLID.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where("A", Predicates.air())
                    .where("#", controller(blocks(definition.getBlock())))
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
            .register();
    public static MultiblockMachineDefinition Circuit_Factory = REGISTRATE
            .multiblock("circuit_factory",WorkableElectricMultiblockMachine::new)
            .appearanceBlock(CASING_STEEL_SOLID)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeTypes(GTRecipeTypes.ASSEMBLER_RECIPES,GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES,GTRecipeTypes.LASER_ENGRAVER_RECIPES,GTRecipeTypes.CUTTER_RECIPES)
            .recipeModifiers(GTRecipeModifiers.OC_PERFECT_SUBTICK,BATCH_MODE)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("AAAAAAAAAAA","BCCCCCCCCCB","ACCCCCCCCCA","ACCCCCCCCCA","ACCCCCCCCCA","ACCCCCCCCCA","ACCCCCCCCCA","AAAAAAAAAAA")
                    .aisle("AAAAAAAAAAA","CB.......BC","CB.......BC","C.........C","C.........C","C.........C","C.........C","AAAAAAAAAAA")
                    .aisle("AAAAAAAAAAA","C.B.....B.C","C.B.....B.C","C.B.....B.C","C.........C","C.........C","C.........C","AAAAAAAAAAA")
                    .aisle("AAAAAAAAAAA","C..B...B..C","C..B...B..C","C..B...B..C","C..B...B..C","C.........C","C.........C","AAACCCCCAAA")
                    .aisle("AAAAAAAAAAA","C...BDB...C","C...BAB...C","C...BDB...C","C...BAB...C","C...BDB...C","C.........C","AAACCCCCAAA")
                    .aisle("AAAAAAAAAAA","C...DBD...C","C...ABE...C","C...DBD...C","C...ABE...C","C...DBD...C","C....B....C","AAACCCCCAAA")
                    .aisle("AAAAAAAAAAA","C...BDB...C","C...BAB...C","C...BDB...C","C...BAB...C","C...BDB...C","C.........C","AAACCCCCAAA")
                    .aisle("AAAAAAAAAAA","C..B...B..C","C..B...B..C","C..B...B..C","C..B...B..C","C.........C","C.........C","AAACCCCCAAA")
                    .aisle("AAAAAAAAAAA","C.B.....B.C","C.B.....B.C","C.B.....B.C","C.........C","C.........C","C.........C","AAAAAAAAAAA")
                    .aisle("AAAAAAAAAAA","CB.......BC","CB.......BC","C.........C","C.........C","C.........C","C.........C","AAAAAAAAAAA")
                    .aisle("AAAAAFAAAAA","BCCCCCCCCCB","ACCCCCCCCCA","ACCCCCCCCCA","ACCCCCCCCCA","ACCCCCCCCCA","ACCCCCCCCCA","AAAAAAAAAAA")
                    .where(".", Predicates.air())
                    .where("A", blocks(CASING_STAINLESS_CLEAN.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                    )
                    .where("B", Predicates.blocks(ChemicalHelper.getBlock(frameGt,StainlessSteel)))
                    .where("C", blocks(CLEANROOM_GLASS.get()))
                    .where("D", blocks(CASING_STAINLESS_STEEL_GEARBOX.get()))
                    .where("E", blocks(CASING_STAINLESS_TURBINE.get()))
                    .where("F", controller(blocks(definition.getBlock())))

                    .build())
            .workableCasingModel(GTCEu.id("block/casings/gcym/watertight_casing"),
                    GTCEu.id("block/multiblock/gcym/large_distillery"))
            .register();
}
