package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import org.satou.gtecore.integration.ae2.PatternCircuitEncoding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Vanilla and ExtendedAE pattern terminals share AE2's processing-recipe transfer. */
@Mixin(value = EncodePatternTransferHandler.class, remap = false)
public abstract class PatternCircuitTransferMixin {

    @WrapOperation(
            method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At(value = "INVOKE",
                    target = "Lappeng/integration/modules/jei/GenericEntryStackHelper;ofInputs(Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;)Ljava/util/List;"))
    private List<List<GenericStack>> gtecore$retainCircuits(IRecipeSlotsView slots,
                                                         Operation<List<List<GenericStack>>> original,
                                                         @Local(argsOnly = true) Object recipe) {
        var inputs = original.call(slots);
        return recipe instanceof GTRecipe gtRecipe ? PatternCircuitEncoding.retainCircuits(inputs, gtRecipe) : inputs;
    }
}
