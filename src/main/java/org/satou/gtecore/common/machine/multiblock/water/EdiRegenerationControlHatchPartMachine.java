package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.List;

/** External redstone selects the next batch; the latch belongs to the player's circuit. */
public class EdiRegenerationControlHatchPartMachine extends MultiblockPartMachine {
    public EdiRegenerationControlHatchPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public int getRedstoneStrength() {
        return getLevel() == null ? 0 : getLevel().getBestNeighborSignal(getPos());
    }

    public boolean isRegenerationRequested() {
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

    private void addText(List<Component> lines) {
        lines.add(Component.translatable("gtecore.water.edi.control_hatch.signal", getRedstoneStrength()));
        lines.add(Component.translatable("gtecore.water.edi.control_hatch.help"));
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 180, 100);
        group.setBackground(GuiTextures.DISPLAY);
        group.addWidget(new ComponentPanelWidget(8, 8, this::addText)
                .textSupplier(isRemote() ? null : this::addText).setMaxWidthLimit(164));
        return group;
    }
}
