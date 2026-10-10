package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;

import java.util.List;

/** Persisted, long-count output queues shared by the Plus buffer and its mirror. */
final class MEPatternBufferOutputHandlers {

    private MEPatternBufferOutputHandlers() {}

    static long insert(KeyStorage buffer, AEKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0) return 0;
        long stored = buffer.storage.getLong(key);
        long inserted = Math.min(amount, Long.MAX_VALUE - stored);
        if (!simulate && inserted > 0) {
            buffer.storage.put(key, stored + inserted);
            buffer.onChanged();
        }
        return inserted;
    }

    private static KeyStorage snapshot(KeyStorage buffer) {
        var copy = new KeyStorage();
        copy.storage.putAll(buffer.storage);
        return copy;
    }

    static NotifiableItemStackHandler items(MEBusPartPlusMachine machine, KeyStorage buffer) {
        var handler = new NotifiableItemStackHandler(machine, 1, IO.OUT, IO.NONE, slots -> new ItemSink(buffer)) {
            @Override
            public List<Object> getContents() {
                return List.of();
            }

            @Override
            public double getTotalContentAmount() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe, List<Ingredient> left, boolean simulate) {
                if (io != IO.OUT) return left;
                var target = simulate ? snapshot(buffer) : buffer;
                for (var it = left.iterator(); it.hasNext();) {
                    var ingredient = it.next();
                    ItemStack output;
                    if (ingredient instanceof IntProviderIngredient provider) {
                        provider.setItemStacks(null);
                        provider.setSampledCount(-1);
                        var stacks = simulate ? new ItemStack[] { provider.getMaxSizeStack() } : provider.getItems();
                        if (stacks.length == 0) {
                            it.remove();
                            continue;
                        }
                        output = stacks[0];
                    } else {
                        var stacks = ingredient.getItems();
                        if (stacks.length == 0) {
                            it.remove();
                            continue;
                        }
                        output = stacks[0];
                    }
                    int amount = ingredient instanceof SizedIngredient sized ? sized.getAmount() : output.getCount();
                    if (ingredient instanceof IntProviderIngredient && simulate) amount = output.getCount();
                    long inserted = insert(target, AEItemKey.of(output), amount, false);
                    int remaining = amount - (int) inserted;
                    if (remaining <= 0) it.remove();
                    else if (ingredient instanceof SizedIngredient sized) sized.setAmount(remaining);
                    else output.setCount(remaining);
                }
                return left.isEmpty() ? null : left;
            }
        };
        buffer.setOnContentsChanged(() -> {
            handler.onContentsChanged();
            machine.updateInventorySubscription();
        });
        return handler;
    }

    static NotifiableFluidTank fluids(MEBusPartPlusMachine machine, KeyStorage buffer) {
        var handler = new NotifiableFluidTank(machine, List.of(new FluidSink(buffer)), IO.OUT, IO.NONE) {
            @Override
            public List<Object> getContents() {
                return List.of();
            }

            @Override
            public double getTotalContentAmount() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left,
                                                          boolean simulate) {
                if (io != IO.OUT) return left;
                var target = simulate ? snapshot(buffer) : buffer;
                for (var it = left.iterator(); it.hasNext();) {
                    var ingredient = it.next();
                    FluidStack output;
                    if (ingredient instanceof IntProviderFluidIngredient provider) {
                        provider.setFluidStacks(null);
                        var stacks = simulate ? new FluidStack[] { provider.getMaxSizeStack() } : provider.getStacks();
                        if (stacks.length == 0) {
                            it.remove();
                            continue;
                        }
                        output = stacks[0];
                    } else {
                        var stacks = ingredient.getStacks();
                        if (stacks.length == 0) {
                            it.remove();
                            continue;
                        }
                        output = stacks[0];
                    }
                    int amount = ingredient instanceof IntProviderFluidIngredient && simulate ?
                            output.getAmount() : ingredient.getAmount();
                    long inserted = insert(target, AEFluidKey.of(output), amount, false);
                    int remaining = amount - (int) inserted;
                    if (remaining <= 0) it.remove();
                    else ingredient.setAmount(remaining);
                }
                return left.isEmpty() ? null : left;
            }
        };
        buffer.setOnContentsChanged(() -> {
            handler.onContentsChanged();
            machine.updateInventorySubscription();
        });
        return handler;
    }

    private static final class ItemSink extends CustomItemStackHandler {
        private final KeyStorage buffer;

        private ItemSink(KeyStorage buffer) {
            super(1);
            this.buffer = buffer;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {}

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            long inserted = insert(buffer, AEItemKey.of(stack), stack.getCount(), simulate);
            return stack.copyWithCount(stack.getCount() - (int) inserted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    private static final class FluidSink extends CustomFluidTank {
        private final KeyStorage buffer;

        private FluidSink(KeyStorage buffer) {
            super(Integer.MAX_VALUE);
            this.buffer = buffer;
        }

        @Override
        public void setFluid(FluidStack fluid) {}

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return (int) insert(buffer, AEFluidKey.of(resource), resource.getAmount(), !action.execute());
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
