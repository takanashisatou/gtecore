package org.satou.gtecore.client.renderer;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.satou.gtecore.common.data.items.StructureTestingTerminalBehavior.HighlightKind;
import org.satou.gtecore.common.data.items.StructureTestingTerminalBehavior.HighlightTarget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class StructureHighlightRenderer {

    private static final int FADE_TICKS = 30;
    private static final long PULSE_PERIOD_MS = 1200L;

    private record ActiveHighlight(BlockPos pos, HighlightKind kind, boolean primary, @Nullable BlockState initialState) {}

    @Nullable
    private static ResourceKey<Level> activeDimension = null;
    private static int remainingTicks = 0;
    private static final Map<BlockPos, ActiveHighlight> HIGHLIGHTS = new LinkedHashMap<>();

    private StructureHighlightRenderer() {}

    public static void handlePacket(ResourceKey<Level> dimension, boolean clear, boolean notifyOnClear,
                                    int durationTicks, List<HighlightTarget> targets) {
        if (clear || targets.isEmpty() || durationTicks <= 0) {
            boolean hadHighlights = clear();
            if (notifyOnClear && hadHighlights) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            Component.translatable("gtecore.structure_terminal.highlight_cleared"), true);
                }
            }
            return;
        }

        HIGHLIGHTS.clear();
        activeDimension = dimension;
        remainingTicks = durationTicks;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        boolean sameDim = level != null && level.dimension().equals(dimension);

        for (HighlightTarget target : targets) {
            BlockPos immutablePos = target.pos().immutable();
            BlockState initialState = sameDim && level.isLoaded(immutablePos) ? level.getBlockState(immutablePos) : null;
            HIGHLIGHTS.put(immutablePos, new ActiveHighlight(immutablePos, target.kind(), target.primary(), initialState));
        }
    }

    public static boolean clear() {
        boolean hadHighlights = !HIGHLIGHTS.isEmpty();
        HIGHLIGHTS.clear();
        activeDimension = null;
        remainingTicks = 0;
        return hadHighlights;
    }

    public static void onClientTick() {
        if (HIGHLIGHTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || activeDimension == null || !level.dimension().equals(activeDimension)) {
            clear();
            return;
        }
        if (!mc.isPaused()) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                clear();
                return;
            }
        }

        var iterator = HIGHLIGHTS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            ActiveHighlight active = entry.getValue();
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (active.initialState() == null) {
                entry.setValue(new ActiveHighlight(pos, active.kind(), active.primary(), current));
            } else if (!current.equals(active.initialState())) {
                iterator.remove();
            }
        }
        if (HIGHLIGHTS.isEmpty()) {
            clear();
        }
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        if (HIGHLIGHTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || activeDimension == null || !level.dimension().equals(activeDimension)) {
            return;
        }

        float fade = remainingTicks < FADE_TICKS
                ? Mth.clamp((remainingTicks - partialTick) / (float) FADE_TICKS, 0.0f, 1.0f)
                : 1.0f;
        if (fade <= 0.0f) {
            return;
        }

        float pulse = 0.5f + 0.5f * Mth.sin((Util.getMillis() % PULSE_PERIOD_MS) / (float) PULSE_PERIOD_MS * Mth.TWO_PI);

        Vec3 camPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        boolean hasSecondary = false;
        ActiveHighlight primaryHighlight = null;

        // Pass 1: Translucent filled X-ray boxes
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (ActiveHighlight entry : HIGHLIGHTS.values()) {
            BlockPos pos = entry.pos();
            float eps = entry.primary() ? 0.008f : 0.003f;
            float r;
            float g;
            float b;
            float a;
            if (entry.kind() == HighlightKind.MISSING) {
                if (entry.primary()) {
                    r = 1.0f;
                    g = 0.78f + 0.17f * pulse;
                    b = 0.12f + 0.18f * pulse;
                    a = (0.30f + 0.22f * pulse) * fade;
                } else {
                    r = 1.0f;
                    g = 0.72f;
                    b = 0.10f;
                    a = 0.22f * fade;
                }
            } else {
                if (entry.primary()) {
                    r = 1.0f;
                    g = 0.18f + 0.22f * pulse;
                    b = 0.14f + 0.16f * pulse;
                    a = (0.34f + 0.24f * pulse) * fade;
                } else {
                    r = 1.0f;
                    g = 0.18f;
                    b = 0.18f;
                    a = 0.26f * fade;
                }
            }
            RenderBufferUtils.renderCubeFace(
                    poseStack, buffer,
                    pos.getX() - eps, pos.getY() - eps, pos.getZ() - eps,
                    pos.getX() + 1.0f + eps, pos.getY() + 1.0f + eps, pos.getZ() + 1.0f + eps,
                    r, g, b, a, true);

            if (entry.primary()) {
                primaryHighlight = entry;
            } else {
                hasSecondary = true;
            }
        }
        tesselator.end();

        // Pass 2: Crisp wireframe outlines for secondary highlights
        if (hasSecondary) {
            buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(2.5f);
            for (ActiveHighlight entry : HIGHLIGHTS.values()) {
                if (entry.primary()) {
                    continue;
                }
                BlockPos pos = entry.pos();
                float eps = 0.003f;
                float r = 1.0f;
                float g = entry.kind() == HighlightKind.MISSING ? 0.78f : 0.22f;
                float b = entry.kind() == HighlightKind.MISSING ? 0.15f : 0.22f;
                float a = (entry.kind() == HighlightKind.MISSING ? 0.88f : 0.92f) * fade;
                RenderBufferUtils.drawCubeFrame(
                        poseStack, buffer,
                        pos.getX() - eps, pos.getY() - eps, pos.getZ() - eps,
                        pos.getX() + 1.0f + eps, pos.getY() + 1.0f + eps, pos.getZ() + 1.0f + eps,
                        r, g, b, a);
            }
            tesselator.end();
        }

        // Pass 3: Thicker pulsing double-wireframe for the primary error
        if (primaryHighlight != null) {
            BlockPos pos = primaryHighlight.pos();
            boolean missing = primaryHighlight.kind() == HighlightKind.MISSING;
            float r = 1.0f;
            float g = missing ? (0.82f + 0.18f * pulse) : (0.20f + 0.30f * pulse);
            float b = missing ? (0.15f + 0.25f * pulse) : (0.15f + 0.20f * pulse);

            buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(4.0f);

            float innerEps = 0.008f;
            RenderBufferUtils.drawCubeFrame(
                    poseStack, buffer,
                    pos.getX() - innerEps, pos.getY() - innerEps, pos.getZ() - innerEps,
                    pos.getX() + 1.0f + innerEps, pos.getY() + 1.0f + innerEps, pos.getZ() + 1.0f + innerEps,
                    r, g, b, (0.85f + 0.15f * pulse) * fade);

            float haloEps = 0.025f + 0.035f * pulse;
            RenderBufferUtils.drawCubeFrame(
                    poseStack, buffer,
                    pos.getX() - haloEps, pos.getY() - haloEps, pos.getZ() - haloEps,
                    pos.getX() + 1.0f + haloEps, pos.getY() + 1.0f + haloEps, pos.getZ() + 1.0f + haloEps,
                    r, g, b, (0.35f + 0.45f * pulse) * fade);

            tesselator.end();
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }
}
