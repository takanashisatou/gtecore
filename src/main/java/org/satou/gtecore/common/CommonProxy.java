package org.satou.gtecore.common;

import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.common.machine.multiblock.part.SteamHatchPartMachine;
import com.hepdd.gtmthings.data.CreativeModeTabs;
import com.hepdd.gtmthings.data.CustomItems;
import com.hepdd.gtmthings.data.GTMTCovers;
import net.minecraftforge.eventbus.api.GenericEvent;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.GTEGTAddon;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTECreativeModeTabs;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEMultiMachine;
import org.satou.gtecore.config.GTEConfig;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;

public class CommonProxy {

    public CommonProxy() {
        init();
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        GTECore_REGISTRATE.registerEventListeners(eventBus);
        eventBus.addListener(CommonProxy::commonSetup);
        eventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        eventBus.addGenericListener(MachineDefinition.class, this::registerMachines);
        eventBus.addGenericListener(CoverDefinition.class, this::registerCovers);
        //Register the GTRecipeTypes event to ensure that GTERecipeTypes is initialized at the right time
    }


    private static void init() {
        GTEConfig.init();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {}

    private void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
        GTERecipeTypes.init();
    }

    private void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        GTEMultiMachine.init();
    }
    private void registerCovers(GTCEuAPI.RegisterEvent<ResourceLocation, CoverDefinition> event) {
        GTECreativeModeTabs.init();
        GTEItems.init();
        GTEBlocks.init();
    }
}
