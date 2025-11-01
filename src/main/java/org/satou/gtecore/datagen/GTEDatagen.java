package org.satou.gtecore.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.satou.gtecore.GTECore;

@Mod.EventBusSubscriber(modid = GTECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GTEDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        // ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        generator.addProvider(event.includeClient(), new CommonLanguageProvider(generator, "en_us"));
        generator.addProvider(event.includeClient(), new CommonLanguageProvider(generator, "zh_cn"));
    }
}
