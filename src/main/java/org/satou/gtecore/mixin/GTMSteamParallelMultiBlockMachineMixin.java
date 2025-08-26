package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SteamParallelMultiblockMachine.class)
public class GTMSteamParallelMultiBlockMachineMixin {
    @Overwrite(remap = false)
    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof SteamParallelMultiblockMachine steamMachine)) {
            return RecipeModifier.nullWrongType(SteamParallelMultiblockMachine.class, machine);
        }
        if (RecipeHelper.getRecipeEUtTier(recipe) > GTValues.LV) return ModifierFunction.NULL;

        // Duration = 1.5x base duration
        // EUt (not steam) = (4/3) * (2/3) * parallels * base EUt, up to a max of 32 EUt
        long eut = recipe.getInputEUt().getTotalEU();
        int parallelAmount = ParallelLogic.getParallelAmount(machine, recipe, 1024);
        double eutMultiplier = (eut * 0.8888 * parallelAmount <= 32) ? (0.8888 * parallelAmount) : (32.0 / eut);
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallelAmount))
                .outputModifier(ContentModifier.multiplier(parallelAmount))
                .durationMultiplier(0.05)
                .eutMultiplier(eutMultiplier)
                .parallels(parallelAmount)
                .build();
    }
}
