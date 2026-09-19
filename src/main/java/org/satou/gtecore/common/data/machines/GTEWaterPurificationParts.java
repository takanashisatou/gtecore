package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import net.minecraft.network.chat.Component;
import org.satou.gtecore.common.machine.multiblock.water.ThermalControlHatchPartMachine;
import org.satou.gtecore.common.machine.multiblock.water.ThermalSignalHatchPartMachine;

import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_MACHINES;

public final class GTEWaterPurificationParts {

    public static final PartAbility THERMAL_CONTROL = new PartAbility("gtecore_thermal_control");
    public static final PartAbility THERMAL_SIGNAL = new PartAbility("gtecore_thermal_signal");

    static {
        GTECore_REGISTRATE.creativeModeTab(() -> MORE_MACHINES);
    }

    public static final MachineDefinition THERMAL_CONTROL_HATCH = GTECore_REGISTRATE
            .machine("thermal_control_hatch", ThermalControlHatchPartMachine::new)
            .langValue("Thermal Control Hatch")
            .tier(GTValues.EV)
            .rotationState(RotationState.ALL)
            .abilities(THERMAL_CONTROL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/machine/overlay_data_hatch"))
            .tooltips(Component.translatable("gtecore.water.thermal_hatch.help"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .register();

    public static final MachineDefinition THERMAL_SIGNAL_HATCH = GTECore_REGISTRATE
            .machine("thermal_signal_hatch", ThermalSignalHatchPartMachine::new)
            .langValue("Thermal Signal Hatch")
            .tier(GTValues.EV)
            .rotationState(RotationState.ALL)
            .abilities(THERMAL_SIGNAL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/machine/overlay_data_hatch"))
            .tooltips(Component.translatable("gtecore.water.thermal_signal.help"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .register();

    private GTEWaterPurificationParts() {}

    public static void init() {}
}
