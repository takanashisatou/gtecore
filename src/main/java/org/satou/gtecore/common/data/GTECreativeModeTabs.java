package org.satou.gtecore.common.data;

import org.satou.gtecore.GTECore;

import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;

import net.minecraft.world.item.CreativeModeTab;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.items.GTEItems.SUPER_STRING_PROCESSOR_ZPM;
import static org.satou.gtecore.common.data.machines.GTEMultiMachine.Easy_Box;
import static org.satou.gtecore.common.data.machines.GTEMultiMachine.STEAM_OP;

public class GTECreativeModeTabs {

    public static final RegistryEntry<CreativeModeTab> MORE_MACHINES = GTECore_REGISTRATE
            .defaultCreativeTab("gtecore_machines", builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("gtecore_machines", GTECore_REGISTRATE))
                    .title(GTECore_REGISTRATE.addLang("itemGroup", GTECore.id("gtecore_machines"), GTECore.NAME))
                    .icon(STEAM_OP::asStack)
                    .build())
            .register();
    public static final RegistryEntry<CreativeModeTab> MORE_ITEMS = GTECore_REGISTRATE
            .defaultCreativeTab("gtecore_items", builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("gtecore_items", GTECore_REGISTRATE))
                    .title(GTECore_REGISTRATE.addLang("itemGroup", GTECore.id("gtecore_items"), GTECore.NAME))
                    .icon(SUPER_STRING_PROCESSOR_ZPM::asStack)
                    .build())
            .register();

    public static void init() {

    }
}
