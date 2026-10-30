package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exposes shared storage in each distinct recipe group without subscribing it once per slot.
 * The separate shared handler list owns change subscriptions; this view owns no storage or listeners.
 */
final class SharedRecipeHandlerView<T> implements IRecipeHandler<T> {

    private final IRecipeHandler<T> delegate;

    SharedRecipeHandlerView(IRecipeHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<T> handleRecipeInner(IO io, GTRecipe recipe, List<T> left, boolean simulate) {
        return delegate.handleRecipeInner(io, recipe, left, simulate);
    }

    @Override
    public List<T> handleRecipe(IO io, GTRecipe recipe, List<?> left, boolean simulate) {
        return delegate.handleRecipe(io, recipe, left, simulate);
    }

    @Override
    public T copyContent(Object content) {
        return delegate.copyContent(content);
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public @NotNull List<Object> getContents() {
        return delegate.getContents();
    }

    @Override
    public double getTotalContentAmount() {
        return delegate.getTotalContentAmount();
    }

    @Override
    public boolean isDistinct() {
        return delegate.isDistinct();
    }

    @Override
    public boolean shouldSearchContent() {
        return delegate.shouldSearchContent();
    }

    @Override
    public RecipeCapability<T> getCapability() {
        return delegate.getCapability();
    }

    @Override
    public boolean test(T ingredient) {
        return delegate.test(ingredient);
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }
}
