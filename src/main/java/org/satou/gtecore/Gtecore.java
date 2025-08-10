package org.satou.gtecore;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.GTRegistry;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.*;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.kjs.GTCEuStartupEvents;
import com.gregtechceu.gtceu.integration.kjs.events.GTRegistryEventJS;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;

import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.GCYMBlocks.CASING_SECURE_MACERATION;
import static com.gregtechceu.gtceu.common.data.GTRecipeModifiers.*;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;
import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;
import static com.mojang.text2speech.Narrator.LOGGER;
import static net.minecraft.resources.ResourceLocation.tryBuild;

//import com.gregtechceu.*;
// The value here should match an entry in the META-INF/mods.toml file
@Mod(Gtecore.MODID)

//@Mod.EventBusSubscriber(modid = "gtecore", bus = Mod.EventBusSubscriber.Bus.MOD)
public class Gtecore {
    public static String MOD_ID = "gtecore";
    //public static final String MODID = "gtecore";
    public static ResourceLocation id(String name) {
        return tryBuild(MOD_ID, name);
    }

    // 建议缓存 GTRegistrate 实例
    public static GTRecipeType register(String name, String group, RecipeType<?>... proxyRecipes) {
        var recipeType = new GTRecipeType(Gtecore.id(name), group, proxyRecipes);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, recipeType.registryName, recipeType);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, recipeType.registryName, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(recipeType.registryName, recipeType);
        return recipeType;
    }
    // Define mod id in a common place for everything to reference
    public static final String MODID = "gtecore";
    // Directly reEXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "gteference a slf4j logger
       public static final Logger LOP = LogUtils.getLogger();
    //    // Create a Deferred Register to hold Blocks which will all be registered under the "gtecore" namespace
    //    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    //    // Create a Deferred Register to hold Items which will all be registered under the "gtecore" namespace
    //    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    //    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "gtecore" namespace
    //    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    //    public static final GTRegistrate REGISTRATE = GTRegistrate.create(MODID);
    //    // Creates a new Block with the id "gtecore:example_block", combining the namespace and path
    //    public static final RegistryObject<Block> core:example_block", combining the namespace and path


    public Gtecore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        // Register the commonSetup method for modloading
        ConfigHolder.init();
        ConfigHolder.INSTANCE.machines.steamMultiParallelAmount = 1024;
        // Register the Deferred Register to the mod event bus so blocks get registered

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);


    }

    private void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
            GTERecipeTypes.init();
    }


    // Add the example block item to the building blocks tab

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code

            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
