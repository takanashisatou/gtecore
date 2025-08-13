package org.satou.gtecore.utils;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraft.data.recipes.FinishedRecipe;
import org.satou.gtecore.GTERecipe;
import org.satou.gtecore.Gtecore;
import org.satou.gtecore.common.registry.GTECoreRegistration;

import java.util.function.Consumer;

@GTAddon
public class GTEGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return GTECoreRegistration.GTECore_REGISTRATE;
    }

    @Override
    public void initializeAddon() {}

    @Override
    public String addonModId() {
        return Gtecore.MOD_ID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        GTERecipe.init(provider);
    }
}