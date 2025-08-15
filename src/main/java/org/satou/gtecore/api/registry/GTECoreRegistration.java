package org.satou.gtecore.api.registry;

import org.satou.gtecore.GTECore;

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public class GTECoreRegistration {

    public static GTRegistrate GTECore_REGISTRATE = GTRegistrate.create(GTECore.MOD_ID);

    static {
        GTECoreRegistration.GTECore_REGISTRATE.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    }

    private GTECoreRegistration() {/**/}
}
