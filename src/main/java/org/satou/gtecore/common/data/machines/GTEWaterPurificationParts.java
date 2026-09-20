package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import net.minecraft.network.chat.Component;
import org.satou.gtecore.common.machine.multiblock.water.EdiLoadSignalHatchPartMachine;
import org.satou.gtecore.common.machine.multiblock.water.EdiLoadSignalBlock;
import org.satou.gtecore.common.machine.multiblock.water.EdiRegenerationControlHatchPartMachine;
import org.satou.gtecore.common.machine.multiblock.water.ThermalControlHatchPartMachine;
import org.satou.gtecore.common.machine.multiblock.water.ThermalSignalHatchPartMachine;
import org.satou.gtecore.common.machine.multiblock.water.UvLampHatchPartMachine;

import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_MACHINES;

public final class GTEWaterPurificationParts {

    public static final PartAbility THERMAL_CONTROL = new PartAbility("gtecore_thermal_control");
    public static final PartAbility THERMAL_SIGNAL = new PartAbility("gtecore_thermal_signal");
    public static final PartAbility UV_LAMP = new PartAbility("gtecore_uv_lamp");

    public static final PartAbility EDI_LOAD_SIGNAL = new PartAbility("gtecore_edi_load_signal");
    public static final PartAbility EDI_REGENERATION_CONTROL = new PartAbility("gtecore_edi_regeneration_control");

    static {
        GTECore_REGISTRATE.creativeModeTab(() -> MORE_MACHINES);
    }

    public static final MachineDefinition THERMAL_CONTROL_HATCH = GTECore_REGISTRATE
            .machine("thermal_control_hatch", ThermalControlHatchPartMachine::new)
            .langValue("Thermal Control Hatch")
            .tier(GTValues.UEV)
            .rotationState(RotationState.ALL)
            .abilities(THERMAL_CONTROL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/machine/overlay_data_hatch"))
            .tooltips(Component.translatable("gtecore.water.thermal_hatch.help"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .register();

    public static final MachineDefinition THERMAL_SIGNAL_HATCH = GTECore_REGISTRATE
            .machine("thermal_signal_hatch", ThermalSignalHatchPartMachine::new)
            .langValue("Thermal Signal Hatch")
            .tier(GTValues.UEV)
            .rotationState(RotationState.ALL)
            .abilities(THERMAL_SIGNAL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/machine/overlay_data_hatch"))
            .tooltips(Component.translatable("gtecore.water.thermal_signal.help"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .register();

    public static final MachineDefinition UV_LAMP_HATCH = GTECore_REGISTRATE
            .machine("uv_lamp_hatch", UvLampHatchPartMachine::new)
            .langValue("UV Lamp Hatch")
            .tier(GTValues.UEV)
            .rotationState(RotationState.ALL)
            .abilities(UV_LAMP)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/machine/overlay_data_hatch"))
            .tooltips(Component.translatable("gtecore.water.uv.lamp.help"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .register();

    public static final MachineDefinition EDI_LOAD_SIGNAL_HATCH = GTECore_REGISTRATE
            .machine("edi_load_signal_hatch", MachineDefinition::new, EdiLoadSignalHatchPartMachine::new,
                    EdiLoadSignalBlock::new, MetaMachineItem::new, MetaMachineBlockEntity::new)
            .langValue("EDI Load Signal Hatch")
            .tier(GTValues.UEV)
            .rotationState(RotationState.ALL)
            .abilities(EDI_LOAD_SIGNAL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/machine/overlay_data_hatch"))
            .tooltips(Component.translatable("gtecore.water.edi.signal_hatch.help"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .register();

    public static final MachineDefinition EDI_REGENERATION_CONTROL_HATCH = GTECore_REGISTRATE
            .machine("edi_regeneration_control_hatch", EdiRegenerationControlHatchPartMachine::new)
            .langValue("EDI Regeneration Control Hatch")
            .tier(GTValues.UEV)
            .rotationState(RotationState.ALL)
            .abilities(EDI_REGENERATION_CONTROL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/machine/overlay_data_hatch"))
            .tooltips(Component.translatable("gtecore.water.edi.control_hatch.help"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .register();

    private GTEWaterPurificationParts() {}

    public static void init() {}
}
