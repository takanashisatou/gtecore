package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.network.GTECoreNetwork;
import org.satou.gtecore.common.network.packets.SPacketStructureHighlight;
import org.satou.gtecore.config.GTEConfig;
import org.satou.gtecore.mixin.BlockPatternAccessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Read-only diagnostics. Never reuse the controller's live matching state. */
public class StructureTestingTerminalBehavior implements IInteractionItem, IAddInformation {

    private static final String PREFIX = "gtecore.structure_terminal.";
    public static final int MAX_HIGHLIGHTS = 64;

    public enum HighlightKind {
        MISSING,
        WRONG
    }

    public record HighlightTarget(BlockPos pos, HighlightKind kind, boolean primary) {}

    public record Diagnostic(
            boolean matches,
            @Nullable BlockPos position,
            Component detail,
            Component candidates,
            List<HighlightTarget> highlights,
            int totalIssueCount) {

        public Diagnostic(boolean matches, @Nullable BlockPos position, Component detail, Component candidates) {
            this(matches, position, detail, candidates, List.of(), 0);
        }
    }

    private record CheckOutcome(
            Diagnostic baseDiagnostic,
            Direction facing,
            boolean flipped,
            @Nullable PatternError rawError) {}

    private record ScanResult(List<HighlightTarget> highlights, int totalIssueCount) {}

    public static int getHighlightDurationTicks() {
        int seconds = GTEConfig.INSTANCE != null ? GTEConfig.INSTANCE.structureHighlightDurationSeconds : 20;
        return Math.max(1, seconds) * 20;
    }

    public static Diagnostic diagnose(IMultiController controller) {
        return diagnose(controller, controller.self().getPos());
    }

    public static Diagnostic diagnose(IMultiController controller, @Nullable BlockPos referencePos) {
        var lock = controller.getPatternLock();
        lock.lock();
        try {
            // Use the actual pattern: some controllers generate it dynamically.
            BlockPattern pattern = controller.getPattern();
            if (pattern == null) return new Diagnostic(false, null, text("no_pattern"), Component.empty());
            int[] repetitions = pattern.getFormedRepetitionCount();
            int[] saved = repetitions.clone();
            try {
                var machine = controller.self();
                Direction[] facings = controller.hasFrontFacing() ? new Direction[] { machine.getFrontFacing() } :
                        new Direction[] { Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST };
                CheckOutcome firstFailure = null;
                for (Direction facing : facings) {
                    CheckOutcome outcome = check(controller, pattern, facing, machine.isFlipped());
                    if (outcome.baseDiagnostic().matches()) return outcome.baseDiagnostic();
                    if (firstFailure == null) firstFailure = outcome;
                    if (machine.allowFlip()) {
                        outcome = check(controller, pattern, facing, !machine.isFlipped());
                        if (outcome.baseDiagnostic().matches()) return outcome.baseDiagnostic();
                    }
                }
                if (firstFailure == null) {
                    return new Diagnostic(false, null, text("failure"), Component.empty());
                }
                Diagnostic base = firstFailure.baseDiagnostic();
                if (firstFailure.rawError() == MultiblockState.UNLOAD_ERROR
                        || firstFailure.rawError() == MultiblockState.UNINIT_ERROR) {
                    return base;
                }
                BlockPos ref = referencePos != null ? referencePos : machine.getPos();
                ScanResult scan = scanMismatches(controller, pattern, firstFailure.facing(), firstFailure.flipped(),
                        base.position(), firstFailure.rawError(), ref);
                return new Diagnostic(false, base.position(), base.detail(), base.candidates(),
                        scan.highlights(), scan.totalIssueCount());
            } finally {
                System.arraycopy(saved, 0, repetitions, 0, saved.length);
            }
        } finally {
            lock.unlock();
        }
    }

    private static CheckOutcome check(IMultiController controller, BlockPattern pattern, Direction facing,
                                      boolean flipped) {
        var machine = controller.self();
        var state = new MultiblockState(machine.getLevel(), machine.getPos());
        if (pattern.checkPatternAt(state, machine.getPos(), facing, machine.getUpwardsFacing(), flipped, false)) {
            return new CheckOutcome(new Diagnostic(true, null, text("success"), Component.empty()),
                    facing, flipped, null);
        }
        return new CheckOutcome(describeFailure(state), facing, flipped, state.error);
    }

