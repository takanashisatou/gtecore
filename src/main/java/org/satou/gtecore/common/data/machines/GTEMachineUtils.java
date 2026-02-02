package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.satou.gtecore.api.registry.GTECoreRegistration;

import static com.gregtechceu.gtceu.api.GTValues.VCF;
import static com.gregtechceu.gtceu.api.GTValues.VOLTAGE_NAMES;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.ALL_TIERS;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.registerTieredMachines;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

public class GTEMachineUtils {
    public static MachineDefinition[] registerBatteryBuffer(int batterySlotSize) {
        return registerBatteryBuffer(GTECoreRegistration.GTECore_REGISTRATE, batterySlotSize);
    }

    public static MachineDefinition[] registerBatteryBuffer(GTRegistrate registrate, int batterySlotSize) {
        return registerTieredMachines(registrate, "super_battery_buffer_" + batterySlotSize + "x",
                (holder, tier) -> new SuperBatterBufferMachine(holder, tier, batterySlotSize),
                (tier, builder) -> builder
                        .rotationState(RotationState.ALL)
                        .model(GTMachineModels.createBatteryBufferModel(batterySlotSize))
                        .langValue("%s %sx Super Battery Buffer".formatted(
                                VCF[tier] + VOLTAGE_NAMES[tier] + ChatFormatting.RESET,
                                batterySlotSize))
                        .tooltips(
                                Component.translatable("gtceu.universal.tooltip.item_storage_capacity",
                                        batterySlotSize),
                                Component.translatable("gtceu.universal.tooltip.voltage_in_out",
                                        FormattingUtil.formatNumbers(GTValues.V[tier]),
                                        GTValues.VNF[tier]),
                                Component.translatable("gtceu.universal.tooltip.amperage_in_till",
                                        batterySlotSize * BatteryBufferMachine.AMPS_PER_BATTERY),
                                Component.translatable("gtceu.universal.tooltip.amperage_out_till", batterySlotSize))
                        .register(),
                GTValues.MAX);
    }
}
