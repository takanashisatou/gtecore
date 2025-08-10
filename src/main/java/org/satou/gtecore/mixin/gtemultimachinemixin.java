package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderHelper;
import com.gregtechceu.gtceu.common.block.BoilerFireboxType;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.satou.gtecore.GTERecipeTypes;
import org.satou.gtecore.Gtecore;
import org.spongepowered.asm.mixin.Mixin;

import java.lang.reflect.Field;

import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_BRONZE_BRICKS;
import static com.gregtechceu.gtceu.common.data.GTBlocks.createCasingBlock;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

@Mixin(GTMultiMachines.class)
public class gtemultimachinemixin {
    private static BlockEntry<Block> iron = createCasingBlock("iron_block",new ResourceLocation("minecraft","iron_block"));
    private static BlockEntry<Block> copper_block = createCasingBlock("copper_block",new ResourceLocation("minecraft","copper_block"));
    private static BlockEntry<Block> bronze_pipe_casing = createCasingBlock("bronze_pipe_casing",new ResourceLocation("gtceu","bronze_pipe_casing"));
    private static BlockEntry<Block> steel = createCasingBlock("steel_block",new ResourceLocation("gtceu","steel_block"));
    private static BlockEntry<Block> bronze = createCasingBlock("bronze_block",new ResourceLocation("gtceu","bronze_block"));
    private static BlockEntry<Block> bra = createCasingBlock("brass_block",new ResourceLocation("gtceu","brass_block"));
    private static BlockEntry<Block> smo = createCasingBlock("smooth_stone",new ResourceLocation("minecraft","smooth_stone"));
    private  static BlockEntry<Block> red = createCasingBlock("redstone_block",new ResourceLocation("minecraft","redstone_block"));
    private static BlockEntry<Block> glass = createCasingBlock("glass",new ResourceLocation("minecraft","glass"));
    //private static ResourceLocation a = GTCEu.id()
    private  static BlockEntry<Block> eme = createCasingBlock("emerald_block",new ResourceLocation("minecraft","emerald_block"));
    private static BlockEntry<Block> lapis = createCasingBlock("lapis_block",new ResourceLocation("minecraft","lapis_block"));
    private static BlockEntry<Block> industral = createCasingBlock("industrial_steam_casing",new ResourceLocation("gtceu","industrial_steam_casing"));
    private static BlockEntry<Block> steam_machine_casing = createCasingBlock("steam_machine_casing",GTCEu.id("block/casings/steam/bronze"));
    private static ResourceLocation id = new ResourceLocation("gtceu", "steam_machine_casing");
    private static Block a = BuiltInRegistries.BLOCK.get(id);
    private static BlockEntry<Block> diamond = createCasingBlock("diamond_block",new ResourceLocation("minecraft","diamond_block"));
    private static BlockEntry<Block> gold = createCasingBlock("gold_block",new ResourceLocation("minecraft","gold_block"));
    private static final MultiblockMachineDefinition big_steam_alloy_smelter = REGISTRATE
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

    private static final MultiblockMachineDefinition big_steam_compressor = REGISTRATE
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
    private static final MultiblockMachineDefinition Easy_Box = REGISTRATE
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
                    .aisle("AIJKLMNOPONMLKJA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AIJKLMNOPONMLKJA",".....AAAAA......","................")
                    .aisle("AJKLMNOPONMLKJIA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AJKLMNOPONMLKJIA",".....AAAAA......","......Q#R.......")
                    .aisle("AKLMNOPONMLKJIHA","A..............A","A..............A","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBAB.ABBA","AKLMNOPONMLKJIHA",".....AAAAA......","................")
                    .aisle("ALMNOPONMLKJIHGA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ALMNOPONMLKJIHGA",".....AAAAA......","................")
                    .aisle("AMNOPONMLKJIHGFA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AMNOPONMLKJIHGFA","................","................")
                    .aisle("ANOPONMLKJIHGFEA","A..............A","A..............A","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","ASOPONMLKJIHGFEA","................","................")
                    .aisle("AOPONMLKJIHGFEDA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","AOPONMLKJIHGFEDA","................","................")
                    .aisle("APONMLKJIHGFEDCA","B..............B","B..............B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","ABBABBABBABBABBA","B..B..B..B..B..B","B..B..B..B..B..B","APONMLKJIHGFEDCA","................","................")
                    .aisle("AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","ABBABBABBABBABBA","ABBABBABBABBABBA","AAAAAAAAAAAAAAAA","................","................")
                    .where(".", Predicates.air())
                    .where("A", blocks(industral.get()))
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
                    .where("L", blocks(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:copper_block"))))
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



}
