package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A physical redstone thermostat output; the player wires it to the separate control hatch. */
public class ThermalSignalHatchPartMachine extends MultiblockPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ThermalSignalHatchPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);
    private static final int HYSTERESIS_DECI_C = 10;

    // Start unpowered after loading, until a real formed controller has been found.
    @DescSynced private int signal;
    private @Nullable TickableSubscription signalSubscription;

    public ThermalSignalHatchPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            signal = 0;
            updateSignal();
            signalSubscription = subscribeServerTick(signalSubscription, this::updateThermalSignal);
        }
    }

    @Override
    public void onUnload() {
        if (signalSubscription != null) {
            signalSubscription.unsubscribe();
            signalSubscription = null;
        }
        if (!isRemote()) setSignal(0);
        super.onUnload();
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        if (!isRemote()) setSignal(0);
    }

    private void updateThermalSignal() {
        for (var controller : getControllers()) {
            if (controller instanceof ThermalPurificationUnitMachine thermalController &&
                    getLevel().hasChunkAt(thermalController.getPos()) && thermalController.isFormed()) {
                var thermal = thermalController.getThermalState();
                int center = (thermal.lowerDeciC() + thermal.upperDeciC()) / 2;
                // Continue controlling during faults, so the player can bring it back into range to repair.
                if (thermal.temperatureDeciC() < center - HYSTERESIS_DECI_C) setSignal(15);
                else if (thermal.temperatureDeciC() > center + HYSTERESIS_DECI_C) setSignal(0);
                return;
            }
        }
        setSignal(0);
    }

    private void setSignal(int next) {
        if (signal == next) return;
        signal = next;
        updateSignal();
    }

    @Override
    public int getOutputSignal(@Nullable Direction side) {
        // Minecraft queries weak power in the direction from the receiver towards this block.
        return side == getFrontFacing().getOpposite() ? signal : 0;
    }

    @Override
    public int getOutputDirectSignal(Direction side) {
        // Do not power the casing behind the hatch and bypass the player's external wiring.
        return 0;
    }

    @Override
    public boolean canConnectRedstone(Direction side) {
        // Forge's wire connection query uses the actual side, unlike the weak-power query.
        return side == getFrontFacing();
    }

    @Override
    public boolean canShared() {
        return false;
    }

    private void addSignalText(List<Component> text) {
        text.add(Component.translatable("gtecore.water.thermal_signal.signal", signal));
        if (!isFormed()) {
            text.add(Component.translatable("gtecore.water.thermal_signal.disconnected")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            text.add(Component.translatable(signal > 0 ? "gtecore.water.thermal_signal.heating" :
                    "gtecore.water.thermal_signal.cooling")
                    .withStyle(signal > 0 ? ChatFormatting.RED : ChatFormatting.AQUA));
        }
        text.add(Component.translatable("gtecore.water.thermal_signal.help"));
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 170, 120);
        group.setBackground(GuiTextures.DISPLAY);
        group.addWidget(new ComponentPanelWidget(6, 6, this::addSignalText)
                .textSupplier(isRemote() ? null : this::addSignalText)
                .setMaxWidthLimit(158));
        return group;
    }
}
