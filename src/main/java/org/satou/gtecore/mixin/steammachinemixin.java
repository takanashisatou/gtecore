package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic.*;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.google.common.math.IntMath;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.math.RoundingMode;

import static com.gregtechceu.gtceu.api.recipe.OverclockingLogic.NON_PERFECT_OVERCLOCK;
import static com.gregtechceu.gtceu.api.recipe.OverclockingLogic.PERFECT_OVERCLOCK;

@Mixin(OverclockingLogic.class)
public interface steammachinemixin {// 注入机器并行

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    OverclockingLogic.OCResult runOverclockingLogic(OCParams params, long maxVoltage);

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    default @NotNull ModifierFunction getModifier(MetaMachine machine, GTRecipe recipe, long maxVoltage,
                                                  boolean shouldParallel) {
        long EUt = RecipeHelper.getRealEUt(recipe).getTotalEU();
        recipe.duration /= 20;
        if (recipe.duration == 0) recipe.duration = 1;
        if (EUt == 0) return ModifierFunction.IDENTITY;

        int recipeTier = GTUtil.getTierByVoltage(EUt);
        int maximumTier = GTUtil.getOCTierByVoltage(maxVoltage);
        int OCs = maximumTier - recipeTier;
        if (recipeTier == GTValues.ULV) OCs--;
        if (OCs == 0) return ModifierFunction.IDENTITY;

        int maxParallels;
        if (!shouldParallel || this == PERFECT_OVERCLOCK || this == NON_PERFECT_OVERCLOCK) { // don't parallel
            maxParallels = 1;
        } else {
            // lg = floor(log_4(duration)), which is how many OCs it takes to get duration < 4 with perfect duration
            // factor
            // If OCs <= lg, duration probably won't go below 4
            // If OCs > lg, then we could have 4^(OCs - lg) parallels
            // Note that 4^x = (2^2)^x = 2^(2x) = 1 << 2x
            int lg = IntMath.log2(recipe.duration, RoundingMode.FLOOR) / 2;
            if (lg > OCs) {
                maxParallels = 16;
            } else {
                int p = GTMath.saturatedCast((1L << (2 * (OCs - lg))) + 1);
                maxParallels = ParallelLogic.getParallelAmount(machine, recipe, p);
            }
        }
        // maxParallels *= 64;

        OverclockingLogic.OCParams params = new OverclockingLogic.OCParams(EUt, recipe.duration, OCs, maxParallels);
        OverclockingLogic.OCResult result = runOverclockingLogic(params, maxVoltage);
        return result.toModifier();
    }
    /*
     * @Inject(method = "recipeModifier",at=@At("HEAD"),remap = false)
     * private static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe,
     * CallbackInfoReturnable<ModifierFunction> cir){
     * if (!(machine instanceof SteamParallelMultiblockMachine steamMachine)) {
     * return RecipeModifier.nullWrongType(SteamParallelMultiblockMachine.class, machine);
     * }
     * if (RecipeHelper.getRecipeEUtTier(recipe) > GTValues.LV) return ModifierFunction.NULL;
     * 
     * // Duration = 1.5x base duration
     * // EUt (not steam) = (4/3) * (2/3) * parallels * base EUt, up to a max of 32 EUt
     * long eut = recipe.getInputEUt().getTotalEU();
     * int parallelAmount = ParallelLogic.getParallelAmount(machine, recipe, steamMachine.getMaxParallels());
     * parallelAmount = 64;
     * double eutMultiplier = (eut * 0.8888 * parallelAmount <= 32) ? (0.8888 * parallelAmount) : (32.0 / eut);
     * return ModifierFunction.builder()
     * .inputModifier(ContentModifier.multiplier(parallelAmount))
     * .outputModifier(ContentModifier.multiplier(parallelAmount))
     * .durationMultiplier(1.5)
     * .eutMultiplier(eutMultiplier)
     * .parallels(parallelAmount)
     * .build();
     * }
     */
}
