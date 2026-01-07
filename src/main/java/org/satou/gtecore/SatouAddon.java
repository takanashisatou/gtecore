package org.satou.gtecore;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraft.data.recipes.FinishedRecipe;
import org.satou.gtecore.api.registry.GTECoreRegistration;
import org.satou.gtecore.data.recipe.GTERecipe;

import java.util.function.Consumer;

@GTAddon
public class SatouAddon implements IGTAddon {
    @Override
    public String addonModId() {
        return GTECore.MOD_ID;
    }

    @Override
    public GTRegistrate getRegistrate() {
        return GTECoreRegistration.GTECore_REGISTRATE;
    }

    @Override
    public void initializeAddon() {}
    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        GTERecipe.init(provider);
    }
}
