package org.satou.gtecore.common;

import org.satou.gtecore.common.data.GTERecipeTypes;
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
}
