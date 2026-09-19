package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationParts;

/** Physical dual-threshold comparator circuit and cross-coupled torch RS latch. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class EdiRedstoneGameTest {
    private static final BlockPos SENSOR = new BlockPos(2, 1, 4);
    private static final BlockPos UNIT = new BlockPos(0, 1, 0);
    private static final BlockPos CONTROL = new BlockPos(15, 3, 6);

    private static void field(Object target, String name, Object value) throws Exception {
        var field = MultiblockControllerMachine.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void dust(GameTestHelper helper, int x, int y, int z) {
        helper.setBlock(new BlockPos(x, y - 1, z), Blocks.STONE);
        helper.setBlock(new BlockPos(x, y, z), Blocks.REDSTONE_WIRE);
    }

    private static void repeater(GameTestHelper helper, int x, int y, int z, Direction input) {
        helper.setBlock(new BlockPos(x, y - 1, z), Blocks.STONE);
        helper.setBlock(new BlockPos(x, y, z), Blocks.REPEATER.defaultBlockState()
                .setValue(DiodeBlock.FACING, input));
    }

    private static void comparator(GameTestHelper helper, int x, int z, Direction input) {
        helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(new BlockPos(x, 1, z), Blocks.COMPARATOR.defaultBlockState()
                .setValue(DiodeBlock.FACING, input).setValue(ComparatorBlock.MODE, ComparatorMode.COMPARE));
    }

    private static void reference(GameTestHelper helper, int x, int z, int fullSlots) {
        BlockPos relative = new BlockPos(x, 1, z);
        helper.setBlock(relative, Blocks.BARREL);
        var barrel = (BarrelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        for (int slot = 0; slot < fullSlots; slot++) barrel.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        barrel.setChanged();
        helper.getLevel().updateNeighbourForOutputSignal(helper.absolutePos(relative), Blocks.BARREL);
    }

    @GameTest(template = "thermal_lab", batch = "ediRedstone", timeoutTicks = 400, required = true)
    public static void rawLoadDrivesExternalHysteresisLatch(GameTestHelper helper) throws Exception {
        // thermal_lab is 19 x 10 x 10. Every block below is inside these owned bounds.
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 18, 9, 9)) {
            helper.setBlock(pos, Blocks.AIR);
        }
        helper.setBlock(UNIT, GTEWaterPurificationMachines.T3_EDI_ULTRAPURE_PURIFICATION_UNIT.getBlock());
        helper.setBlock(SENSOR, GTEWaterPurificationParts.EDI_LOAD_SIGNAL_HATCH.getBlock());
        helper.setBlock(CONTROL, GTEWaterPurificationParts.EDI_REGENERATION_CONTROL_HATCH.getBlock());
        var unit = (EdiPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(UNIT));
        var sensor = (EdiLoadSignalHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(SENSOR));
        var control = (EdiRegenerationControlHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(CONTROL));
        field(unit, "isFormed", true);
        // This fixture isolates physical redstone; no actual multiblock pattern is installed.
        // onLoad registers an asynchronous pattern checker, including queued BE initialization.
        var saved = MultiblockWorldSavedData.getOrCreate(helper.getLevel());
        saved.removeAsyncLogic(unit);
        helper.runAtTickTime(1, () -> saved.removeAsyncLogic(unit));
        field(unit, "partPositions", new BlockPos[]{helper.absolutePos(SENSOR), helper.absolutePos(CONTROL)});
        sensor.addedToController(unit);
        control.addedToController(unit);
        sensor.setFrontFacing(Direction.EAST);

        // Comparators read the hatch's raw analog value directly, without wire attenuation.
        comparator(helper, 3, 4, Direction.WEST); // SET: signal >= 12
        comparator(helper, 3, 3, Direction.NORTH);
        reference(helper, 3, 2, 22); // floor(14 * 22 / 27) + 1 = 12
        comparator(helper, 2, 5, Direction.NORTH); // signal >= 4; invert to RESET
        comparator(helper, 1, 5, Direction.WEST);
        reference(helper, 0, 5, 6); // floor(14 * 6 / 27) + 1 = 4
        dust(helper, 4, 1, 4);
        for (int x = 4; x <= 9; x++) dust(helper, x, 1, 3);
        repeater(helper, 10, 1, 3, Direction.WEST);

        // RESET inverter and isolated perimeter return; a repeater restores its long wire.
        helper.setBlock(new BlockPos(2, 1, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 7), Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
                .setValue(RedstoneWallTorchBlock.FACING, Direction.SOUTH));
        for (int x = 2; x <= 17; x++) dust(helper, x, 1, 8);
        repeater(helper, 9, 1, 8, Direction.WEST);
        dust(helper, 17, 1, 7);
        dust(helper, 17, 1, 6);
        repeater(helper, 16, 1, 6, Direction.EAST);

        // Two NOR gates: SET powers A, RESET powers B; B's torch is latched Q.
        helper.setBlock(new BlockPos(11, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(15, 1, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(11, 2, 3), Blocks.REDSTONE_TORCH);
        helper.setBlock(new BlockPos(15, 2, 6), Blocks.REDSTONE_TORCH);
        for (int x = 12; x <= 15; x++) dust(helper, x, 2, 3);
        dust(helper, 15, 2, 4);
        dust(helper, 15, 1, 5);
        for (int x = 11; x <= 14; x++) dust(helper, x, 2, 6);
        dust(helper, 11, 2, 5);
        dust(helper, 11, 1, 4);

        unit.getEdiState().restore(new PurificationEdiState.Snapshot(240_000));
        helper.runAtTickTime(70, () -> {
            helper.assertTrue(!control.isRegenerationRequested(), "Initial low load must reset the physical latch");
            unit.getEdiState().restore(new PurificationEdiState.Snapshot(960_000));
        });
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(sensor.getAnalogOutputSignal() == 12 && control.isRegenerationRequested(),
                    "Signal 12 must set regeneration through the comparator and torch latch");
            unit.getEdiState().restore(new PurificationEdiState.Snapshot(640_000));
        });
        helper.runAtTickTime(210, () -> {
            helper.assertTrue(sensor.getAnalogOutputSignal() == 8 && control.isRegenerationRequested(),
                    "Signal 8 must retain the previously set state in the external latch");
            unit.getEdiState().restore(new PurificationEdiState.Snapshot(240_000));
        });
        helper.runAtTickTime(280, () -> {
            helper.assertTrue(sensor.getAnalogOutputSignal() == 3 && !control.isRegenerationRequested(),
                    "Signal 3 must reset regeneration through reference-4 inversion");
            unit.getEdiState().restore(new PurificationEdiState.Snapshot(640_000));
        });
        helper.runAtTickTime(350, () -> {
            helper.assertTrue(sensor.getAnalogOutputSignal() == 8 && !control.isRegenerationRequested(),
                    "Signal 8 must retain the reset state without hidden controller hysteresis");
            helper.succeed();
        });
    }
}
