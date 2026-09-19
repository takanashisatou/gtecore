package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Redstone input for the first purification stage: powered heats, unpowered cools. */
public class ThermalControlHatchPartMachine extends MultiblockPartMachine {

    public ThermalControlHatchPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public int getRedstoneStrength() {
        return getLevel() == null ? 0 : getLevel().getBestNeighborSignal(getPos());
    }

    public boolean isHeating() {
        return getRedstoneStrength() > 0;
    }

    @Override
    public boolean canConnectRedstone(Direction side) {
        return true;
    }

    @Override
    public boolean canShared() {
        return false;
    }

    private void addControlText(List<Component> text) {
        int strength = getRedstoneStrength();
        text.add(Component.translatable("gtecore.water.thermal_hatch.signal", strength));
        text.add(Component.translatable(strength > 0 ? "gtecore.water.thermal_hatch.heating" :
                "gtecore.water.thermal_hatch.cooling").withStyle(strength > 0 ? ChatFormatting.RED : ChatFormatting.AQUA));
        text.add(Component.translatable("gtecore.water.thermal_hatch.help"));
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 170, 100);
        group.setBackground(GuiTextures.DISPLAY);
        group.addWidget(new ComponentPanelWidget(6, 6, this::addControlText)
                        .textSupplier(isRemote() ? null : this::addControlText)
                        .setMaxWidthLimit(158));
        return group;
    }
}
