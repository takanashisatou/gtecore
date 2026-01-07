package org.satou.gtecore.common.data.machines;

import com.google.common.primitives.Longs;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.hepdd.gtmthings.api.misc.WirelessEnergyContainer;
import net.minecraft.core.Direction;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class SuperBatterBufferMachine extends BatteryBufferMachine {

    public SuperBatterBufferMachine(IMachineBlockEntity holder, int tier, int inventorySize, Object... args) {
        super(holder, tier, inventorySize, args);
    }
    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        return new SuperEnergyBatteryTrait((int) args[0]);
    }
    private List<IElectricItem> getNonEmptyBatteries() {
        List<IElectricItem> batteries = new ArrayList<>();
        for (int i = 0; i < batteryInventory.getSlots(); i++) {
            var batteryStack = batteryInventory.getStackInSlot(i);
            var electricItem = GTCapabilityHelper.getElectricItem(batteryStack);
            if (electricItem != null) {
                if (electricItem.canProvideChargeExternally() && electricItem.getCharge() > 0) {
                    batteries.add(electricItem);
                }
            }
        }
        return batteries;
    }
    private List<Object> getNonFullBatteries() {
        List<Object> batteries = new ArrayList<>();
        for (int i = 0; i < batteryInventory.getSlots(); i++) {
            var batteryStack = batteryInventory.getStackInSlot(i);
            var electricItem = GTCapabilityHelper.getElectricItem(batteryStack);
            if (electricItem != null) {
                if (electricItem.getCharge() < electricItem.getMaxCharge()) {
                    batteries.add(electricItem);
                }
            } else if (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE) {
                IEnergyStorage energyStorage = GTCapabilityHelper.getForgeEnergyItem(batteryStack);
                if (energyStorage != null) {
                    if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
                        batteries.add(energyStorage);
                    }
                }
            }
        }
        return batteries;
    }
    private List<Object> getAllBatteries() {
        List<Object> batteries = new ArrayList<>();
        for (int i = 0; i < batteryInventory.getSlots(); i++) {
            var batteryStack = batteryInventory.getStackInSlot(i);
            var electricItem = GTCapabilityHelper.getElectricItem(batteryStack);
            if (electricItem != null) {
                batteries.add(electricItem);
            } else if (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE) {
                IEnergyStorage energyStorage = GTCapabilityHelper.getForgeEnergyItem(batteryStack);
                if (energyStorage != null) {
                    batteries.add(energyStorage);
                }
            }
        }
        return batteries;
    }
    protected class SuperEnergyBatteryTrait extends NotifiableEnergyContainer
    {
        protected SuperEnergyBatteryTrait(int inventorySize) {
        super(SuperBatterBufferMachine.this, GTValues.V[tier] * inventorySize * 32L, GTValues.V[tier],
                inventorySize * AMPS_PER_BATTERY, GTValues.V[tier], inventorySize);
        this.setSideInputCondition(side -> side != getFrontFacing() && isWorkingEnabled());
        this.setSideOutputCondition(side -> side == getFrontFacing() && isWorkingEnabled());
        }

        @Override
        public void checkOutputSubscription() {
        if (isWorkingEnabled()) {
            super.checkOutputSubscription();
        } else if (outputSubs != null) {
            outputSubs.unsubscribe();
            outputSubs = null;
        }
    }

        @Override
        public void serverTick() {
        var outFacing = getFrontFacing();
        var energyContainer = GTCapabilityHelper.getEnergyContainer(getLevel(), getPos().relative(outFacing),
                outFacing.getOpposite());
        if (energyContainer == null) {
            return;
        }

        var voltage = getOutputVoltage();
        var batteries = getNonEmptyBatteries();

        if (!batteries.isEmpty()) {
            // Prioritize as many packets as available of energy created
            long internalAmps = Math.abs(Math.min(0, getInternalStorage() / voltage));
            long genAmps = Math.max(0, batteries.size() - internalAmps);
            long outAmps = 0L;

            if (genAmps > 0) {
                outAmps = energyContainer.acceptEnergyFromNetwork(outFacing.getOpposite(), voltage, genAmps);
                if (outAmps == 0 && internalAmps == 0)
                    return;
            }

            long energy = (outAmps + internalAmps) * voltage;
            long distributed = energy / batteries.size();

            boolean changed = false;
            for (IElectricItem electricItem : batteries) {
                var charged = electricItem.discharge(Long.MAX_VALUE, getTier(), true, true, false);
                if (charged > 0) {
                    changed = true;
                }

                WirelessEnergyContainer container = WirelessEnergyContainer.getOrCreateContainer(getOwnerUUID());
                container.setStorage(container.getStorage().add(BigInteger.valueOf(charged)));

                //energyOutputPerSec += charged;
            }

            if (changed) {
                SuperBatterBufferMachine.this.markDirty();
                checkOutputSubscription();
            }

            // Subtract energy created out of thin air from the buffer
            setEnergyStored(0);;
        }
    }

        @Override
        public long acceptEnergyFromNetwork(@Nullable Direction side, long voltage, long amperage) {
        var latestTimeStamp = getMachine().getOffsetTimer();
            //WirelessEnergyContainer container = getWirelessEnergyContainer();
        if (lastTimeStamp < latestTimeStamp) {
            amps = 0;
            lastTimeStamp = latestTimeStamp;
        }
        if (amperage <= 0 || voltage <= 0)
            return 0;

        var batteries = getNonFullBatteries();
        var leftAmps = batteries.size() * AMPS_PER_BATTERY - amps;
        var usedAmps = Math.max(leftAmps, amperage);
        if (leftAmps <= 0)
            return 0;

        if (side == null || inputsEnergy(side)) {
            if (voltage > getInputVoltage()) {
                doExplosion(GTUtil.getExplosionPower(voltage));
                return usedAmps;
            }

            // Prioritizes as many packets as available from the buffer
            long internalAmps = Math.min(leftAmps, Math.max(0, getInternalStorage() / voltage));

            usedAmps = Math.max(usedAmps, leftAmps - internalAmps);
            amps += usedAmps;

            long energy = (usedAmps + internalAmps) * voltage;
            WirelessEnergyContainer container = WirelessEnergyContainer.getOrCreateContainer(getOwnerUUID());
            BigInteger WirelssEnergy = container.getStorage();
            WirelssEnergy.add(BigInteger.valueOf(energy));
            long afterEnergy;
            if(WirelssEnergy.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                afterEnergy = Long.MAX_VALUE;
            }else{
                afterEnergy = WirelssEnergy.longValue();
            }

            long distributed = energy / batteries.size();
            container.setStorage(container.getStorage().subtract(BigInteger.valueOf(afterEnergy - energy)));
            energy = afterEnergy;
            boolean changed = false;
            for (Object item : batteries) {
                long charged = 0;
                if (item instanceof IElectricItem electricItem) {
                    charged = electricItem.charge(
                            energy, getTier(),
                            true, false);
                } else if (item instanceof IEnergyStorage energyStorage) {
                    charged = FeCompat.insertEu(energyStorage,
                            energy, false);
                }
                if (charged > 0) {
                    changed = true;
                }
                energy -= charged;
                energyInputPerSec += charged;
            }

            if (changed) {
                SuperBatterBufferMachine.this.markDirty();
                checkOutputSubscription();
            }
            //container.setStorage(container.getStorage().subtract(new BigInteger(String.valueOf(WirelessAmps * voltage))));

            // Remove energy used and then transfer overflow energy into the internal buffer
            setEnergyStored(getInternalStorage() - internalAmps * voltage + energy);

            return usedAmps;
        }
        return 0;
    }

        @Override
        public long getEnergyCapacity() {
        long energyCapacity = 0L;
        for (Object battery : getAllBatteries()) {
            if (battery instanceof IElectricItem electricItem) {
                energyCapacity += electricItem.getMaxCharge();
            } else if (battery instanceof IEnergyStorage energyStorage) {
                energyCapacity += FeCompat.toEu(energyStorage.getMaxEnergyStored(), FeCompat.ratio(false));
            }
        }
        return energyCapacity;
    }

        @Override
        public long getEnergyStored() {
        long energyStored = 0L;
        for (Object battery : getAllBatteries()) {
            if (battery instanceof IElectricItem electricItem) {
                energyStored += electricItem.getCharge();
            } else if (battery instanceof IEnergyStorage energyStorage) {
                energyStored += FeCompat.toEu(energyStorage.getEnergyStored(), FeCompat.ratio(false));
            }
        }
        return energyStored;
    }

        private long getInternalStorage() {
        return energyStored;
        }
    }
}
