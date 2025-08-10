package org.satou.gtecore;

import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;

public class RegistryUtils {

    public static void unfreezeOtherModRegistry(String modid, String registryName) {
        try {
            ResourceLocation rl = new ResourceLocation(modid, registryName);
            IForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(rl);
            if (registry == null) {
                System.out.println("找不到注册表：" + rl);
                return;
            }

            Field frozenField = registry.getClass().getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.setBoolean(registry, false);

            System.out.println("成功解冻注册表：" + rl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
