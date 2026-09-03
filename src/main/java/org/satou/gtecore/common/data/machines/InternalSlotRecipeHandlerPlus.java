package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroupDistinctness;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import net.minecraft.world.item.crafting.Ingredient;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InternalSlotRecipeHandlerPlus {

    @Getter
    private final List<RecipeHandlerList> slotHandlers;
    @Getter
    private final RecipeHandlerList sharedHandlerList;

    public InternalSlotRecipeHandlerPlus(MEPatternBufferPlusPartMachine buffer, MEPatternBufferPlusPartMachine.InternalSlot[] slots) {
        this.slotHandlers = new ArrayList<>(slots.length);
        for (int i = 0; i < slots.length; i++) {
            var rhl = new SlotRHL(buffer, slots[i], i);
            slotHandlers.add(rhl);
        }
        this.sharedHandlerList = RecipeHandlerList.of(IO.IN, buffer.getCircuitInventory(), buffer.getShareInventory(), buffer.getShareTank());
        this.sharedHandlerList.setGroup(RecipeHandlerGroupDistinctness.BYPASS_DISTINCT);
    }

    @Getter
    protected static class SlotRHL extends RecipeHandlerList {

        private final SlotItemRecipeHandler itemRecipeHandler;
        private final SlotFluidRecipeHandler fluidRecipeHandler;
        private final NotifiableItemStackHandler circuitInventory;

        public SlotRHL(MEPatternBufferPlusPartMachine buffer, MEPatternBufferPlusPartMachine.InternalSlot slot, int idx) {
            super(IO.IN);
            itemRecipeHandler = new SlotItemRecipeHandler(buffer, slot, idx);
            fluidRecipeHandler = new SlotFluidRecipeHandler(buffer, slot, idx);
            circuitInventory = slot.circuitInventory;
            addHandlers(itemRecipeHandler, fluidRecipeHandler, circuitInventory);
            this.setGroup(RecipeHandlerGroupDistinctness.BUS_DISTINCT);
        }

        @Override
        public boolean isDistinct() {
            return true;
        }

        @Override
        public void setDistinct(boolean ignored, boolean notify) {}
    }

    @Getter
    protected static class SlotItemRecipeHandler extends NotifiableRecipeHandlerTrait<Ingredient> {

        private final MEPatternBufferPlusPartMachine.InternalSlot slot;
        private final int priority;

        private final RecipeCapability<Ingredient> capability = ItemRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotItemRecipeHandler(MEPatternBufferPlusPartMachine buffer, MEPatternBufferPlusPartMachine.InternalSlot slot, int index) {
            super(buffer);
            this.slot = slot;
            this.priority = IFilteredHandler.HIGH + index + 1;

            slot.addListener(this::notifyListeners);
        }

        @Override
        public int getSize() {
            return slot.isItemEmpty() ? 0 : Math.max(1, slot.getItems().size());
        }

        @Override
        public List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe, List<Ingredient> left, boolean simulate) {
            if (io != IO.IN || slot.isItemEmpty()) return left;
            return slot.handleItemInternal(left, simulate);
        }

        @Override
        public @NotNull List<Object> getContents() {
            if (slot.isItemEmpty()) return Collections.emptyList();
            return new ArrayList<>(slot.getItems());
        }

        @Override
        public double getTotalContentAmount() {
            return slot.getTotalItemCount();
        }
    }

    @Getter
    protected static class SlotFluidRecipeHandler extends NotifiableRecipeHandlerTrait<FluidIngredient> {

        private final MEPatternBufferPlusPartMachine.InternalSlot slot;
        private final int priority;

        private final RecipeCapability<FluidIngredient> capability = FluidRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotFluidRecipeHandler(MEPatternBufferPlusPartMachine buffer, MEPatternBufferPlusPartMachine.InternalSlot slot, int index) {
            super(buffer);
            this.slot = slot;
            this.priority = IFilteredHandler.HIGH + index + 1;
            slot.addListener(this::notifyListeners);
        }

        @Override
        public int getSize() {
            return slot.isFluidEmpty() ? 0 : Math.max(1, slot.getFluids().size());
        }

        @Override
        public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left,
                                                       boolean simulate) {
            if (io != IO.IN || slot.isFluidEmpty()) return left;
            return slot.handleFluidInternal(left, simulate);
        }

        @Override
        public @NotNull List<Object> getContents() {
            if (slot.isFluidEmpty()) return Collections.emptyList();
            return new ArrayList<>(slot.getFluids());
        }

        @Override
        public double getTotalContentAmount() {
            return slot.getTotalFluidAmount();
        }
    }
}
