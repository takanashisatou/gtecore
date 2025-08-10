package org.satou.gtecore;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

public class GTERecipeTypes {
    public static GTRecipeType register(String name, String group, RecipeType<?>... proxyRecipes) {
        var recipeType = new GTRecipeType(Gtecore.id(name), group, proxyRecipes);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, recipeType.registryName, recipeType);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, recipeType.registryName, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(recipeType.registryName, recipeType);
        return recipeType;
    }
    public final static GTRecipeType EASY_BOX = register("easu_box","multiblock")
            .setMaxIOSize(80, 1, 0, 0).setEUIO(IO.IN)
            .setSlotOverlay(false, false, GuiTextures.FURNACE_OVERLAY_1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setIconSupplier(() -> GTMachines.ALLOY_SMELTER[GTValues.LV].asStack())
            .setSteamProgressBar(GuiTextures.PROGRESS_BAR_ARROW_STEAM, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.FURNACE);
    public static void init() {
    }
}
