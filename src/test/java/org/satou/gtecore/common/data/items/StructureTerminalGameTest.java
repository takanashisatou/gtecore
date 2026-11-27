package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IObjectHolder;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class StructureTerminalGameTest {

    @GameTest(template = "empty", batch = "structureTerminal", required = true)
    public static void terminalFormsAndDiagnosesT1Structure(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
        // The empty template only clears its own bounds; the real T1 extends beyond it.
        clearRegion(helper, pos.offset(-10, -2, -10), pos.offset(10, 10, 10));
        helper.getLevel().setBlockAndUpdate(pos, GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT.getBlock().defaultBlockState());
        var controller = (IMultiController) MetaMachine.getMachine(helper.getLevel(), pos);
        controller.self().setFrontFacing(Direction.EAST);

        // 1. Isolated controller must stay unformed and emit capped multi-position MISSING highlights with primary first.
        var isolated = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!isolated.matches(), "An isolated T1 controller must fail structure detection");
        helper.assertTrue(!controller.isFormed(), "An isolated T1 controller must stay unformed");
        helper.assertTrue(isolated.position() != null && !isolated.position().equals(pos),
                "Failure must point to a structure block, not the controller");
        helper.assertTrue(isolated.totalIssueCount() > StructureTestingTerminalBehavior.MAX_HIGHLIGHTS,
                "Isolated T1 should report more than " + StructureTestingTerminalBehavior.MAX_HIGHLIGHTS
                        + " issues, got " + isolated.totalIssueCount());
        helper.assertTrue(isolated.highlights().size() == StructureTestingTerminalBehavior.MAX_HIGHLIGHTS,
                "Highlights must be capped at " + StructureTestingTerminalBehavior.MAX_HIGHLIGHTS
                        + ", got " + isolated.highlights().size());
        helper.assertTrue(isolated.highlights().get(0).primary()
                        && isolated.highlights().get(0).pos().equals(isolated.position()),
                "Primary error position must be highlighted first and marked primary");
        helper.assertTrue(isolated.highlights().stream().allMatch(h -> h.kind() == HighlightKind.MISSING),
                "Isolated T1 in air should classify all missing non-air blocks as MISSING");

        // 2. FakePlayer interaction safety (sneak-check, sneak-air-clear, preview, non-controller pass).
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, GTEItems.CHECK_STRUCTURE_TERMINAL.asStack());
        player.setShiftKeyDown(true);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var behavior = new StructureTestingTerminalBehavior();
        helper.assertTrue(behavior.onItemUseFirst(player.getMainHandItem(), new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "Sneak inspection must consume interaction before controller UI (and safely handle FakePlayer network sends)");
        helper.assertTrue(!controller.isFormed(), "Sneak-checking an incomplete T1 must leave it unformed");
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

        // 3. Freshly completed valid T1 forms immediately via terminal and reports formed with no highlights.
        creativeAutoBuild(helper, player, controller, pos);
        var complete = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(complete.matches(), "Complete east-facing T1 must form and pass: "
                + complete.detail().getString() + " at " + complete.position());
        helper.assertTrue(controller.isFormed(), "Complete T1 must actually be formed after terminal check");
        helper.assertTrue(complete.highlights().isEmpty() && complete.totalIssueCount() == 0,
                "Formed structure must produce no highlights");

        // 4. Rechecking an already-formed controller preserves formation and attached parts.
        var parts = List.copyOf(controller.getParts());
        var rechecked = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(rechecked.matches() && controller.isFormed(),
                "Checking an already-formed controller must report formed and preserve formation");
        helper.assertTrue(parts.equals(controller.getParts()),
                "Checking an already-formed controller must preserve attached parts");

        // 5. Damaging the formed structure invalidates formation and highlights both WRONG and MISSING blocks.
        BlockPos wrongBlockPos = isolated.position();
        BlockPos missingBlockPos = isolated.highlights().stream()
                .map(HighlightTarget::pos)
                .filter(p -> !p.equals(wrongBlockPos) && !helper.getLevel().getBlockState(p).isAir())
                .findFirst()
                .orElseThrow();
        helper.getLevel().setBlockAndUpdate(wrongBlockPos, Blocks.BEDROCK.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(missingBlockPos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(!controller.isFormed(), "Breaking formed T1 blocks must invalidate formation via world mapping");

        var damaged = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!damaged.matches() && !controller.isFormed(),
                "Damaged T1 must stay unformed and report failure");
        helper.assertTrue(wrongBlockPos.equals(damaged.position()),
                "Damaged T1 must point to the actual replaced block");
        helper.assertTrue(damaged.highlights().size() >= 2,
                "Multi-position scan must capture both wrong and missing blocks, got " + damaged.highlights().size());
        helper.assertTrue(damaged.highlights().get(0).pos().equals(wrongBlockPos)
                        && damaged.highlights().get(0).kind() == HighlightKind.WRONG
                        && damaged.highlights().get(0).primary(),
                "Replaced bedrock block must be highlighted first as primary WRONG");
        helper.assertTrue(damaged.highlights().stream().anyMatch(
                h -> h.pos().equals(missingBlockPos) && h.kind() == HighlightKind.MISSING && !h.primary()),
                "Removed block must be highlighted as secondary MISSING");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structureTerminalVariable", required = true)
    public static void terminalHandlesVariableLengthDistillationTower(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
        clearRegion(helper, pos.offset(-5, -2, -5), pos.offset(5, 16, 5));

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
        // Base hatches: energy input, fluid import, maintenance — intentionally 0 fluid export hatches first.
        helper.getLevel().setBlockAndUpdate(pos.offset(-1, 0, 2), GTMachines.ENERGY_INPUT_HATCH[GTValues.HV].getBlock().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.offset(1, 0, 2), GTMachines.FLUID_IMPORT_HATCH[GTValues.HV].getBlock().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.offset(0, 0, 2), GTMachines.MAINTENANCE_HATCH.getBlock().defaultBlockState());

        // (b) 0 fluid export hatches: BlockPattern matches, but DistillationTowerMachine.onStructureFormed rejects it.
        var noOutput = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!controller.isFormed(), "Distillation Tower with 0 fluid export hatches must stay unformed");
        helper.assertTrue(!noOutput.matches(), "Terminal must report failure for Distillation Tower with 0 fluid export hatches");
        helper.assertTrue(Component.translatable("gtecore.structure_terminal.reject.distillation_no_output").equals(noOutput.detail()),
                "Expected missing fluid output hatch reason, got: " + noOutput.detail().getString());
        helper.assertTrue(noOutput.highlights().isEmpty(),
                "Missing fluid output hatch across tower should not emit bogus block highlights");

        // (a) Place a fluid export hatch on layer y=1: Distillation Tower now forms cleanly.
        helper.getLevel().setBlockAndUpdate(pos.offset(1, 1, 2), GTMachines.FLUID_EXPORT_HATCH[GTValues.HV].getBlock().defaultBlockState());
        var healthy = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(healthy.matches() && controller.isFormed(),
                "Complete 5-tall Distillation Tower with fluid export hatch must form and pass: "
                        + healthy.detail().getString() + " at " + healthy.position());
        helper.assertTrue(healthy.highlights().isEmpty() && healthy.totalIssueCount() == 0,
                "Formed Distillation Tower must produce no highlights");

        // (c) Break one casing on repeatable ring y=2 (MISSING) and place bedrock in hollow center y=1 (WRONG).
        BlockPos missingRingPos = pos.offset(1, 2, 1);
        BlockPos wrongCenterPos = pos.offset(0, 1, 1);
        helper.getLevel().setBlockAndUpdate(missingRingPos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(wrongCenterPos, Blocks.BEDROCK.defaultBlockState());
        helper.assertTrue(!controller.isFormed(), "Damaging formed Distillation Tower must invalidate formation");

        var damaged = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!damaged.matches() && !controller.isFormed(),
                "Damaged variable-length Distillation Tower must fail and stay unformed");

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

    @GameTest(template = "empty", batch = "structureTerminalActiveTransformer", required = true)
    public static void terminalRejectsUnformedActiveTransformer(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
        clearRegion(helper, pos.offset(-4, -3, -4), pos.offset(4, 4, 4));

        // 3x3x3 Active Transformer facing NORTH: dx in [-1, 1], dy in [-1, 1], dz in [0, 2], core at (0, 0, 1).
        helper.getLevel().setBlockAndUpdate(pos, GTMultiMachines.ACTIVE_TRANSFORMER.getBlock().defaultBlockState());
        var controller = (IMultiController) MetaMachine.getMachine(helper.getLevel(), pos);
        controller.self().setFrontFacing(Direction.NORTH);

        var casing = GTBlocks.HIGH_POWER_CASING.getDefaultState();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 0; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos target = pos.offset(dx, dy, dz);
                    if (dx == 0 && dy == 0 && dz == 1) {
                        helper.getLevel().setBlockAndUpdate(target, GTBlocks.SUPERCONDUCTING_COIL.getDefaultState());
                    } else {
                        helper.getLevel().setBlockAndUpdate(target, casing);
                    }
                }
            }
        }
        // Only an energy input hatch, missing energy output hatch.
        helper.getLevel().setBlockAndUpdate(pos.offset(0, 0, 2), GTMachines.ENERGY_INPUT_HATCH[GTValues.EV].getBlock().defaultBlockState());

        var missingOutput = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!controller.isFormed(), "Active Transformer without energy output hatch must stay unformed");
        helper.assertTrue(!missingOutput.matches(), "Terminal must reject Active Transformer missing energy output hatch");
        helper.assertTrue(Component.translatable("gtecore.structure_terminal.reject.active_transformer_no_output").equals(missingOutput.detail()),
                "Expected missing energy output hatch reason, got: " + missingOutput.detail().getString());

        // Adding an energy output hatch makes the Active Transformer form cleanly.
        helper.getLevel().setBlockAndUpdate(pos.offset(1, 0, 2), GTMachines.ENERGY_OUTPUT_HATCH[GTValues.EV].getBlock().defaultBlockState());
        var valid = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(valid.matches() && controller.isFormed(),
                "Active Transformer with both energy input and output hatches must form and pass: "
                        + valid.detail().getString());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structureTerminalLargeMiner", required = true)
    public static void terminalRejectsSidewaysLargeMiner(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 24, 1));
        clearRegion(helper, pos.offset(-8, -8, -8), pos.offset(8, 8, 8));

        helper.getLevel().setBlockAndUpdate(pos, GTMultiMachines.LARGE_MINER[GTValues.EV].getBlock().defaultBlockState());
        var controller = (IMultiController) MetaMachine.getMachine(helper.getLevel(), pos);
        controller.self().setFrontFacing(Direction.NORTH);
        controller.self().setUpwardsFacing(Direction.EAST);
        helper.assertTrue(controller.self().getUpwardsFacing() == Direction.EAST,
                "Large Miner must accept upwardsFacing=EAST for sideways regression test");

        // Build matching sideways geometry so BlockPattern passes while LargeMinerMachine.checkPattern() rejects it.
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        creativeAutoBuild(helper, player, controller, pos);

        var sideways = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!controller.isFormed(), "Sideways Large Miner must stay unformed");
        helper.assertTrue(!sideways.matches(), "Terminal must report failure for sideways Large Miner");
        helper.assertTrue(pos.equals(sideways.position()),
                "Sideways Large Miner failure must point to controller position");
        helper.assertTrue(Component.translatable("gtecore.structure_terminal.reject.large_miner_orientation").equals(sideways.detail()),
                "Expected Large Miner sideways orientation reason, got: " + sideways.detail().getString());
        helper.assertTrue(sideways.highlights().size() == 1
                        && sideways.highlights().get(0).pos().equals(pos)
                        && sideways.highlights().get(0).kind() == HighlightKind.WRONG
                        && sideways.highlights().get(0).primary(),
                "Sideways Large Miner controller must be highlighted as primary WRONG");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structureTerminalResearchStation", required = true)
    public static void terminalHighlightsMisrotatedResearchStationHolder(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 24, 1));
        clearRegion(helper, pos.offset(-8, -6, -8), pos.offset(8, 6, 8));

        helper.getLevel().setBlockAndUpdate(pos, GTResearchMachines.RESEARCH_STATION.getBlock().defaultBlockState());
        var controller = (IMultiController) MetaMachine.getMachine(helper.getLevel(), pos);
        controller.self().setFrontFacing(Direction.NORTH);

        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        creativeAutoBuild(helper, player, controller, pos);

        // Locate the placed Object Holder and intentionally rotate it the wrong way (same facing as controller).
        BlockPos holderPos = null;
        MetaMachine holderMachine = null;
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-6, -4, -6), pos.offset(6, 4, 6))) {
            MetaMachine m = MetaMachine.getMachine(helper.getLevel(), scan);
            if (m instanceof IObjectHolder) {
                holderPos = scan.immutable();
                holderMachine = m;
                break;
            }
        }
        helper.assertTrue(holderPos != null && holderMachine != null, "Auto-built Research Station must contain an Object Holder");
        holderMachine.setFrontFacing(Direction.NORTH);

        var misrotated = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(!controller.isFormed(), "Research Station with mis-rotated Object Holder must stay unformed");
        helper.assertTrue(!misrotated.matches(), "Terminal must reject Research Station with mis-rotated Object Holder");
        helper.assertTrue(holderPos.equals(misrotated.position()),
                "Failure must point to the mis-rotated Object Holder position");
        helper.assertTrue(Component.translatable("gtecore.structure_terminal.reject.research_holder_facing").equals(misrotated.detail()),
                "Expected Object Holder facing reason, got: " + misrotated.detail().getString());
        helper.assertTrue(misrotated.highlights().size() == 1
                        && misrotated.highlights().get(0).pos().equals(holderPos)
                        && misrotated.highlights().get(0).kind() == HighlightKind.WRONG
                        && misrotated.highlights().get(0).primary(),
                "Mis-rotated Object Holder must be highlighted as primary WRONG");

        // Rotate Object Holder to face the controller (SOUTH) -> Research Station forms cleanly.
        holderMachine.setFrontFacing(Direction.SOUTH);
        var formed = StructureTestingTerminalBehavior.diagnose(controller);
        helper.assertTrue(formed.matches() && controller.isFormed(),
                "Research Station with properly oriented Object Holder must form and pass: "
                        + formed.detail().getString());
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

    private static void clearRegion(GameTestHelper helper, BlockPos min, BlockPos max) {
        for (BlockPos clear : BlockPos.betweenClosed(min, max)) {
            helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void creativeAutoBuild(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player,
                                          IMultiController controller, BlockPos pos) {
        GameType previousMode = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.CREATIVE);
        try {
            controller.getPattern().autoBuild(player, new MultiblockState(helper.getLevel(), pos));
        } finally {
            player.setGameMode(previousMode);
        }
    }
}
