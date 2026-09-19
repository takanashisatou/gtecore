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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Raw 0-15 membrane load. No threshold decisions or hidden automatic regeneration. */
public class EdiLoadSignalHatchPartMachine extends MultiblockPartMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            EdiLoadSignalHatchPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);
    @DescSynced private int signal;
    private @Nullable TickableSubscription subscription;

    public EdiLoadSignalHatchPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) subscription = subscribeServerTick(subscription, this::refreshSignal);
    }

    @Override
    public void onUnload() {
        if (subscription != null) subscription.unsubscribe();
        subscription = null;
        if (!isRemote()) setSignal(0);
        super.onUnload();
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        if (!isRemote()) setSignal(0);
    }

    public void refreshSignal() {
        for (var controller : getControllers()) {
            if (controller instanceof EdiPurificationUnitMachine unit && unit.isFormed() &&
                    getLevel().hasChunkAt(unit.getPos())) {
                setSignal(unit.getEdiState().signal());
                return;
            }
        }
        setSignal(0);
    }

    private void setSignal(int next) {
        if (signal == next) return;
        signal = next;
        updateSignal();
        getLevel().updateNeighbourForOutputSignal(getPos(), getBlockState().getBlock());
    }

    @Override
    public int getAnalogOutputSignal() {
        return signal;
    }

    @Override
    public int getOutputSignal(@Nullable Direction side) {
        return side == getFrontFacing().getOpposite() ? signal : 0;
    }

    @Override
    public int getOutputDirectSignal(Direction side) {
        return 0;
    }

    @Override
    public boolean canConnectRedstone(Direction side) {
        return side == getFrontFacing();
    }

    @Override
    public boolean canShared() {
        return false;
    }

    private void addText(List<Component> lines) {
        lines.add(Component.translatable("gtecore.water.edi.signal", signal));
        lines.add(Component.translatable("gtecore.water.edi.signal_hatch.help"));
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 190, 110);
        group.setBackground(GuiTextures.DISPLAY);
        group.addWidget(new ComponentPanelWidget(8, 8, this::addText)
                .textSupplier(isRemote() ? null : this::addText).setMaxWidthLimit(174));
        return group;
    }
}