    static Diagnostic describeFailure(MultiblockState state) {
        PatternError error = state.error;
        // Shared sentinel errors have no worldState; do not call getPos/getCandidates on them.
        if (error == MultiblockState.UNLOAD_ERROR) {
            return new Diagnostic(false, null, text("unloaded"), Component.empty());
        }
        if (error == null || error == MultiblockState.UNINIT_ERROR || state.predicate == null) {
            return new Diagnostic(false, null, text("failure"), Component.empty());
        }
        var names = new LinkedHashSet<Component>();
        for (var alternatives : error.getCandidates()) {
            for (ItemStack stack : alternatives) {
                if (!stack.isEmpty()) names.add(stack.getHoverName());
                if (names.size() >= 8) break;
            }
            if (names.size() >= 8) break;
        }
        MutableComponent candidates = Component.empty();
        for (Component name : names) {
            if (!candidates.getSiblings().isEmpty()) candidates.append(", ");
            candidates.append(name.copy());
        }
        Component detail = error.getClass() == PatternError.class ? text("wrong_block") : error.getErrorInfo();
        // Global minCount (type 1) is checked after the loop and has no specific failing position.
        // Global maxCount (type 0) fails at the excess block that triggered it.
        BlockPos position = error instanceof SinglePredicateError single && single.type == 1 ? null : state.getPos();
        return new Diagnostic(false, position, detail, candidates);
    }

    private static ScanResult scanMismatches(IMultiController controller, BlockPattern pattern, Direction facing,
                                             boolean flipped, @Nullable BlockPos primaryPos,
                                             @Nullable PatternError primaryError, BlockPos referencePos) {
        var machine = controller.self();
        Level level = machine.getLevel();
        BlockPos centerPos = machine.getPos();
        Direction upwardsFacing = machine.getUpwardsFacing();
        if (level == null) {
            return new ScanResult(List.of(), 0);
        }

        // Guarded instanceof check: never force-cast Mixin accessor interfaces.
        if (!(pattern instanceof BlockPatternAccessor accessor)) {
            return fallbackSingleHighlight(level, primaryPos, primaryError);
        }

        TraceabilityPredicate[][][] blockMatches = accessor.gtecore$getBlockMatches();
        int fingerLength = accessor.gtecore$getFingerLength();
        int thumbLength = accessor.gtecore$getThumbLength();
        int palmLength = accessor.gtecore$getPalmLength();
        int[] centerOffset = accessor.gtecore$getCenterOffset();
        int[][] aisleRepetitions = pattern.aisleRepetitions;

        if (blockMatches == null || centerOffset == null || centerOffset.length < 5
                || aisleRepetitions == null || aisleRepetitions.length != fingerLength
                || fingerLength <= 0 || thumbLength <= 0 || palmLength <= 0) {
            return fallbackSingleHighlight(level, primaryPos, primaryError);
        }

        int[] reps = chooseRepetitions(level, centerPos, accessor, blockMatches, aisleRepetitions,
                centerOffset, fingerLength, thumbLength, palmLength, facing, upwardsFacing, flipped);

        int controllerAisle = Mth.clamp(centerOffset[2], 0, fingerLength - 1);
        int startZ = 0;
        for (int c = 0; c < controllerAisle; c++) {
            startZ -= reps[c];
        }

        MultiblockState scanState = new MultiblockState(level, centerPos);
        scanState.clean();
        Map<BlockPos, HighlightKind> allIssues = new LinkedHashMap<>();

        int z = startZ;
        for (int c = 0; c < fingerLength; c++) {
            for (int r = 0; r < reps[c]; r++) {
                scanState.getLayerCount().clear();
                for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {
                        TraceabilityPredicate predicate = blockMatches[c][b][a];
                        if (predicate == null || predicate.isAny()) {
                            continue;
                        }
                        BlockPos pos = accessor.gtecore$setActualRelativeOffset(x, y, z, facing, upwardsFacing, flipped)
                                .offset(centerPos);
                        if (!scanState.update(pos, predicate)) {
                            continue;
                        }
                        boolean canPartShared = checkPartShared(scanState, predicate, centerPos);
                        boolean matched = predicate.test(scanState) && canPartShared;
                        if (!matched) {
                            HighlightKind kind = classifyFailure(scanState, predicate, canPartShared);
                            allIssues.putIfAbsent(pos.immutable(), kind);
                        }
                    }
                }
                z++;
            }
        }

        if (primaryPos != null && !allIssues.containsKey(primaryPos) && level.isLoaded(primaryPos)) {
            allIssues.put(primaryPos.immutable(), classifyPrimaryFallback(level, primaryPos, primaryError));
        }

