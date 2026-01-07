package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IFluidRenderMulti;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.GTECore;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ORE_PROCESS_CENTER_MACHINE extends WorkableElectricMultiblockMachine implements IFluidRenderMulti {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ORE_PROCESS_CENTER_MACHINE.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Setter
    @DescSynced
    @RequireRerender
    private @NotNull Set<BlockPos> fluidBlockOffsets = new HashSet<>();

    public ORE_PROCESS_CENTER_MACHINE(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        IFluidRenderMulti.super.onStructureFormed();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        IFluidRenderMulti.super.onStructureInvalid();
    }

    @NotNull
    @Override
    public Set<BlockPos> saveOffsets() {
        Direction up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction back = getFrontFacing().getOpposite();
        Direction clockWise = RelativeDirection.RIGHT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction counterClockWise = RelativeDirection.LEFT.getRelative(getFrontFacing(), getUpwardsFacing(),
                isFlipped());
        Direction down = RelativeDirection.DOWN.getRelative(getFrontFacing(), getUpwardsFacing(),isFlipped());

        BlockPos pos = getPos();
        BlockPos center = null;
        for (int i = 0; i < 6; i++) {
            if (i == 0) {
                center = pos.relative(counterClockWise);
                continue;
            }
            center = center.relative(counterClockWise);
        }

        Set<BlockPos> offsets = new HashSet<>();
        for (int i = 0; i < 1; i++) {
            //center = center.relative(down);
            BlockPos center2 = new BlockPos(center);
            for (int j = 0; j < 12; j++) {
                center2 = center2.relative(back);
                offsets.add(center2.subtract(pos));
                offsets.add(center2.relative(clockWise).subtract(pos));
                offsets.add(center2.relative(counterClockWise).subtract(pos));
            }

        }
        return offsets;
    }
}
