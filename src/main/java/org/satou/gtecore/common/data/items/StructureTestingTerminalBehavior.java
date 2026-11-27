package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.IObjectHolder;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.ActiveTransformerMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CleanroomMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.DistillationTowerMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.LargeMinerMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.DataBankMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.primitive.CharcoalPileIgniterMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.network.GTECoreNetwork;
import org.satou.gtecore.common.network.packets.SPacketStructureHighlight;
import org.satou.gtecore.config.GTEConfig;
import org.satou.gtecore.mixin.BlockPatternAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Structure formation check, failure diagnosis, and multi-position highlight targeting for multiblock controllers. */
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
            boolean matched,
            @Nullable Diagnostic baseDiagnostic,
            Direction facing,
            boolean flipped,
            @Nullable PatternError rawError,
            @Nullable MultiblockState state) {}

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
            // Ground truth: an already-formed controller must immediately report formed without invoking getPattern().
            if (controller.isFormed()) {
                return new Diagnostic(true, null, text("success"), Component.empty());
            }

            // Attempt real formation on the server so freshly completed valid structures form immediately.
            if (tryFormController(controller)) {
                return new Diagnostic(true, null, text("success"), Component.empty());
            }

            // Controller is still unformed: never report success.
            return diagnoseUnformed(controller, referencePos);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs real locked formation matching {@code MultiblockControllerMachine.asyncCheckPattern}, while guarding
     * against controllers whose {@code onStructureFormed()} rejects the structure via {@code onStructureInvalid()}.
     */
    private static boolean tryFormController(IMultiController controller) {
        var machine = controller.self();
        if (!(machine.getLevel() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!controller.checkPattern()) {
            return false;
        }
        MultiblockState liveState = controller.getMultiblockState();
        machine.setFlipped(liveState.isNeededFlip());
        controller.onStructureFormed();
        if (controller.isFormed()) {
            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
            mwsd.addMapping(liveState);
            mwsd.removeAsyncLogic(controller);
            machine.notifyBlockUpdate();
            return true;
        }
        machine.setFlipped(false);
        return false;
    }

    private static Diagnostic diagnoseUnformed(IMultiController controller, @Nullable BlockPos referencePos) {
        // Use the actual pattern: some controllers generate it dynamically.
        BlockPattern pattern = controller.getPattern();
        if (pattern == null) {
            return new Diagnostic(false, null, text("no_pattern"), Component.empty());
        }
        int[] repetitions = pattern.getFormedRepetitionCount();
        int[] saved = repetitions.clone();
        try {
            var machine = controller.self();
            Direction[] facings = controller.hasFrontFacing() ? new Direction[] { machine.getFrontFacing() } :
                    new Direction[] { Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST };
            boolean allowsFlip = machine.allowFlip();
            CheckOutcome firstFailure = null;
            CheckOutcome matchedOutcome = null;
            // Strictly align facing and flip traversal with BlockPattern.checkPatternAt(MultiblockState, boolean).
            for (Direction facing : facings) {
                CheckOutcome outcome = check(controller, pattern, facing, false);
                if (outcome.matched()) {
                    matchedOutcome = outcome;
                    break;
                }
                if (firstFailure == null) {
                    firstFailure = outcome;
                }
                if (allowsFlip) {
                    CheckOutcome flippedOutcome = check(controller, pattern, facing, true);
                    if (flippedOutcome.matched()) {
                        matchedOutcome = flippedOutcome;
                    }
                    break;
                }
            }
            if (matchedOutcome != null && matchedOutcome.state() != null) {
                return diagnosePostPatternRejection(controller, matchedOutcome.state());
            }
            if (firstFailure == null || firstFailure.baseDiagnostic() == null) {
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
    }

    private static CheckOutcome check(IMultiController controller, BlockPattern pattern, Direction facing,
                                      boolean flipped) {
        var machine = controller.self();
        var state = new MultiblockState(machine.getLevel(), machine.getPos());
        if (pattern.checkPatternAt(state, machine.getPos(), facing, machine.getUpwardsFacing(), flipped, false)) {
            return new CheckOutcome(true, null, facing, flipped, null, state);
        }
        return new CheckOutcome(false, describeFailure(state), facing, flipped, state.error, null);
    }

    private static Diagnostic diagnosePostPatternRejection(IMultiController controller, MultiblockState matchedState) {
        var machine = controller.self();
        List<IMultiPart> matchedParts = collectMatchedParts(controller, matchedState);

        if (controller instanceof LargeMinerMachine) {
            Direction upwards = machine.getUpwardsFacing();
            if (upwards != Direction.NORTH && upwards != Direction.SOUTH) {
                return singleWrongBlockDiagnostic(machine.getPos().immutable(),
                        text("reject.large_miner_orientation"));
            }
        }

        if (controller instanceof DistillationTowerMachine) {
            int startY = machine.getPos().getY() + 1;
            List<IMultiPart> exportParts = matchedParts.stream()
                    .filter(part -> PartAbility.EXPORT_FLUIDS.isApplicable(part.self().getBlockState().getBlock()))
                    .filter(part -> part.self().getPos().getY() >= startY)
                    .toList();
            if (exportParts.isEmpty()) {
                return new Diagnostic(false, null, text("reject.distillation_no_output"), Component.empty());
            }
            int maxY = exportParts.get(exportParts.size() - 1).self().getPos().getY();
            int outputIndex = 0;
            for (int y = startY; y <= maxY; ++y) {
                if (exportParts.size() <= outputIndex) {
                    continue;
                }
                IMultiPart part = exportParts.get(outputIndex);
                int partY = part.self().getPos().getY();
                if (partY == y) {
                    outputIndex++;
                } else if (partY < y) {
                    return singleWrongBlockDiagnostic(part.self().getPos().immutable(),
                            text("reject.distillation_invalid_output"));
                }
            }
            return new Diagnostic(false, null, text("reject.distillation_no_output"), Component.empty());
        }

        if (controller instanceof ActiveTransformerMachine) {
            boolean hasInput = false;
            boolean hasOutput = false;
            Long2ObjectMap<IO> ioMap = matchedState.getMatchContext().getOrCreate("ioMap", Long2ObjectMaps::emptyMap);
            for (IMultiPart part : matchedParts) {
                IO io = ioMap.getOrDefault(part.self().getPos().asLong(), IO.BOTH);
                if (io == IO.NONE) {
                    continue;
                }
                for (var handlerList : part.getRecipeHandlers()) {
                    if (!handlerList.isValid(io)) {
                        continue;
                    }
                    boolean hasEnergy = handlerList.getCapability(EURecipeCapability.CAP).stream()
                            .anyMatch(IEnergyContainer.class::isInstance);
                    if (!hasEnergy) {
                        continue;
                    }
                    if (handlerList.getHandlerIO().support(IO.IN)) {
                        hasInput = true;
                    } else if (handlerList.getHandlerIO().support(IO.OUT)) {
                        hasOutput = true;
                    }
                }
            }
            if (!hasInput && !hasOutput) {
                return new Diagnostic(false, null, text("reject.active_transformer_no_io"), Component.empty());
            }
            if (!hasInput) {
                return new Diagnostic(false, null, text("reject.active_transformer_no_input"), Component.empty());
            }
            if (!hasOutput) {
                return new Diagnostic(false, null, text("reject.active_transformer_no_output"), Component.empty());
            }
        }

        if (controller instanceof PowerSubstationMachine) {
            boolean hasBattery = false;
            for (Map.Entry<String, Object> entry : matchedState.getMatchContext().entrySet()) {
                if (entry.getKey().startsWith(PowerSubstationMachine.PMC_BATTERY_HEADER)
                        && entry.getValue() instanceof PowerSubstationMachine.BatteryMatchWrapper wrapper
                        && wrapper.getAmount() > 0) {
                    hasBattery = true;
                    break;
                }
            }
            if (!hasBattery) {
                return new Diagnostic(false, null, text("reject.power_substation_empty_batteries"), Component.empty());
            }
        }

        if (controller instanceof ResearchStationMachine) {
            Direction expectedHolderFacing = machine.getFrontFacing().getOpposite();
            boolean foundHolder = false;
            boolean foundComputation = false;
            for (IMultiPart part : matchedParts) {
                if (part instanceof IObjectHolder holder) {
                    if (holder.getFrontFacing() != expectedHolderFacing) {
                        return singleWrongBlockDiagnostic(part.self().getPos().immutable(),
                                text("reject.research_holder_facing"));
                    }
                    foundHolder = true;
                }
                if (part.self().getHolder().self().getCapability(GTCapability.CAPABILITY_COMPUTATION_PROVIDER)
                        .isPresent()) {
                    foundComputation = true;
                }
            }
            if (!foundHolder) {
                return new Diagnostic(false, null, text("reject.research_missing_holder"), Component.empty());
            }
            if (!foundComputation) {
                return new Diagnostic(false, null, text("reject.research_missing_computation"), Component.empty());
            }
        }

        if (controller instanceof DataBankMachine) {
            boolean hasMaintenance = matchedParts.stream().anyMatch(IMaintenanceMachine.class::isInstance);
            if (!hasMaintenance) {
                return new Diagnostic(false, null, text("reject.data_bank_maintenance"), Component.empty());
            }
        }

        if (controller instanceof SteamParallelMultiblockMachine) {
            boolean hasSteamHatch = false;
            for (IMultiPart part : matchedParts) {
                if (!PartAbility.STEAM.isApplicable(part.self().getDefinition().getBlock())) {
                    continue;
                }
                for (var handlerList : part.getRecipeHandlers()) {
                    if (!handlerList.isValid(IO.IN)) {
                        continue;
                    }
                    for (var fluidHandler : handlerList.getCapability(FluidRecipeCapability.CAP)) {
                        if (fluidHandler instanceof NotifiableFluidTank tank
                                && tank.isFluidValid(0, GTMaterials.Steam.getFluid(1))) {
                            hasSteamHatch = true;
                            break;
                        }
                    }
                    if (hasSteamHatch) {
                        break;
                    }
                }
                if (hasSteamHatch) {
                    break;
                }
            }
            if (!hasSteamHatch) {
                return new Diagnostic(false, null, text("reject.steam_missing_hatch"), Component.empty());
            }
        }

        if (controller instanceof CleanroomMachine cleanroom && !hasValidCleanroomDimensions(cleanroom)) {
            return singleWrongBlockDiagnostic(machine.getPos().immutable(), text("reject.cleanroom_dimensions"));
        }

        if (controller instanceof CharcoalPileIgniterMachine charcoal && !hasValidCharcoalPileDimensions(charcoal)) {
            return singleWrongBlockDiagnostic(machine.getPos().immutable(), text("reject.charcoal_dimensions"));
        }

        return new Diagnostic(false, null, text("reject.generic"), Component.empty());
    }

    private static List<IMultiPart> collectMatchedParts(IMultiController controller, MultiblockState matchedState) {
        Set<IMultiPart> rawParts = matchedState.getMatchContext().getOrCreate("parts", Collections::emptySet);
        List<IMultiPart> parts = new ArrayList<>();
        for (IMultiPart part : rawParts) {
            if (controller.shouldAddPartToController(part)) {
                parts.add(part);
            }
        }
        parts.sort(controller.getPartSorter());
        return parts;
    }

    private static Diagnostic singleWrongBlockDiagnostic(BlockPos pos, Component detail) {
        return new Diagnostic(false, pos, detail, Component.empty(),
                List.of(new HighlightTarget(pos, HighlightKind.WRONG, true)), 1);
    }

    private static boolean hasValidCleanroomDimensions(CleanroomMachine cleanroom) {
        Level level = cleanroom.getLevel();
        if (level == null) {
            return false;
        }
        Direction front = cleanroom.getFrontFacing();
        Direction back = front.getOpposite();
        Direction left = front.getCounterClockWise();
        Direction right = left.getOpposite();

        BlockPos.MutableBlockPos lPos = cleanroom.getPos().mutable();
        BlockPos.MutableBlockPos rPos = cleanroom.getPos().mutable();
        BlockPos.MutableBlockPos fPos = cleanroom.getPos().mutable();
        BlockPos.MutableBlockPos bPos = cleanroom.getPos().mutable();
        BlockPos.MutableBlockPos hPos = cleanroom.getPos().mutable();

        int lDist = 0;
        int rDist = 0;
        int bDist = 0;
        int fDist = 0;
        int hDist = 0;

        for (int i = 1; i < 8; i++) {
            if (lDist == 0 && cleanroom.isBlockEdge(level, lPos, left)) lDist = i;
            if (rDist == 0 && cleanroom.isBlockEdge(level, rPos, right)) rDist = i;
            if (bDist == 0 && cleanroom.isBlockEdge(level, bPos, back)) bDist = i;
            if (fDist == 0 && cleanroom.isBlockEdge(level, fPos, front)) fDist = i;
            if (lDist != 0 && rDist != 0 && bDist != 0 && fDist != 0) break;
        }
        for (int i = 1; i < 15; i++) {
            if (cleanroom.isBlockFloor(level, hPos, Direction.DOWN)) hDist = i;
            if (hDist != 0) break;
        }
        return Math.abs(lDist - rDist) <= 1 && Math.abs(bDist - fDist) <= 1
                && lDist >= CleanroomMachine.MIN_RADIUS && rDist >= CleanroomMachine.MIN_RADIUS
                && bDist >= CleanroomMachine.MIN_RADIUS && fDist >= CleanroomMachine.MIN_RADIUS
                && hDist >= CleanroomMachine.MIN_DEPTH;
    }

    private static boolean hasValidCharcoalPileDimensions(CharcoalPileIgniterMachine charcoal) {
        Level level = charcoal.getLevel();
        if (level == null) {
            return false;
        }
        Direction front = charcoal.getFrontFacing();
        Direction back = front.getOpposite();
        Direction left = RelativeDirection.LEFT.getRelativeFacing(front, charcoal.getUpwardsFacing(), false);
        Direction right = RelativeDirection.RIGHT.getRelativeFacing(front, charcoal.getUpwardsFacing(), false);

        BlockPos down = charcoal.getPos().relative(Direction.DOWN);
        BlockPos.MutableBlockPos lPos = down.mutable();
        BlockPos.MutableBlockPos rPos = down.mutable();
        BlockPos.MutableBlockPos fPos = down.mutable();
        BlockPos.MutableBlockPos bPos = down.mutable();
        BlockPos.MutableBlockPos hPos = charcoal.getPos().mutable();

        int lDist = 0;
        int rDist = 0;
        int bDist = 0;
        int fDist = 0;
        int hDist = 0;

        for (int i = 1; i < 6; i++) {
            if (lDist != 0 && rDist != 0 && hDist != 0) break;
            if (lDist == 0 && level.getBlockState(lPos.move(left)).is(CustomTags.CHARCOAL_PILE_IGNITER_WALLS)) lDist = i;
            if (rDist == 0 && level.getBlockState(rPos.move(right)).is(CustomTags.CHARCOAL_PILE_IGNITER_WALLS)) rDist = i;
            if (bDist == 0 && level.getBlockState(bPos.move(back)).is(CustomTags.CHARCOAL_PILE_IGNITER_WALLS)) bDist = i;
            if (fDist == 0 && level.getBlockState(fPos.move(front)).is(CustomTags.CHARCOAL_PILE_IGNITER_WALLS)) fDist = i;
            if (hDist == 0 && level.getBlockState(hPos.move(Direction.DOWN)).is(Blocks.BRICKS)) hDist = i;
        }
        return Math.abs(lDist - rDist) <= 1 && Math.abs(bDist - fDist) <= 1
                && lDist >= 1 && rDist >= 1 && fDist >= 1 && bDist >= 1 && hDist >= 2;
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
