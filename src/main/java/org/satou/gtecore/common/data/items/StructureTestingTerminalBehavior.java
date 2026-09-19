package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.satou.gtecore.GTECore;

import java.util.LinkedHashSet;
import java.util.List;

/** Read-only diagnostics. Never reuse the controller's live matching state. */
public class StructureTestingTerminalBehavior implements IInteractionItem, IAddInformation {
    private static final String PREFIX = "gtecore.structure_terminal.";

    public record Diagnostic(boolean matches, BlockPos position, Component detail, Component candidates) {}

    public static Diagnostic diagnose(IMultiController controller) {
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
                Diagnostic firstFailure = null;
                for (Direction facing : facings) {
                    Diagnostic result = check(controller, pattern, facing, machine.isFlipped());
                    if (result.matches()) return result;
                    if (firstFailure == null) firstFailure = result;
                    if (machine.allowFlip()) {
                        result = check(controller, pattern, facing, !machine.isFlipped());
                        if (result.matches()) return result;
                    }
                }
                return firstFailure;
            } finally {
                System.arraycopy(saved, 0, repetitions, 0, saved.length);
            }
        } finally {
            lock.unlock();
        }
    }

    private static Diagnostic check(IMultiController controller, BlockPattern pattern, Direction facing, boolean flipped) {
        var machine = controller.self();
        var state = new MultiblockState(machine.getLevel(), machine.getPos());
        if (pattern.checkPatternAt(state, machine.getPos(), facing, machine.getUpwardsFacing(), flipped, false)) {
            return new Diagnostic(true, null, text("success"), Component.empty());
        }
        return describeFailure(state);
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
        // A global count error does not identify a wrong block at the last visited position.
        BlockPos position = error instanceof SinglePredicateError single && single.type <= 1 ? null : state.getPos();
        return new Diagnostic(false, position, detail, candidates);
    }

    public void checkStructure(Player player, IMultiController controller) {
        try {
            Diagnostic result = diagnose(controller);
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
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(text("tooltip.check"));
        lines.add(text("tooltip.preview"));
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
