package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.satou.gtecore.Gtecore;
import org.spongepowered.asm.mixin.Mixin;
import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.GTBlocks.createCasingBlock;
import static com.gregtechceu.gtceu.common.data.GTRecipeModifiers.BATCH_MODE;
import static com.gregtechceu.gtceu.common.data.GTRecipeModifiers.OC_PERFECT;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

@Mixin(GTMultiMachines.class)
public class gtemultimachinemixin {

    //private static ResourceLocation a = GTCEu.id()
    private static BlockEntry<Block> steam_machine_casing = createCasingBlock("steam_machine_casing",GTCEu.id("block/casings/steam/bronze"));
    private static ResourceLocation id = new ResourceLocation("gtceu", "steam_machine_casing");
    private static Block a = BuiltInRegistries.BLOCK.get(id);
    private static final MultiblockMachineDefinition big_steam_alloy_smelter = REGISTRATE
            .multiblock("big_alloy", SteamParallelMultiblockMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .recipeTypes(GTRecipeTypes.ALLOY_SMELTER_RECIPES)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBB","BBB","BBB")
                    .aisle("BBB","BAB","BBB")
                    .aisle("BBB","B#B","BBB")
                    .where('A', Predicates.air())
                    .where('#',controller(blocks(definition.getBlock())))
                    .where('B',blocks(steam_machine_casing.get()).setMinGlobalLimited(6)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                        )
                    .build())
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/steam_oven"))
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
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
                    .where('A', Predicates.air())
                    .where('#',controller(blocks(definition.getBlock())))
                    .where('B',blocks(steam_machine_casing.get()).setMinGlobalLimited(6)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                    )
                    .build())
            .tooltips(Component.translatable("com.gtecore.tooltips.0"))
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/steam_oven"))
            .recipeModifier(SteamParallelMultiblockMachine::recipeModifier, true)
            .register();
}
