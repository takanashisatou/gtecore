package org.satou.gtecore;

import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.satou.gtecore.common.registry.GTECoreRegistration;

import java.util.function.Supplier;

import static com.gregtechceu.gtceu.common.data.GTMachines.CREATIVE_ENERGY;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Diamond;
import static org.satou.gtecore.GTEMultiMachine.Easy_Box;
import static org.satou.gtecore.GTERecipeTypes.EASY_BOX;
import static org.satou.gtecore.common.registry.GTECoreRegistration.GTECore_REGISTRATE;

public class CreativeModeTabs {
    public static final RegistryEntry<CreativeModeTab> MORE_MACHINES = GTECore_REGISTRATE
            .defaultCreativeTab("more_machines", builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("more_machines", GTECore_REGISTRATE))
                    .title(GTECore_REGISTRATE.addLang("itemGroup", Gtecore.id("gtecore_machine"), Gtecore.NAME))
                    .icon(Easy_Box::asStack)
                    .build())
            .register();
}