        if (allIssues.isEmpty()) {
            return new ScanResult(List.of(), 0);
        }

        int totalCount = allIssues.size();
        List<HighlightTarget> selected = new ArrayList<>(Math.min(totalCount, MAX_HIGHLIGHTS));
        BlockPos primaryKey = primaryPos != null ? primaryPos.immutable() : null;
        if (primaryKey != null && allIssues.containsKey(primaryKey)) {
            selected.add(new HighlightTarget(primaryKey, allIssues.get(primaryKey), true));
        }

        if (selected.size() < MAX_HIGHLIGHTS) {
            List<Map.Entry<BlockPos, HighlightKind>> secondary = new ArrayList<>(allIssues.size());
            for (var entry : allIssues.entrySet()) {
                if (primaryKey == null || !entry.getKey().equals(primaryKey)) {
                    secondary.add(entry);
                }
            }
            secondary.sort(Comparator
                    .comparingDouble((Map.Entry<BlockPos, HighlightKind> e) -> e.getKey().distSqr(referencePos))
                    .thenComparing(Map.Entry::getKey));
            int remaining = MAX_HIGHLIGHTS - selected.size();
            int limit = Math.min(remaining, secondary.size());
            for (int i = 0; i < limit; i++) {
                var entry = secondary.get(i);
                selected.add(new HighlightTarget(entry.getKey(), entry.getValue(), false));
            }
        }

