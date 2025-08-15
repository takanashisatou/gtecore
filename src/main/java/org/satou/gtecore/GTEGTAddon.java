package org.satou.gtecore;

import org.satou.gtecore.api.registry.GTECoreRegistration;
import org.satou.gtecore.data.recipe.GTERecipe;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

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
        return GTECore.MOD_ID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        GTERecipe.init(provider);
    }
}
