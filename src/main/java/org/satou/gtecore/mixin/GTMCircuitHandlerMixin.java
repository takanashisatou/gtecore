package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.hepdd.gtmthings.api.machine.trait.ProgrammableCircuitHandler;
import com.hepdd.gtmthings.common.cover.ProgrammableCover;
import com.hepdd.gtmthings.common.item.VirtualItemProviderBehavior;
import com.hepdd.gtmthings.data.CustomItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(ItemStackHandler.class)
public class GTMCircuitHandlerMixin {

    @Overwrite(remap = false)
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.is(CustomItems.VIRTUAL_ITEM_PROVIDER.get())) {
            boolean allow = true;
            if (((ProgrammableCircuitHandler) ((Object) this)).getMachine() instanceof SimpleTieredMachine tieredMachine) {
                allow = false;
                for (CoverBehavior cover : tieredMachine.getCoverContainer().getCovers()) {
                    if (cover instanceof ProgrammableCover) {
                        allow = true;
                        break;
                    }
                }
            }
            if (allow) {
                ((ProgrammableCircuitHandler) ((Object) this)).setStackInSlot(slot, VirtualItemProviderBehavior.getVirtualItem(stack));
                return ItemStack.EMPTY;
            }
        }else if(stack.is(GTItems.PROGRAMMED_CIRCUIT.get())){
            ((ProgrammableCircuitHandler) ((Object) this)).setStackInSlot(slot, stack);
            return ItemStack.EMPTY;
        }
        return stack;
    }
}
