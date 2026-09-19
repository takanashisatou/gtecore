package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;

import java.util.Arrays;

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
                "Sneak inspection must consume interaction before controller UI");
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
        helper.assertTrue("complete".equals(live.getMatchContext().get("terminal_sentinel")), "Successful diagnosis changed live context");
        helper.assertTrue(controller.checkPatternWithLock(), "The real T1 matcher must also accept the built structure");
        controller.onStructureFormed();
        var parts = java.util.List.copyOf(controller.getParts());
        live.getMatchContext().set("terminal_sentinel", "formed");
        helper.assertTrue(StructureTestingTerminalBehavior.diagnose(controller).matches() && controller.isFormed(),
                "Checking a formed machine must preserve formation");
        helper.assertTrue(parts.equals(controller.getParts()) && "formed".equals(live.getMatchContext().get("terminal_sentinel")),
                "Checking a running controller must preserve parts and live context");
        BlockPos broken = result.position();
        helper.getLevel().setBlockAndUpdate(broken, Blocks.BEDROCK.defaultBlockState());
        live.getMatchContext().set("terminal_sentinel", "damaged");
        var damaged = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!damaged.matches() && broken.equals(damaged.position()), "Damaged T1 must point to the actual replaced block");
        helper.assertTrue("damaged".equals(live.getMatchContext().get("terminal_sentinel")), "Repeated diagnostics changed live context");
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
