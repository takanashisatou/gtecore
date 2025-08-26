package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.recipe.condition.VentCondition;
import org.satou.gtecore.GTECore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleSteamMachine.class)
public class SimpleSteamMachineRecipeMixin {
    @Inject(remap = false,at = @At("HEAD"),cancellable = true,method = "recipeModifier")
    private static void recipeModifier(MetaMachine machine, GTRecipe recipe, CallbackInfoReturnable<ModifierFunction> cir) {
        if (!(machine instanceof SimpleSteamMachine steamMachine)) {
            cir.setReturnValue(RecipeModifier.nullWrongType(SimpleSteamMachine.class, machine));
        }
        if (RecipeHelper.getRecipeEUtTier(recipe) > GTValues.LV) {
            cir.setReturnValue(ModifierFunction.NULL);
        }
        var builder = ModifierFunction.builder().conditions(VentCondition.INSTANCE);
        cir.setReturnValue(builder.durationMultiplier(0.00001).build());
        cir.cancel();
    }
}
