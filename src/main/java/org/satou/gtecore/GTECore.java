package org.satou.gtecore;

import org.satou.gtecore.client.ClientProxy;
import org.satou.gtecore.common.CommonProxy;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraft.resources.ResourceLocation.tryBuild;

@Mod(GTECore.MOD_ID)
public class GTECore {

    public static final String NAME = "GTE Core";
    public static final String MOD_ID = "gtecore";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static ResourceLocation id(String name) {
        return tryBuild(MOD_ID, name);
    }

    public GTECore() {
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }
}
