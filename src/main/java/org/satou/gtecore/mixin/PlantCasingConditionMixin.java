package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import dev.arbor.gtnn.common.recipe.PlantCasingCondition;
import dev.arbor.gtnn.data.GTNNRecipeTypes;
import org.satou.gtecore.common.data.machines.GTEMultiMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlantCasingCondition.class, remap = false)
public abstract class PlantCasingConditionMixin {

    @Shadow
    private int tier;

    @Inject(method = "testCondition", at = @At("HEAD"), cancellable = true)
    private void gtecore$checkIntegratedPlantCasing(GTRecipe recipe, RecipeLogic recipeLogic,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (recipeLogic.getMachine().self().getDefinition() == GTEMultiMachine.INTEGRATED_PETROCHEMICAL_PLANT &&
                recipe.recipeType == GTNNRecipeTypes.INSTANCE.getCHEMICAL_PLANT_RECIPES()) {
            // The integrated plant replaces the fully upgraded chemical plant. Keep the
            // condition in place so reverse checks and every other requirement still apply.
            cir.setReturnValue(tier <= PlantCasingCondition.TUNGSTEN_STEEL);
        }
    }
}
