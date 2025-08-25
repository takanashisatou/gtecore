package org.satou.gtecore.config;

import org.satou.gtecore.GTECore;

import com.gregtechceu.gtceu.config.ConfigHolder;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = GTECore.MOD_ID)
public class GTEConfig {

    public static GTEConfig INSTANCE;
    private static final Object lock = new Object();

    public static void init() {
        synchronized (lock) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(GTEConfig.class, ConfigFormats.YAML).getConfigInstance();
            }
            ConfigHolder.init();

            ConfigHolder.INSTANCE.machines.steamMultiParallelAmount = 1024;
        }
    }

    @Configurable
    @Configurable.Range(min = 0)
    public double durationMultiplier = 1;
}
