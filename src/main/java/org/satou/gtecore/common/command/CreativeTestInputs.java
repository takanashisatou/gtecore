package org.satou.gtecore.common.command;

import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputBusPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Initializes GTMThings 1.5.4's actual creative configuration, including its phantom GUI and refill loop. */
final class CreativeTestInputs {
    private CreativeTestInputs() {}

    static void energy(CreativeEnergyHatchPartMachine hatch) {
        energy(hatch, GTValues.EV);
    }

    static void energy(CreativeEnergyHatchPartMachine hatch, int tier) {
        try {
            long voltage = GTValues.V[tier];
            int amps = 256;
            // These are GTMThings' persisted GUI settings. Refresh its native infinite
            // container before forming the plant, without the GUI setter's queued invalidation.
            var voltageField = CreativeEnergyHatchPartMachine.class.getDeclaredField("voltage");
            var ampsField = CreativeEnergyHatchPartMachine.class.getDeclaredField("amps");
            var capacityField = CreativeEnergyHatchPartMachine.class.getDeclaredField("maxEnergy");
            var tierField = CreativeEnergyHatchPartMachine.class.getDeclaredField("setTier");
            for (var field : List.of(voltageField, ampsField, capacityField, tierField)) field.setAccessible(true);
            voltageField.setLong(hatch, voltage);
            ampsField.setInt(hatch, amps);
            capacityField.setLong(hatch, voltage * amps);
            tierField.setInt(hatch, tier);
            hatch.loadCustomPersistedData(new CompoundTag());
            hatch.onChanged();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法初始化 GTMThings 创造能源仓配置", exception);
        }
    }

    static void fluid(CreativeInputHatchPartMachine hatch, FluidStack template) {
        try {
            // GTMThings exposes the live tank but keeps its persistent GUI configuration private.
            // Use its own configuration method so the GUI, refill map and saved state agree.
            var setFluid = CreativeInputHatchPartMachine.class.getDeclaredMethod("setFluid", int.class, FluidStack.class);
            setFluid.setAccessible(true);
            setFluid.invoke(hatch, 0, template.copy());
            var supply = template.copy();
            supply.setAmount(Integer.MAX_VALUE);
            hatch.tank.setFluidInTank(0, supply);
            var subscribe = CreativeInputHatchPartMachine.class.getDeclaredMethod("updateTankSubscription");
            subscribe.setAccessible(true);
            subscribe.invoke(hatch);
            hatch.onChanged();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法初始化 GTMThings 创造输入仓配置", exception);
        }
    }

    static void item(CreativeInputBusPartMachine bus, ItemStack template) {
        try {
            var storageField = CreativeInputBusPartMachine.class.getDeclaredField("creativeStorage");
            storageField.setAccessible(true);
            if (!(storageField.get(bus) instanceof ItemStackTransfer storage)) {
                throw new IllegalStateException("GTMThings 创造输入总线配置类型不兼容");
            }
            storage.setStackInSlot(0, template.copyWithCount(1));
            var configuredItems = CreativeInputBusPartMachine.class.getDeclaredField("lstItem");
            configuredItems.setAccessible(true);
            configuredItems.set(bus, new ArrayList<>(List.of(template.getItem())));
            bus.getInventory().setStackInSlot(0, template.copyWithCount(Integer.MAX_VALUE));
            var subscribe = CreativeInputBusPartMachine.class.getDeclaredMethod("updateInventorySubscription");
            subscribe.setAccessible(true);
            subscribe.invoke(bus);
            bus.onChanged();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法初始化 GTMThings 创造输入总线配置", exception);
        }
    }
}
