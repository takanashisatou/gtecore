package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleGeneratorMachine;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.RecipeAmperageEnergyContainer;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleGeneratorMachine.class)
public class SimpleGeneratorMixin {
    @Inject(method = "createEnergyContainer", at = @At("HEAD"), cancellable = true,remap = false)
    protected void createEnergyContainer(Object[] args, CallbackInfoReturnable<NotifiableEnergyContainer> cir) {
        long tierVoltage = GTValues.V[((WorkableTieredMachine)((Object)this)).getTier()];
        var energyContainer = RecipeAmperageEnergyContainer.makeEmitterContainer(((WorkableTieredMachine)((Object)this)), tierVoltage * 64L,
                tierVoltage, 8);
        energyContainer.setSideOutputCondition(side -> !((WorkableTieredMachine)((Object)this)).hasFrontFacing() || side == ((WorkableTieredMachine)((Object)this)).getFrontFacing());
        cir.setReturnValue(energyContainer);
        cir.cancel();
    }
    @Inject(method = "recipeModifier", at = @At("HEAD"), cancellable = true,remap = false)
    private static void recipeModifier(MetaMachine machine, GTRecipe recipe, CallbackInfoReturnable<ModifierFunction> cir) {
        if (!(machine instanceof SimpleGeneratorMachine generator)) {
            cir.setReturnValue(RecipeModifier.nullWrongType(SimpleGeneratorMachine.class, machine));
            cir.cancel();
            return;
        }
        long EUt = recipe.getOutputEUt().getTotalEU();
        if (EUt <= 0){
            cir.setReturnValue(ModifierFunction.NULL);
            cir.cancel();
            return;
        }

        int maxParallel = (int) (generator.getOverclockVoltage() / EUt);
        int parallels = ParallelLogic.getParallelAmountFast(generator, recipe, maxParallel);
         cir.setReturnValue(
                 ModifierFunction.builder()
                         .inputModifier(ContentModifier.multiplier(parallels))
                         .outputModifier(ContentModifier.multiplier(parallels))
                         .parallels(parallels * 8)
                         .eutModifier(ContentModifier.multiplier(parallels))
                         .amperageModifier(ContentModifier.multiplier(8))
                 .build());
         cir.cancel();
    }
}
