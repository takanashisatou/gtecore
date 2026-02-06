package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Predicates.class)
public class PredicatesMixin {
    /**
     * @author takanashisatou
     * @reason Enable machines to freely use any number of energy or power hatches
     */
    @Overwrite(remap = false)
    public static TraceabilityPredicate autoAbilities(GTRecipeType[] recipeType, boolean checkEnergyIn, boolean checkEnergyOut, boolean checkItemIn, boolean checkItemOut, boolean checkFluidIn, boolean checkFluidOut) {
        TraceabilityPredicate predicate = new TraceabilityPredicate();
        GTRecipeType[] var8;
        int var9;
        int var10;
        GTRecipeType type;
        if (checkEnergyIn) {
            var8 = recipeType;
            var9 = recipeType.length;

            for(var10 = 0; var10 < var9; ++var10) {
                type = var8[var10];
                if (type.getMaxInputs(EURecipeCapability.CAP) > 0) {
                    predicate = predicate.or(Predicates.abilities(PartAbility.INPUT_ENERGY).setPreviewCount(1));
                    break;
                }
            }
        }

        if (checkEnergyOut) {
            var8 = recipeType;
            var9 = recipeType.length;

            for(var10 = 0; var10 < var9; ++var10) {
                type = var8[var10];
                if (type.getMaxOutputs(EURecipeCapability.CAP) > 0) {
                    predicate = predicate.or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setPreviewCount(1));
                    break;
                }
            }
        }

        if (checkItemIn) {
            var8 = recipeType;
            var9 = recipeType.length;

            for(var10 = 0; var10 < var9; ++var10) {
                type = var8[var10];
                if (type.getMaxInputs(ItemRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1));
                    break;
                }
            }
        }

        if (checkItemOut) {
            var8 = recipeType;
            var9 = recipeType.length;

            for(var10 = 0; var10 < var9; ++var10) {
                type = var8[var10];
                if (type.getMaxOutputs(ItemRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1));
                    break;
                }
            }
        }

        if (checkFluidIn) {
            var8 = recipeType;
            var9 = recipeType.length;

            for(var10 = 0; var10 < var9; ++var10) {
                type = var8[var10];
                if (type.getMaxInputs(FluidRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1));
                    break;
                }
            }
        }

        if (checkFluidOut) {
            var8 = recipeType;
            var9 = recipeType.length;

            for(var10 = 0; var10 < var9; ++var10) {
                type = var8[var10];
                if (type.getMaxOutputs(FluidRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1));
                    break;
                }
            }
        }

        return predicate;
    }
}
