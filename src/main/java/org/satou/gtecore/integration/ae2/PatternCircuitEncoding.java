package org.satou.gtecore.integration.ae2;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTItems;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Keeps programmed circuit selectors that JEI exposes as non-consumable ingredients. */
public final class PatternCircuitEncoding {

    private PatternCircuitEncoding() {}

    public static List<List<GenericStack>> retainCircuits(List<List<GenericStack>> inputs, GTRecipe recipe) {
        var result = new ArrayList<>(inputs);
        for (var content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            if (content.chance != 0) continue;
            var ingredient = ItemRecipeCapability.CAP.of(content.content);
            var circuits = Arrays.stream(ingredient.getItems())
                    .filter(stack -> stack.is(GTItems.PROGRAMMED_CIRCUIT.get()))
                    .map(stack -> new GenericStack(AEItemKey.of(stack.copyWithCount(1)), 1))
                    .distinct()
                    .toList();
            if (!circuits.isEmpty() && result.stream().flatMap(List::stream)
                    .noneMatch(input -> circuits.stream().anyMatch(circuit -> circuit.what().equals(input.what())))) {
                result.add(circuits);
            }
        }
        return List.copyOf(result);
    }
}
