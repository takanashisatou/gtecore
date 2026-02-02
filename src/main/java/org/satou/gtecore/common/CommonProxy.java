package org.satou.gtecore.common;

import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipes;
import com.gregtechceu.gtceu.data.pack.GTDynamicDataPack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.GenericEvent;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTECreativeModeTabs;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.data.condition.GTERecipeConditions;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEMachines;
import org.satou.gtecore.config.GTEConfig;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.satou.gtecore.data.recipe.GTERecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTRecipes.RECIPE_FILTERS;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;

public class CommonProxy {

    public CommonProxy() {
        init();
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        GTECore_REGISTRATE.registerEventListeners(eventBus);
        eventBus.addListener(CommonProxy::commonSetup);
        eventBus.addListener(this::registerMaterials);
        eventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        eventBus.addGenericListener(GTRecipes.class, this::registerRecipes);
        eventBus.addGenericListener(MachineDefinition.class, this::registerMachines);
        eventBus.addGenericListener(CoverDefinition.class, this::registerCovers);
        eventBus.addGenericListener(RecipeConditionType.class,this::registerRecipeConditions);
        //Register the GTRecipeTypes event to ensure that GTERecipeTypes is initialized at the right time
    }



    private void registerRecipes(GTCEuAPI.RegisterEvent<ResourceLocation,GTRecipes> event) {
        Consumer<FinishedRecipe> consumer = GTDynamicDataPack::addRecipe;
        Consumer<FinishedRecipe> consumer2 = recipe -> {
            if (!RECIPE_FILTERS.contains(recipe.getId())) {
                consumer.accept(recipe);
            }
        };
        GTERecipe.init(consumer2);
    }


    private static void init() {
        GTEConfig.init();
        //Meow
    }

    private static void commonSetup(FMLCommonSetupEvent event) {}

    private void registerRecipeConditions(GTCEuAPI.RegisterEvent<ResourceLocation,RecipeConditionType> event) {
        GTERecipeConditions.init();
    }
    private void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
        GTERecipeTypes.init();

    }

    private void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        GTEMachines.init();
    }
    private void registerCovers(GTCEuAPI.RegisterEvent<ResourceLocation, CoverDefinition> event) {
        GTECreativeModeTabs.init();
        GTEItems.init();
        GTEBlocks.init();
    }
    private void registerMaterials(MaterialEvent materialEvent){
        GTEMaterials.init();
    }
}
