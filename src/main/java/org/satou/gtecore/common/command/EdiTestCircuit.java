package org.satou.gtecore.common.command;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

import java.util.Map;

/** Physical, player-editable threshold circuit for the command's north-facing EDI unit. */
final class EdiTestCircuit {
    private EdiTestCircuit() {}

    record Layout(BlockPos disconnectPoint, Map<BlockPos, Integer> referenceBarrels) {}

    /**
     * The board is ten blocks south of the unit's preview origin. Its analogue input is
     * copied through comparators, never through attenuating wire or digital repeaters.
     * The two thresholds and cross-coupled torch latch follow EdiRedstoneGameTest.
     */
    static Layout plan(Map<BlockPos, BlockState> blocks, BlockPos signal, BlockPos control,
                       BlockPos boardOrigin) {
        if (signal.getY() != boardOrigin.getY() + 1 || signal.getZ() != boardOrigin.getZ() - 4 ||
                control.getY() != boardOrigin.getY() + 1 || control.getZ() != boardOrigin.getZ() - 10 ||
                signal.getX() < boardOrigin.getX() || signal.getX() > boardOrigin.getX() + 6 ||
                control.getX() < boardOrigin.getX() || control.getX() > boardOrigin.getX() + 6) {
            throw new IllegalArgumentException("Unexpected EDI hatch positions: signal=" + signal +
                    ", control=" + control + ", board=" + boardOrigin);
        }
        // NORTH previews normalize LEFT and FRONT axes, mirroring pattern X and Z.
        // Align the board to the actual sensor, but keep the return outside the unit.
        BlockPos circuitOrigin = new BlockPos(signal.getX() - 2, boardOrigin.getY(), boardOrigin.getZ());
        int westReturnX = boardOrigin.getX() - 2 - circuitOrigin.getX();
        int controlX = control.getX() - circuitOrigin.getX();
        var board = new Board(blocks, circuitOrigin);
        // Comparator output strongly powers this solid analogue fan-out block. Both
        // threshold comparators read its unattenuated input, including exact 12 and 4.
        for (int z = -3; z <= 3; z++) board.comparator(2, z, Direction.NORTH);
        board.block(2, 1, 4, Blocks.STONE.defaultBlockState());
        board.comparator(3, 4, Direction.WEST);
        board.comparator(3, 3, Direction.NORTH);
        board.block(3, 1, 2, Blocks.BARREL.defaultBlockState());
        board.comparator(2, 5, Direction.NORTH);
        board.comparator(1, 5, Direction.WEST);
        board.block(0, 1, 5, Blocks.BARREL.defaultBlockState());
        board.dust(4, 1, 4);
        for (int x = 4; x <= 9; x++) board.dust(x, 1, 3);
        board.repeater(10, 1, 3, Direction.WEST);

        board.block(2, 1, 6, Blocks.STONE.defaultBlockState());
        board.block(2, 1, 7, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
                .setValue(RedstoneWallTorchBlock.FACING, Direction.SOUTH));
        for (int x = 2; x <= 17; x++) {
            if (x == 9) board.repeater(x, 1, 8, Direction.WEST);
            else board.dust(x, 1, 8);
        }
        board.dust(17, 1, 7);
        board.dust(17, 1, 6);
        board.repeater(16, 1, 6, Direction.EAST);

        board.block(11, 1, 3, Blocks.STONE.defaultBlockState());
        board.block(15, 1, 6, Blocks.STONE.defaultBlockState());
        board.block(11, 2, 3, Blocks.REDSTONE_TORCH.defaultBlockState());
        board.block(15, 2, 6, Blocks.REDSTONE_TORCH.defaultBlockState());
        for (int x = 12; x <= 15; x++) board.dust(x, 2, 3);
        board.dust(15, 2, 4);
        board.dust(15, 1, 5);
        for (int x = 11; x <= 14; x++) board.dust(x, 2, 6);
        board.dust(11, 2, 5);
        board.dust(11, 1, 4);

        // Take Q above its torch, then bridge over the reset branch before returning
        // outside the west wall. This leaves every block of the real 7x7x7 unit intact.
        board.block(15, 3, 6, Blocks.STONE.defaultBlockState());
        board.repeater(15, 3, 7, Direction.NORTH);
        for (int z = 8; z <= 10; z++) board.dust(15, 3, z);
        for (int x = 14; x >= westReturnX; x--) {
            if (x == 9 || x == 1) board.repeater(x, 3, 10, Direction.EAST);
            else board.dust(x, 3, 10);
        }
        for (int z = 9; z >= -13; z--) {
            if (z == 7 || z == -1 || z == -9) board.repeater(westReturnX, 3, z, Direction.SOUTH);
            else board.dust(westReturnX, 3, z);
        }
        for (int x = westReturnX + 1; x <= controlX; x++) {
            if (x == controlX) {
                // The lower stair must see a conducting step to read dust above it.
                // Only the bridge over RESET needs insulating glass supports.
                board.block(x, 2, -13, Blocks.STONE.defaultBlockState());
                board.block(x, 3, -13, Blocks.REDSTONE_WIRE.defaultBlockState());
            } else if (x == controlX - 2) board.repeater(x, 3, -13, Direction.WEST);
            else board.dust(x, 3, -13);
        }
        board.dust(controlX, 2, -12);
        board.dust(controlX, 1, -11);
        return new Layout(circuitOrigin.offset(15, 3, 7), Map.of(
                circuitOrigin.offset(3, 1, 2), 22, circuitOrigin.offset(0, 1, 5), 6));
    }

    static void configure(ServerLevel level, Layout layout) {
        for (var entry : layout.referenceBarrels().entrySet()) {
            if (!(level.getBlockEntity(entry.getKey()) instanceof BarrelBlockEntity barrel)) {
                throw new IllegalStateException("Missing EDI reference barrel at " + entry.getKey());
            }
            barrel.clearContent();
            for (int slot = 0; slot < entry.getValue(); slot++) {
                barrel.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            barrel.setChanged();
            level.updateNeighbourForOutputSignal(entry.getKey(), Blocks.BARREL);
        }
    }

    private record Board(Map<BlockPos, BlockState> blocks, BlockPos origin) {
        private void block(int x, int y, int z, BlockState state) {
            BlockPos pos = origin.offset(x, y, z);
            BlockState previous = blocks.get(pos);
            if (previous != null && !previous.isAir() && !previous.equals(state)) {
                throw new IllegalArgumentException("EDI circuit overlaps another planned block at " + pos);
            }
            blocks.put(pos, state);
        }

        private void supported(int x, int y, int z, BlockState state) {
            // Glass keeps the elevated Q bridge from powering RESET dust below it.
            block(x, y - 1, z, (y >= 3 ? Blocks.GLASS : Blocks.STONE).defaultBlockState());
            block(x, y, z, state);
        }

        private void dust(int x, int y, int z) {
            supported(x, y, z, Blocks.REDSTONE_WIRE.defaultBlockState());
        }

        private void repeater(int x, int y, int z, Direction input) {
            supported(x, y, z, Blocks.REPEATER.defaultBlockState().setValue(DiodeBlock.FACING, input));
        }

        private void comparator(int x, int z, Direction input) {
            supported(x, 1, z, Blocks.COMPARATOR.defaultBlockState()
                    .setValue(DiodeBlock.FACING, input).setValue(ComparatorBlock.MODE, ComparatorMode.COMPARE));
        }
    }
}
