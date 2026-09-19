package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationParts;

import java.util.ArrayList;
import java.util.List;

/** Exercises the registered preview and actual pattern, without injecting structure members. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class UvPurificationStructureGameTest {

    @GameTest(template = "thermal_lab", batch = "waterPurificationUv", required = true)
    public static void previewFormsWithFourOrOneLampButRejectsZero(GameTestHelper helper) {
        var definition = GTEWaterPurificationMachines.T2_UV_OXIDATION_PURIFICATION_UNIT;
        var shape = definition.getMatchingShapes().getFirst().getBlocks();
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos controllerPos = null;
        List<BlockPos> lamps = new ArrayList<>();
        for (int x = 0; x < shape.length; x++) {
            for (int y = 0; y < shape[x].length; y++) {
                for (int z = 0; z < shape[x][y].length; z++) {
                    var state = shape[x][y][z].getBlockState();
                    if (state.hasProperty(BlockStateProperties.FACING)) {
                        state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
                    }
                    if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                    }
                    BlockPos pos = anchor.offset(x, y, z);
                    level.setBlock(pos, state, 2);
                    if (state.is(definition.getBlock())) controllerPos = pos;
                    if (state.is(GTEWaterPurificationParts.UV_LAMP_HATCH.getBlock())) lamps.add(pos);
                }
            }
        }
        helper.assertTrue(lamps.size() == 4, "Registered T2 preview must contain exactly four lamp hatches");
        helper.assertTrue(controllerPos != null, "Registered T2 preview must contain its controller");
        var machine = MetaMachine.getMachine(level, controllerPos);
        helper.assertTrue(machine instanceof UvPurificationUnitMachine, "Preview must place the registered UV controller");
        var unit = (UvPurificationUnitMachine) machine;
        form(helper, unit, 4);

        unit.onStructureInvalid();
        for (int i = 1; i < lamps.size(); i++) {
            level.setBlock(lamps.get(i), GTBlocks.CASING_PTFE_INERT.getDefaultState(), 2);
        }
        form(helper, unit, 1);

        unit.onStructureInvalid();
        level.setBlock(lamps.getFirst(), GTBlocks.CASING_PTFE_INERT.getDefaultState(), 2);
        helper.assertTrue(!unit.checkPatternWithLock(), "T2 structure must reject zero lamps even with all slots cased");
        helper.succeed();
    }

    private static void form(GameTestHelper helper, UvPurificationUnitMachine unit, int expectedLamps) {
        helper.assertTrue(unit.checkPatternWithLock(), "Real T2 structure must match with " + expectedLamps + " lamps");
        unit.setFlipped(unit.getMultiblockState().isNeededFlip());
        unit.onStructureFormed();
        helper.assertTrue(unit.isFormed() && unit.getLampCount() == expectedLamps,
                "Formed T2 must discover exactly " + expectedLamps + " actual lamp parts");
    }
}
