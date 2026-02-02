package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.ae2.machine.MEInputHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferProxyPartMachine;
import net.minecraft.network.chat.Component;
import org.satou.gtecore.api.registry.GTECoreRegistration;

import static com.gregtechceu.gtceu.api.GTValues.EV;
import static com.gregtechceu.gtceu.api.GTValues.LuV;
import static com.gregtechceu.gtceu.common.data.GTCreativeModeTabs.MACHINE;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.registerBatteryBuffer;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_MACHINES;

public class GTEMachines {
    static {
        GTECore_REGISTRATE.creativeModeTab(()->MORE_MACHINES);
    }
    public static final MachineDefinition[] SUPER_BATTERY_BUFFER_1 = GTEMachineUtils.registerBatteryBuffer(1);
    public final static MachineDefinition STEAM_IMPORT_HATCH_ME = GTECore_REGISTRATE
            .machine("me_steam_hatch", MEInputHatchPartMachine::new)
            .langValue("ME Steam Hatch")
            .tier(EV)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.STEAM)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"),
                    Component.translatable("gtceu.machine.me.fluid_import.tooltip"),
                    Component.translatable("gtceu.machine.me.copy_paste.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .register();
    public static final MachineDefinition ME_PATTERN_BUFFER_PLUS = GTECore_REGISTRATE
            .machine("me_pattern_buffer_plus", MEPatternBufferPlusPartMachine::new)
            .tier(LuV)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS,
                    PartAbility.EXPORT_ITEMS)
            .rotationState(RotationState.ALL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch"))
            .langValue("ME Pattern Buffer Plus")
            .tooltips(Component.translatable("block.gtecore.pattern_buffer_plus.desc.0"),
                    Component.translatable("block.gtecore.pattern_buffer_plus.desc.1"),
                    Component.translatable("block.gtecore.pattern_buffer_plus.desc.2"),
                    Component.translatable("block.gtecore.pattern_buffer_plus.desc.3"))
            .register();
    public static final MachineDefinition ME_PATTERN_BUFFER_PROXY_PLUS = GTECore_REGISTRATE
            .machine("me_pattern_buffer_proxy_plus", MEPatternBufferProxyPlusPartMachine::new)
            .tier(LuV)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS,
                    PartAbility.EXPORT_ITEMS)
            .rotationState(RotationState.ALL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch_proxy"))
            .langValue("ME Pattern Buffer Proxy Plus")
            .tooltips(
                    Component.translatable("block.gtecore.pattern_buffer_proxy_plus.desc.0"),
                    Component.translatable("block.gtecore.pattern_buffer_proxy_plus.desc.1"),
                    Component.translatable("block.gtecore.pattern_buffer_proxy_plus.desc.2"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .register();
    public static void init() {
        GTEMultiMachine.init();
        GTEMultiMachines2.init();
    }
}
