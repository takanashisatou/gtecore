package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationParts;

/** Real structure and sensor discovery; no injected member lists. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class EdiPurificationStructureGameTest {
    @GameTest(template = "thermal_lab", batch = "ediStructure", required = true)
    public static void previewRequiresBothPortsAndPreservesLoadWhenBroken(GameTestHelper helper) {
        var definition = GTEWaterPurificationMachines.T3_EDI_ULTRAPURE_PURIFICATION_UNIT;
        var shape = definition.getMatchingShapes().getFirst().getBlocks();
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos controller = null, sensorPos = null, controlPos = null;
        int sensors = 0, controls = 0;
        for (int x = 0; x < shape.length; x++) for (int y = 0; y < shape[x].length; y++) {
            for (int z = 0; z < shape[x][y].length; z++) {
                var state = shape[x][y][z].getBlockState();
                if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
                if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                var pos = anchor.offset(x, y, z);
                level.setBlock(pos, state, 2);
                if (state.is(definition.getBlock())) controller = pos;
                if (state.is(GTEWaterPurificationParts.EDI_LOAD_SIGNAL_HATCH.getBlock())) { sensorPos = pos; sensors++; }
                if (state.is(GTEWaterPurificationParts.EDI_REGENERATION_CONTROL_HATCH.getBlock())) { controlPos = pos; controls++; }
            }
        }
        helper.assertTrue(controller != null && sensors == 1 && controls == 1,
                "EDI preview must contain one controller and exactly one of each redstone port");
        var unit = (EdiPurificationUnitMachine) MetaMachine.getMachine(level, controller);
        form(helper, unit);
        unit.getEdiState().produced(960000);
        var sensor = (EdiLoadSignalHatchPartMachine) MetaMachine.getMachine(level, sensorPos);
        sensor.refreshSignal();
        helper.assertTrue(sensor.getOutputSignal(Direction.SOUTH) == 12 && sensor.getOutputSignal(Direction.NORTH) == 0 &&
                sensor.getOutputDirectSignal(Direction.SOUTH) == 0,
                "The formed sensor must report raw load only on its front, without strongly powering casings");

        unit.onStructureInvalid();
        var controlState = level.getBlockState(controlPos);
        level.setBlock(controlPos, GCYMBlocks.CASING_WATERTIGHT.getDefaultState(), 2);
        helper.assertTrue(!unit.checkPatternWithLock() && unit.getEdiState().load() == 960000,
                "Removing the control port must invalidate the structure without washing its load");
        level.setBlock(controlPos, controlState, 2);
        form(helper, unit);
        unit.onStructureInvalid();
        level.setBlock(sensorPos, GCYMBlocks.CASING_WATERTIGHT.getDefaultState(), 2);
        helper.assertTrue(!unit.checkPatternWithLock() && unit.getEdiState().load() == 960000,
                "Removing the sensor must invalidate the structure without washing its load");
        helper.succeed();
    }

    private static void form(GameTestHelper helper, EdiPurificationUnitMachine unit) {
        helper.assertTrue(unit.checkPatternWithLock(), "Registered EDI preview must form in the real world");
        unit.setFlipped(unit.getMultiblockState().isNeededFlip());
        unit.onStructureFormed();
        helper.assertTrue(unit.isFormed(), "EDI controller did not become formed");
    }
}
