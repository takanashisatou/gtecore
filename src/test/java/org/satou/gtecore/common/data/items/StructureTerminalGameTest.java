package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.data.items.StructureTestingTerminalBehavior.HighlightKind;
import org.satou.gtecore.common.data.items.StructureTestingTerminalBehavior.HighlightTarget;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class StructureTerminalGameTest {

    @GameTest(template = "empty", batch = "structureTerminal", required = true)
    public static void terminalDiagnosesT1WithoutChangingLiveState(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
        // The empty template only clears its own bounds; the real T1 extends beyond it.
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-10, -2, -10), pos.offset(10, 10, 10))) {
            helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.getLevel().setBlockAndUpdate(pos, GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT.getBlock().defaultBlockState());
        var controller = (IMultiController) MetaMachine.getMachine(helper.getLevel(), pos);
        controller.self().setFrontFacing(Direction.EAST);
        var live = controller.getMultiblockState();
        live.clean();
        live.getMatchContext().set("terminal_sentinel", "keep");
        live.addPosCache(pos);
        var cache = live.cache;
        var error = live.error;
        var pattern = controller.getPattern();
        int[] repetitions = pattern.getFormedRepetitionCount().clone();
        var result = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!result.matches(), "An isolated T1 controller must fail structure detection");
        helper.assertTrue(result.position() != null && !result.position().equals(pos), "Failure must point to a structure block, not the controller");
        // Isolated 5x7x5 T1 has > 64 missing blocks, so highlights must be capped at MAX_HIGHLIGHTS with primary included.
        helper.assertTrue(result.totalIssueCount() > StructureTestingTerminalBehavior.MAX_HIGHLIGHTS,
                "Isolated T1 should report more than " + StructureTestingTerminalBehavior.MAX_HIGHLIGHTS
                        + " issues, got " + result.totalIssueCount());
        helper.assertTrue(result.highlights().size() == StructureTestingTerminalBehavior.MAX_HIGHLIGHTS,
                "Highlights must be capped at " + StructureTestingTerminalBehavior.MAX_HIGHLIGHTS
                        + ", got " + result.highlights().size());
        helper.assertTrue(result.highlights().stream().anyMatch(h -> h.primary() && h.pos().equals(result.position())),
                "Primary error position must be highlighted and marked primary");
        helper.assertTrue(result.highlights().stream().allMatch(h -> h.kind() == HighlightKind.MISSING),
                "Isolated T1 in air should classify all missing non-air blocks as MISSING");
        helper.assertTrue("keep".equals(live.getMatchContext().get("terminal_sentinel")), "Terminal cleared live match context");
        helper.assertTrue(live.cache == cache && live.cache.contains(pos.asLong()) && live.error == error,
                "Terminal changed the live cache or error");
        helper.assertTrue(Arrays.equals(repetitions, pattern.getFormedRepetitionCount()), "Terminal changed pattern repetitions");
        helper.assertTrue(!controller.isFormed(), "Read-only detection must not form the controller");

        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, GTEItems.CHECK_STRUCTURE_TERMINAL.asStack());
        player.setShiftKeyDown(true);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var behavior = new StructureTestingTerminalBehavior();
        helper.assertTrue(behavior.onItemUseFirst(player.getMainHandItem(), new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "Sneak inspection must consume interaction before controller UI (and safely handle FakePlayer network sends)");
        // Sneak-right-clicking air must safely consume without touching client classes or throwing on FakePlayer.
        helper.assertTrue(behavior.use(GTEItems.CHECK_STRUCTURE_TERMINAL.get(), helper.getLevel(), player, InteractionHand.MAIN_HAND)
                .getResult().consumesAction(),
                "Sneak-right-clicking air must consume action to clear highlights");
        player.setShiftKeyDown(false);
        helper.assertTrue(behavior.onItemUseFirst(player.getMainHandItem(), new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "Preview server interaction must consume without loading the client renderer");
        BlockPos ordinary = pos.above();
        helper.getLevel().setBlockAndUpdate(ordinary, Blocks.STONE.defaultBlockState());
        hit = new BlockHitResult(Vec3.atCenterOf(ordinary), Direction.UP, ordinary, false);
        helper.assertTrue(behavior.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.PASS,
                "Non-controller interactions must pass through");
        helper.getLevel().removeBlock(ordinary, false);
        GameType previousMode = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.CREATIVE);
        try {
            pattern.autoBuild(player, new MultiblockState(helper.getLevel(), pos));
        } finally {
            player.setGameMode(previousMode);
        }
        live.getMatchContext().set("terminal_sentinel", "complete");
        var complete = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(complete.matches(), "Complete east-facing T1 must pass: " + complete.detail().getString() +
                " at " + complete.position() + " candidates " + complete.candidates().getString() +
                " actual " + (complete.position() == null ? "none" : helper.getLevel().getBlockState(complete.position())));
        helper.assertTrue(complete.highlights().isEmpty() && complete.totalIssueCount() == 0,
                "Complete structure must produce no highlights");
        helper.assertTrue("complete".equals(live.getMatchContext().get("terminal_sentinel")), "Successful diagnosis changed live context");
        helper.assertTrue(controller.checkPatternWithLock(), "The real T1 matcher must also accept the built structure");
        controller.onStructureFormed();
        var parts = java.util.List.copyOf(controller.getParts());
        live.getMatchContext().set("terminal_sentinel", "formed");
        helper.assertTrue(StructureTestingTerminalBehavior.diagnose(controller).matches() && controller.isFormed(),
                "Checking a formed machine must preserve formation");
        helper.assertTrue(parts.equals(controller.getParts()) && "formed".equals(live.getMatchContext().get("terminal_sentinel")),
                "Checking a running controller must preserve parts and live context");

        // Introduce BOTH a wrong block (bedrock) and a missing block (air) simultaneously.
        BlockPos wrongBlockPos = result.position();
        BlockPos missingBlockPos = result.highlights().stream()
                .map(HighlightTarget::pos)
                .filter(p -> !p.equals(wrongBlockPos) && !helper.getLevel().getBlockState(p).isAir())
                .findFirst()
                .orElseThrow();
        helper.getLevel().setBlockAndUpdate(wrongBlockPos, Blocks.BEDROCK.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(missingBlockPos, Blocks.AIR.defaultBlockState());
        live.getMatchContext().set("terminal_sentinel", "damaged");
        var damaged = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!damaged.matches() && wrongBlockPos.equals(damaged.position()),
                "Damaged T1 must point to the actual replaced block");
        helper.assertTrue(damaged.highlights().size() >= 2,
                "Multi-position scan must capture both wrong and missing blocks, got " + damaged.highlights().size());
        helper.assertTrue(damaged.highlights().stream().anyMatch(
                h -> h.pos().equals(wrongBlockPos) && h.kind() == HighlightKind.WRONG && h.primary()),
                "Replaced bedrock block must be highlighted as primary WRONG");
        helper.assertTrue(damaged.highlights().stream().anyMatch(
                h -> h.pos().equals(missingBlockPos) && h.kind() == HighlightKind.MISSING && !h.primary()),
                "Removed block must be highlighted as secondary MISSING");
        helper.assertTrue("damaged".equals(live.getMatchContext().get("terminal_sentinel")), "Repeated diagnostics changed live context");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structureTerminalVariable", required = true)
    public static void terminalScansVariableLengthDistillationTowerSafely(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-5, -2, -5), pos.offset(5, 16, 5))) {
            helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
        }

        // Build a 5-layer tall Distillation Tower (base y=0, Z-ring y=1, 2 repeatable X-rings y=2..3, roof y=4).
        // Front facing NORTH -> RIGHT = WEST (-X), BACK = SOUTH (+Z), UP = +Y.
        // Controller at (0, 0, 0), base spans x in [-1, 1], z in [0, 2].
        helper.getLevel().setBlockAndUpdate(pos, GTMultiMachines.DISTILLATION_TOWER.getBlock().defaultBlockState());
        var controller = (IMultiController) MetaMachine.getMachine(helper.getLevel(), pos);
        controller.self().setFrontFacing(Direction.NORTH);

        var casing = GTBlocks.CASING_STAINLESS_CLEAN.getDefaultState();
        for (int dy = 0; dy <= 4; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 0; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    boolean hollow = (dy >= 1 && dy <= 3) && dx == 0 && dz == 1;
                    if (!hollow) {
                        helper.getLevel().setBlockAndUpdate(pos.offset(dx, dy, dz), casing);
                    }
                }
            }
        }
        // Required base hatches: 1 energy input, 1 fluid import, 1 maintenance.
        helper.getLevel().setBlockAndUpdate(pos.offset(-1, 0, 2), GTMachines.ENERGY_INPUT_HATCH[GTValues.HV].getBlock().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.offset(1, 0, 2), GTMachines.FLUID_IMPORT_HATCH[GTValues.HV].getBlock().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.offset(0, 0, 2), GTMachines.MAINTENANCE_HATCH.getBlock().defaultBlockState());

        var pattern = controller.getPattern();
        int[] savedBefore = pattern.getFormedRepetitionCount().clone();

        var healthy = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(healthy.matches(), "Complete 5-tall Distillation Tower must pass: " + healthy.detail().getString()
                + " at " + healthy.position());
        helper.assertTrue(Arrays.equals(savedBefore, pattern.getFormedRepetitionCount()),
                "Diagnosing variable-length tower must restore formedRepetitionCount");

        // Break one casing on repeatable ring y=2 (MISSING) and place bedrock in hollow center y=1 (WRONG).
        BlockPos missingRingPos = pos.offset(1, 2, 1);
        BlockPos wrongCenterPos = pos.offset(0, 1, 1);
        helper.getLevel().setBlockAndUpdate(missingRingPos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(wrongCenterPos, Blocks.BEDROCK.defaultBlockState());

        var damaged = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!damaged.matches(), "Damaged variable-length tower must fail");
        helper.assertTrue(Arrays.equals(savedBefore, pattern.getFormedRepetitionCount()),
                "Failed diagnosis on variable-length tower must still restore formedRepetitionCount");

        Set<BlockPos> highlightedPositions = new HashSet<>();
        for (HighlightTarget target : damaged.highlights()) {
            highlightedPositions.add(target.pos());
        }
        helper.assertTrue(highlightedPositions.contains(wrongCenterPos),
                "Expected blocked hollow center " + wrongCenterPos + " in highlights, got " + highlightedPositions);
        helper.assertTrue(highlightedPositions.contains(missingRingPos),
                "Expected missing repeatable ring block " + missingRingPos + " in highlights, got " + highlightedPositions);
        helper.assertTrue(damaged.highlights().stream().anyMatch(
                h -> h.pos().equals(wrongCenterPos) && h.kind() == HighlightKind.WRONG),
                "Unexpected block in air slot must be classified WRONG");
        helper.assertTrue(damaged.highlights().stream().anyMatch(
                h -> h.pos().equals(missingRingPos) && h.kind() == HighlightKind.MISSING),
                "Missing casing in repeatable ring must be classified MISSING");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structureTerminalSentinels", required = true)
    public static void unloadedAndUninitializedDiagnosticsAreSafe(GameTestHelper helper) {
        var state = new MultiblockState(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(!StructureTestingTerminalBehavior.describeFailure(state).matches(), "Uninitialized state must not pass");
        state.error = MultiblockState.UNLOAD_ERROR;
        var result = StructureTestingTerminalBehavior.describeFailure(state);
        helper.assertTrue(!result.matches() && result.position() == null, "Unloaded sentinel must report without accessing absent world state");
        helper.succeed();
    }
}
