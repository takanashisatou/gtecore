package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import net.minecraft.world.level.block.state.BlockState;

/** Exposes the hatch as a comparator source; weak redstone still leaves only its front face. */
public class EdiLoadSignalBlock extends MetaMachineBlock {
    public EdiLoadSignalBlock(Properties properties, MachineDefinition definition) {
        super(properties, definition);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
}
