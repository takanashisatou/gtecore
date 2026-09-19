package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.chat.Component;

import java.util.List;

/** One physical lamp bank. Power is supplied through the controller's central-plant link. */
public class UvLampHatchPartMachine extends MultiblockPartMachine {

    public UvLampHatchPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public boolean canShared() {
        return false;
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 180, 96);
        group.setBackground(GuiTextures.DISPLAY);
        group.addWidget(new ComponentPanelWidget(8, 8, List.of(
                Component.translatable("gtecore.water.uv.lamp.title"),
                Component.translatable("gtecore.water.uv.lamp.help"))).setMaxWidthLimit(164));
        return group;
    }
}
