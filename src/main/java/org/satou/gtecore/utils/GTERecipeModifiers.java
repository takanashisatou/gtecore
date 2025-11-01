package org.satou.gtecore.utils;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.common.machine.multiblock.generator.FUEL_ENGINE;

import java.util.Collections;

public class GTERecipeModifiers {
    public static ModifierFunction recipeModifierForSuperStringMixer(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine super_string_mixer_machine)) {
            return RecipeModifier.nullWrongType(WorkableElectricMultiblockMachine.class, machine);
        }
        super_string_mixer_machine.recipeLogic.setMultiParallelLogic(true);
        super_string_mixer_machine.recipeLogic.setMultiParallelCount(2000000000);
        int maxParallel = 2000000000; // get maximum parallel
        int actualParallel = FUEL_ENGINE.getParallelAmount(super_string_mixer_machine, recipe, maxParallel);
        //double eutMultiplier = actualParallel * super_string_mixer_machine.getProductionBoost();
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(actualParallel))
                .outputModifier(ContentModifier.multiplier(actualParallel))
                .parallels(actualParallel)
                .build();
    }

    public static @NotNull ModifierFunction recipeModifierForSuperStringOsCillatorArray(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine super_string_oscillator_array_machine)) {
            return RecipeModifier.nullWrongType(WorkableElectricMultiblockMachine.class, machine);
        }
        super_string_oscillator_array_machine.recipeLogic.setMultiParallelLogic(true);
        super_string_oscillator_array_machine.recipeLogic.setMultiParallelCount(2000000000);
        int maxParallel = 2000000000; // get maximum parallel
        int actualParallel = FUEL_ENGINE.getParallelAmount(super_string_oscillator_array_machine, recipe, maxParallel);
        //double eutMultiplier = actualParallel * super_string_mixer_machine.getProductionBoost();
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(actualParallel))
                .outputModifier(ContentModifier.multiplier(actualParallel))
                .parallels(actualParallel)
                .build();
    }
}
