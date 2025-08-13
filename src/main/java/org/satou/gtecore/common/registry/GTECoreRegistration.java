package org.satou.gtecore.common.registry;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.satou.gtecore.Gtecore;

public class GTECoreRegistration {
    public static GTRegistrate GTECore_REGISTRATE = GTRegistrate.create(Gtecore.MOD_ID);

    static {
        GTECoreRegistration.GTECore_REGISTRATE.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    }

    private GTECoreRegistration() {/**/}
}
