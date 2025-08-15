package org.satou.gtecore.common.data;

import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;

import net.minecraft.world.item.CreativeModeTab;

import com.tterrag.registrate.util.entry.RegistryEntry;
import org.satou.gtecore.GTECore;

import static org.satou.gtecore.common.data.machines.GTEMultiMachine.Easy_Box;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;

public class GTECreativeModeTabs {

    public static final RegistryEntry<CreativeModeTab> MORE_MACHINES = GTECore_REGISTRATE
            .defaultCreativeTab("more_machines", builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("more_machines", GTECore_REGISTRATE))
                    .title(GTECore_REGISTRATE.addLang("itemGroup", GTECore.id("gtecore_machine"), GTECore.NAME))
                    .icon(Easy_Box::asStack)
                    .build())
            .register();
}