        return new ScanResult(List.copyOf(selected), totalCount);
    }

    private static int[] chooseRepetitions(Level level, BlockPos centerPos, BlockPatternAccessor accessor,
                                           TraceabilityPredicate[][][] blockMatches, int[][] aisleRepetitions,
                                           int[] centerOffset, int fingerLength, int thumbLength, int palmLength,
                                           Direction facing, Direction upwardsFacing, boolean flipped) {
        int[] reps = new int[fingerLength];
        boolean hasVariable = false;
        for (int c = 0; c < fingerLength; c++) {
            int minRep = Math.max(0, aisleRepetitions[c][0]);
            int maxRep = Math.max(minRep, aisleRepetitions[c][1]);
            reps[c] = minRep;
            if (maxRep > minRep) {
                hasVariable = true;
            }
        }
        if (!hasVariable) {
            return reps;
        }

        int controllerAisle = Mth.clamp(centerOffset[2], 0, fingerLength - 1);

        // Repeatable aisles before the controller: grow backwards from z = -1 while slices match cleanly.
        int zBefore = 0;
        for (int c = controllerAisle - 1; c >= 0; c--) {
            int minRep = Math.max(0, aisleRepetitions[c][0]);
            int maxRep = Math.max(minRep, aisleRepetitions[c][1]);
            int chosen = minRep;
            if (maxRep > minRep) {
                for (int r = minRep; r < maxRep; r++) {
                    int candidateZ = zBefore - (r + 1);
                    if (sliceMatchesCleanly(level, centerPos, accessor, blockMatches[c], centerOffset,
                            thumbLength, palmLength, candidateZ, facing, upwardsFacing, flipped)) {
                        chosen = r + 1;
                    } else {
                        break;
                    }
                }
            }
            reps[c] = chosen;
            zBefore -= chosen;
        }

        // Controller aisle and aisles after controller: scan forward.
        int zAfter = reps[controllerAisle];
        for (int c = controllerAisle + 1; c < fingerLength; c++) {
            int minRep = Math.max(0, aisleRepetitions[c][0]);
            int maxRep = Math.max(minRep, aisleRepetitions[c][1]);
            int chosen = minRep;
            if (maxRep > minRep) {
                int matchedCap = -1;
                if (c + 1 < fingerLength && !isAisleAllAny(blockMatches[c + 1], thumbLength, palmLength)) {
                    // Prefer the furthest valid end-cap slice supported by at least one matching non-air block before it.
                    for (int k = maxRep; k >= minRep; k--) {
                        if (sliceMatchesCleanly(level, centerPos, accessor, blockMatches[c + 1], centerOffset,
                                thumbLength, palmLength, zAfter + k, facing, upwardsFacing, flipped)) {
                            if (k == minRep || sliceHasNonAirMatch(level, centerPos, accessor, blockMatches[c],
                                    centerOffset, thumbLength, palmLength, zAfter + k - 1,
                                    facing, upwardsFacing, flipped)) {
                                matchedCap = k;
                                break;
                            }
                        }
                    }
                }
                if (matchedCap >= minRep) {
                    chosen = matchedCap;
                } else {
                    for (int k = minRep; k < maxRep; k++) {
                        if (sliceMatchesCleanly(level, centerPos, accessor, blockMatches[c], centerOffset,
                                thumbLength, palmLength, zAfter + k, facing, upwardsFacing, flipped)) {
                            chosen = k + 1;
                        } else {
                            break;
                        }
                    }
                }
            }
            reps[c] = chosen;
            zAfter += chosen;
        }

        return reps;
    }

    private static boolean isAisleAllAny(TraceabilityPredicate[][] slice, int thumbLength, int palmLength) {
        for (int b = 0; b < thumbLength; b++) {
            for (int a = 0; a < palmLength; a++) {
                TraceabilityPredicate predicate = slice[b][a];
                if (predicate != null && !predicate.isAny()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean sliceMatchesCleanly(Level level, BlockPos centerPos, BlockPatternAccessor accessor,
                                               TraceabilityPredicate[][] slice, int[] centerOffset,
                                               int thumbLength, int palmLength, int z,
                                               Direction facing, Direction upwardsFacing, boolean flipped) {
        MultiblockState probe = new MultiblockState(level, centerPos);
        probe.clean();
        for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
            for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {
                TraceabilityPredicate predicate = slice[b][a];
                if (predicate == null || predicate.isAny()) {
                    continue;
                }
                BlockPos pos = accessor.gtecore$setActualRelativeOffset(x, y, z, facing, upwardsFacing, flipped)
                        .offset(centerPos);
                if (!probe.update(pos, predicate)) {
                    return false;
                }
                if (!checkPartShared(probe, predicate, centerPos) || !predicate.test(probe)) {
                    return false;
                }
            }
        }
        for (var entry : probe.getLayerCount().object2IntEntrySet()) {
            if (entry.getIntValue() < entry.getKey().minLayerCount) {
                return false;
            }
        }
        return true;
    }

    private static boolean sliceHasNonAirMatch(Level level, BlockPos centerPos, BlockPatternAccessor accessor,
                                               TraceabilityPredicate[][] slice, int[] centerOffset,
                                               int thumbLength, int palmLength, int z,
                                               Direction facing, Direction upwardsFacing, boolean flipped) {
        MultiblockState probe = new MultiblockState(level, centerPos);
        probe.clean();
        for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
            for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {
                TraceabilityPredicate predicate = slice[b][a];
                if (predicate == null || predicate.isAny() || predicate.isAir()) {
                    continue;
                }
                BlockPos pos = accessor.gtecore$setActualRelativeOffset(x, y, z, facing, upwardsFacing, flipped)
                        .offset(centerPos);
                if (!probe.update(pos, predicate)) {
                    continue;
                }
                BlockState state = probe.getBlockState();
                if (!state.isAir() && !state.canBeReplaced()
                        && checkPartShared(probe, predicate, centerPos)
                        && predicate.test(probe)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkPartShared(MultiblockState state, TraceabilityPredicate predicate, BlockPos centerPos) {
        if (state.getTileEntity() instanceof IMachineBlockEntity machineBlockEntity
                && machineBlockEntity.getMetaMachine() instanceof IMultiPart part) {
            if (!predicate.isAny() && part.isFormed() && !part.canShared() && !part.hasController(centerPos)) {
                return false;
            }
        }
        return true;
    }

    private static HighlightKind classifyFailure(MultiblockState state, TraceabilityPredicate predicate,
                                                 boolean canPartShared) {
        if (!canPartShared) {
            return HighlightKind.WRONG;
        }
        if (state.error instanceof SinglePredicateError single && (single.type == 0 || single.type == 2)) {
            return HighlightKind.WRONG;
        }
        if (predicate.isAir()) {
            return HighlightKind.WRONG;
        }
        BlockState blockState = state.getBlockState();
        return (blockState.isAir() || blockState.canBeReplaced()) ? HighlightKind.MISSING : HighlightKind.WRONG;
    }

    private static HighlightKind classifyPrimaryFallback(Level level, BlockPos pos, @Nullable PatternError error) {
        if (error instanceof SinglePredicateError single && (single.type == 0 || single.type == 2)) {
            return HighlightKind.WRONG;
        }
        if (error instanceof PatternStringError) {
            return HighlightKind.WRONG;
        }
        BlockState blockState = level.getBlockState(pos);
        return (blockState.isAir() || blockState.canBeReplaced()) ? HighlightKind.MISSING : HighlightKind.WRONG;
    }

    private static ScanResult fallbackSingleHighlight(Level level, @Nullable BlockPos primaryPos,
                                                      @Nullable PatternError primaryError) {
        if (primaryPos == null || !level.isLoaded(primaryPos)) {
            return new ScanResult(List.of(), 0);
        }
        HighlightKind kind = classifyPrimaryFallback(level, primaryPos, primaryError);
        return new ScanResult(List.of(new HighlightTarget(primaryPos.immutable(), kind, true)), 1);
    }

    private static void sendHighlights(Player player, Level level, Diagnostic result) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) {
            return;
        }
        if (result.matches() || result.highlights().isEmpty()) {
            GTECoreNetwork.sendToPlayer(serverPlayer, SPacketStructureHighlight.clear(level.dimension(), false));
        } else {
            GTECoreNetwork.sendToPlayer(serverPlayer, SPacketStructureHighlight.highlight(
                    level.dimension(), getHighlightDurationTicks(), result.highlights()));
        }
    }

    private static void sendClear(Player player, Level level, boolean notifyOnClear) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            GTECoreNetwork.sendToPlayer(serverPlayer, SPacketStructureHighlight.clear(level.dimension(), notifyOnClear));
        }
    }

    public void checkStructure(Player player, IMultiController controller) {
        try {
            Diagnostic result = diagnose(controller, player.blockPosition());
            Level level = controller.self().getLevel();
            if (level != null) {
                sendHighlights(player, level, result);
            }
            player.sendSystemMessage(text(result.matches() ? "success" : "failure")
                    .withStyle(result.matches() ? ChatFormatting.GREEN : ChatFormatting.RED));
            if (!result.matches()) {
                if (result.position() != null) {
                    BlockPos pos = result.position();
                    player.sendSystemMessage(Component.translatable(PREFIX + "position", pos.getX(), pos.getY(), pos.getZ()));
                }
                player.sendSystemMessage(result.detail());
                if (!result.candidates().getString().isEmpty()) {
                    player.sendSystemMessage(Component.translatable(PREFIX + "candidates", result.candidates()));
                }
                if (!result.highlights().isEmpty()) {
                    if (result.totalIssueCount() > result.highlights().size()) {
                        player.sendSystemMessage(Component.translatable(PREFIX + "highlighted_capped",
                                result.highlights().size(), result.totalIssueCount()).withStyle(ChatFormatting.YELLOW));
                    } else {
                        player.sendSystemMessage(Component.translatable(PREFIX + "highlighted",
                                result.highlights().size()).withStyle(ChatFormatting.YELLOW));
                    }
                }
            }
        } catch (RuntimeException exception) {
            GTECore.LOGGER.error("Structure terminal could not inspect {}", controller.self().getPos(), exception);
            player.sendSystemMessage(text("error").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context); // Intercept before the machine opens its normal UI.
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !(MetaMachine.getMachine(level, context.getClickedPos()) instanceof IMultiController controller)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) checkStructure(player, controller);
        } else if (level.isClientSide) {
            ClientPreview.show(player, controller);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                sendClear(player, level, true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(text("tooltip.check"));
        lines.add(text("tooltip.preview"));
        lines.add(text("tooltip.highlight"));
    }

    private static MutableComponent text(String key) {
        return Component.translatable(PREFIX + key);
    }

    /** Isolated so dedicated servers never initialize the renderer's OpenGL buffers. */
    private static final class ClientPreview {

        private static void show(Player player, IMultiController controller) {
            var machine = controller.self();
            try {
                if (!machine.getDefinition().isRenderWorldPreview()) {
                    player.displayClientMessage(text("no_preview"), true);
                    return;
                }
                // GTM's example renderer does not mirror shapes or support vertical fronts correctly.
                if (machine.isFlipped() || machine.getFrontFacing().getAxis() == Direction.Axis.Y) {
                    player.displayClientMessage(text("preview_orientation"), true);
                    return;
                }
                MultiblockInWorldPreviewRenderer.showPreview(machine.getPos(), machine,
                        ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
                player.displayClientMessage(text("preview"), true);
            } catch (RuntimeException exception) {
                GTECore.LOGGER.error("Structure terminal could not preview {}", machine.getPos(), exception);
                player.displayClientMessage(text("no_preview"), true);
            }
        }
    }
}
