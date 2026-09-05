package org.satou.gtecore.client.renderer.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.BloomUtils;
import com.lowdragmc.shimmer.client.shader.RenderUtils;
import org.satou.gtecore.client.renderer.GTERenderTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.joml.Matrix4f;

public class TreeOfImaginaryRender extends DynamicRender<WorkableElectricMultiblockMachine, TreeOfImaginaryRender> {

    public static final Codec<TreeOfImaginaryRender> CODEC = Codec.unit(TreeOfImaginaryRender::new);
    public static final DynamicRenderType<WorkableElectricMultiblockMachine, TreeOfImaginaryRender> TYPE = new DynamicRenderType<>(
            CODEC);

    private static final int SEGMENTS = 16;
    private static final float[] SIN_16 = new float[SEGMENTS + 1];
    private static final float[] COS_16 = new float[SEGMENTS + 1];

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = (2.0 * Math.PI * i) / SEGMENTS;
            SIN_16[i] = (float) Math.sin(angle);
            COS_16[i] = (float) Math.cos(angle);
        }
    }

    private static final float MAX_FADE = 20.0f; // 1 second (20 ticks)
    private float fadeTimer = 0.0f;

    public TreeOfImaginaryRender() {
    }

    @Override
    public DynamicRenderType<WorkableElectricMultiblockMachine, TreeOfImaginaryRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(WorkableElectricMultiblockMachine machine, Vec3 cameraPos) {
        if (!machine.isFormed()) {
            return false;
        }
        return machine.getRecipeLogic().isWorking() || machine.getRecipeLogic().isActive() || fadeTimer > 0.0f;
    }

    @Override
    public boolean shouldRenderOffScreen(WorkableElectricMultiblockMachine machine) {
        return machine.isFormed() && (machine.getRecipeLogic().isWorking() || machine.getRecipeLogic().isActive() || fadeTimer > 0.0f);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public AABB getRenderBoundingBox(WorkableElectricMultiblockMachine machine) {
        return new AABB(machine.getPos()).inflate(64.0D);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(WorkableElectricMultiblockMachine machine, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        boolean working = machine.getRecipeLogic().isWorking() || machine.getRecipeLogic().isActive();
        if (working) {
            fadeTimer = MAX_FADE;
        } else {
            fadeTimer = Math.max(0.0f, fadeTimer - Minecraft.getInstance().getDeltaFrameTime());
        }

        if (fadeTimer <= 0.0f) {
            return;
        }

        if (GTCEu.Mods.isShimmerLoaded()) {
            PoseStack finalStack = RenderUtils.copyPoseStack(poseStack);
            BloomUtils.entityBloom(source -> renderElements(machine, partialTick, finalStack,
                    source.getBuffer(GTERenderTypes.getImaginaryEnergy())));
        } else {
            renderElements(machine, partialTick, poseStack, buffer.getBuffer(GTERenderTypes.getImaginaryEnergy()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void renderElements(WorkableElectricMultiblockMachine machine, float partialTick,
            PoseStack poseStack, VertexConsumer buffer) {
        float fade = fadeTimer / MAX_FADE;
        Matrix4f mat = poseStack.last().pose();

        Direction front = machine.getFrontFacing();
        Direction upwards = machine.getUpwardsFacing();
        boolean flipped = machine.isFlipped();

        Direction back = RelativeDirection.BACK.getRelative(front, upwards, flipped);
        Direction up = RelativeDirection.UP.getRelative(front, upwards, flipped);
        Direction right = RelativeDirection.RIGHT.getRelative(front, upwards, flipped);

        float baseX = 0.5f + back.getStepX() * 16.0f;
        float baseY = 0.5f + back.getStepY() * 16.0f;
        float baseZ = 0.5f + back.getStepZ() * 16.0f;

        int rx = right.getStepX(), ry = right.getStepY(), rz = right.getStepZ();
        int ux = up.getStepX(), uy = up.getStepY(), uz = up.getStepZ();
        int bx = back.getStepX(), by = back.getStepY(), bz = back.getStepZ();

        float sec = (machine.getOffsetTimer() + partialTick) * 0.05f;

        // 1. Inner Core High-Energy Pillar (Radiant White-Gold, r = 1.8f)
        float rot1 = sec * 0.75f;
        float r1 = 1.75f + 0.15f * Mth.sin(sec * 2.2f);
        renderCylinder(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                r1, 0.5f, 36.0f, rot1, 1.0f, 0.95f, 0.55f, 0.92f * fade);

        // 2. Outer Resonant Energy Sheath (Enveloping the 5x5 trunk, r = 3.65f, Quantum Cyan)
        float rot2 = -sec * 0.45f;
        float r2 = 3.65f + 0.20f * Mth.sin(sec * 1.5f);
        renderCylinder(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                r2, 0.5f, 35.0f, rot2, 0.05f, 0.88f, 1.0f, 0.75f * fade);

        // 3. Canopy Sky Beacon Flare (Shooting out above the crown into the heavens, h = 34.0f to 46.0f)
        float rotSky = sec * 1.1f;
        float rSky = 2.2f + 0.25f * Mth.sin(sec * 2.8f);
        renderCylinder(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                rSky, 34.0f, 46.0f, rotSky, 1.0f, 0.82f, 0.20f, 0.85f * fade);

        // 4. Ascending Imaginary Energy Waves (3 Continuous Travelling Wave Rings)
        for (int p = 0; p < 3; p++) {
            float phase = (sec * 0.35f + p * 0.3333f) % 1.0f;
            float hWave = 1.5f + phase * 32.0f;
            float rWave = 3.8f + phase * phase * 10.5f;
            float aWave = Mth.sin(phase * (float) Math.PI) * 0.85f * fade;
            float wWave = 0.55f;
            float wr = (p % 2 == 0) ? 1.0f : 0.08f;
            float wg = (p % 2 == 0) ? 0.85f : 0.92f;
            float wb = (p % 2 == 0) ? 0.15f : 1.0f;
            renderRing(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                    rWave, wWave, hWave, sec * (1.2f - p * 0.4f), wr, wg, wb, aWave);
        }

        // 5. Tier 1: Lower Trunk Torus (Gold Resonance Ring at h = 8.5f)
        float rT1 = 5.2f + 0.25f * Mth.sin(sec * 1.6f);
        renderRing(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                rT1, 0.65f, 8.5f, sec * 0.8f, 1.0f, 0.84f, 0.16f, 0.90f * fade);

        // 6. Tree Heart Singularity (Hollow Zone, h = 18.0f)
        float heartPulse = 0.35f * Mth.sin(sec * 3.0f);
        float sHeart1 = 2.6f + heartPulse;
        renderStarNode(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                0, 18.0f, 0, sHeart1, 1.0f, 0.95f, 0.45f, 0.95f * fade);
        float sHeart2 = 3.8f - heartPulse * 0.5f;
        renderStarNode(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                0, 18.0f, 0, sHeart2, 0.10f, 0.90f, 1.0f, 0.70f * fade);

        // 7. Tier 2: Mid Heart Giant Celestial Ring (Cyan Quantum Disk at h = 18.0f)
        float rT2 = 8.8f + 0.35f * Mth.sin(sec * 1.4f);
        renderRing(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                rT2, 0.85f, 18.0f, -sec * 0.6f, 0.05f, 0.92f, 1.0f, 0.88f * fade);

        // 8. Tier 3: Grand Canopy Crown Halo (Majestic Gold Celestial Ring at h = 28.5f)
        float rT3 = 14.5f + 0.50f * Mth.sin(sec * 1.1f);
        renderRing(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                rT3, 1.05f, 28.5f, sec * 0.4f, 1.0f, 0.80f, 0.12f, 0.92f * fade);

        // 9. Floating Canopy Star Cores (12 Brilliant Resonant Crystals Orbiting Canopy)
        for (int k = 0; k < 12; k++) {
            float angleK = (2.0f * (float) Math.PI * k) / 12.0f + sec * 0.25f;
            float rK = 13.5f + 1.2f * Mth.sin(sec * 0.8f + k * 0.5236f);
            float hK = 27.5f + 1.6f * Mth.sin(sec * 1.5f + k * 1.0472f);
            float uK = rK * Mth.cos(angleK);
            float vK = rK * Mth.sin(angleK);

            float sK = 1.35f + 0.15f * Mth.sin(sec * 2.5f + k);
            float nr = (k % 2 == 0) ? 1.0f : 0.08f;
            float ng = (k % 2 == 0) ? 0.86f : 0.94f;
            float nb = (k % 2 == 0) ? 0.16f : 1.0f;
            float na = 0.92f * fade;

            renderStarNode(buffer, mat, baseX, baseY, baseZ, rx, ry, rz, ux, uy, uz, bx, by, bz,
                    uK, hK, vK, sK, nr, ng, nb, na);
        }
    }

    private static void renderCylinder(VertexConsumer buffer, Matrix4f mat,
            float baseX, float baseY, float baseZ,
            int rx, int ry, int rz,
            int ux, int uy, int uz,
            int bx, int by, int bz,
            float radius, float yMin, float yMax, float rot,
            float r, float g, float b, float a) {
        float c = Mth.cos(rot), s = Mth.sin(rot);
        for (int i = 0; i < SEGMENTS; i++) {
            float u0 = radius * (COS_16[i] * c - SIN_16[i] * s);
            float v0 = radius * (SIN_16[i] * c + COS_16[i] * s);
            float u1 = radius * (COS_16[i + 1] * c - SIN_16[i + 1] * s);
            float v1 = radius * (SIN_16[i + 1] * c + COS_16[i + 1] * s);

            float x0 = baseX + rx * u0 + ux * yMin + bx * v0;
            float y0 = baseY + ry * u0 + uy * yMin + by * v0;
            float z0 = baseZ + rz * u0 + uz * yMin + bz * v0;

            float x1 = baseX + rx * u0 + ux * yMax + bx * v0;
            float y1 = baseY + ry * u0 + uy * yMax + by * v0;
            float z1 = baseZ + rz * u0 + uz * yMax + bz * v0;

            float x2 = baseX + rx * u1 + ux * yMax + bx * v1;
            float y2 = baseY + ry * u1 + uy * yMax + by * v1;
            float z2 = baseZ + rz * u1 + uz * yMax + bz * v1;

            float x3 = baseX + rx * u1 + ux * yMin + bx * v1;
            float y3 = baseY + ry * u1 + uy * yMin + by * v1;
            float z3 = baseZ + rz * u1 + uz * yMin + bz * v1;

            addQuad(buffer, mat, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a);
        }
    }

    private static void renderRing(VertexConsumer buffer, Matrix4f mat,
            float baseX, float baseY, float baseZ,
            int rx, int ry, int rz,
            int ux, int uy, int uz,
            int bx, int by, int bz,
            float radius, float width, float yCenter, float rot,
            float r, float g, float b, float a) {
        float c = Mth.cos(rot), s = Mth.sin(rot);
        float yBottom = yCenter - width * 0.5f;
        float yTop = yCenter + width * 0.5f;
        float rIn = radius - width * 0.5f;
        float rOut = radius + width * 0.5f;

        for (int i = 0; i < SEGMENTS; i++) {
            float cos0 = COS_16[i] * c - SIN_16[i] * s;
            float sin0 = SIN_16[i] * c + COS_16[i] * s;
            float cos1 = COS_16[i + 1] * c - SIN_16[i + 1] * s;
            float sin1 = SIN_16[i + 1] * c + COS_16[i + 1] * s;

            // 1. Vertical cylindrical band
            float u0 = radius * cos0, v0 = radius * sin0;
            float u1 = radius * cos1, v1 = radius * sin1;

            float x0 = baseX + rx * u0 + ux * yBottom + bx * v0;
            float y0 = baseY + ry * u0 + uy * yBottom + by * v0;
            float z0 = baseZ + rz * u0 + uz * yBottom + bz * v0;

            float x1 = baseX + rx * u0 + ux * yTop + bx * v0;
            float y1 = baseY + ry * u0 + uy * yTop + by * v0;
            float z1 = baseZ + rz * u0 + uz * yTop + bz * v0;

            float x2 = baseX + rx * u1 + ux * yTop + bx * v1;
            float y2 = baseY + ry * u1 + uy * yTop + by * v1;
            float z2 = baseZ + rz * u1 + uz * yTop + bz * v1;

            float x3 = baseX + rx * u1 + ux * yBottom + bx * v1;
            float y3 = baseY + ry * u1 + uy * yBottom + by * v1;
            float z3 = baseZ + rz * u1 + uz * yBottom + bz * v1;

            addQuad(buffer, mat, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a);

            // 2. Horizontal washer disc
            float hx0 = baseX + rx * (rIn * cos0) + ux * yCenter + bx * (rIn * sin0);
            float hy0 = baseY + ry * (rIn * cos0) + uy * yCenter + by * (rIn * sin0);
            float hz0 = baseZ + rz * (rIn * cos0) + uz * yCenter + bz * (rIn * sin0);

            float hx1 = baseX + rx * (rOut * cos0) + ux * yCenter + bx * (rOut * sin0);
            float hy1 = baseY + ry * (rOut * cos0) + uy * yCenter + by * (rOut * sin0);
            float hz1 = baseZ + rz * (rOut * cos0) + uz * yCenter + bz * (rOut * sin0);

            float hx2 = baseX + rx * (rOut * cos1) + ux * yCenter + bx * (rOut * sin1);
            float hy2 = baseY + ry * (rOut * cos1) + uy * yCenter + by * (rOut * sin1);
            float hz2 = baseZ + rz * (rOut * cos1) + uz * yCenter + bz * (rOut * sin1);

            float hx3 = baseX + rx * (rIn * cos1) + ux * yCenter + bx * (rIn * sin1);
            float hy3 = baseY + ry * (rIn * cos1) + uy * yCenter + by * (rIn * sin1);
            float hz3 = baseZ + rz * (rIn * cos1) + uz * yCenter + bz * (rIn * sin1);

            addQuad(buffer, mat, hx0, hy0, hz0, hx1, hy1, hz1, hx2, hy2, hz2, hx3, hy3, hz3, r, g, b, a);
        }
    }

    private static void renderStarNode(VertexConsumer buffer, Matrix4f mat,
            float baseX, float baseY, float baseZ,
            int rx, int ry, int rz,
            int ux, int uy, int uz,
            int bx, int by, int bz,
            float u, float h, float v, float s,
            float r, float g, float b, float a) {
        float cx = baseX + rx * u + ux * h + bx * v;
        float cy = baseY + ry * u + uy * h + by * v;
        float cz = baseZ + rz * u + uz * h + bz * v;

        // Plane 1: Right-Up
        addQuad(buffer, mat,
                cx - rx * s - ux * s, cy - ry * s - uy * s, cz - rz * s - uz * s,
                cx + rx * s - ux * s, cy + ry * s - uy * s, cz + rz * s - uz * s,
                cx + rx * s + ux * s, cy + ry * s + uy * s, cz + rz * s + uz * s,
                cx - rx * s + ux * s, cy - ry * s + uy * s, cz - rz * s + uz * s,
                r, g, b, a);

        // Plane 2: Back-Up
        addQuad(buffer, mat,
                cx - bx * s - ux * s, cy - by * s - uy * s, cz - bz * s - uz * s,
                cx + bx * s - ux * s, cy + by * s - uy * s, cz + bz * s - uz * s,
                cx + bx * s + ux * s, cy + by * s + uy * s, cz + bz * s + uz * s,
                cx - bx * s + ux * s, cy - by * s + uy * s, cz - bz * s + uz * s,
                r, g, b, a);

        // Plane 3: Right-Back (Horizontal plane)
        addQuad(buffer, mat,
                cx - rx * s - bx * s, cy - ry * s - by * s, cz - rz * s - bz * s,
                cx + rx * s - bx * s, cy + ry * s - by * s, cz + rz * s - bz * s,
                cx + rx * s + bx * s, cy + ry * s + by * s, cz + rz * s + bz * s,
                cx - rx * s + bx * s, cy - ry * s + by * s, cz - rz * s + bz * s,
                r, g, b, a);
    }

    private static void addQuad(VertexConsumer buffer, Matrix4f mat,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b, float a) {
        buffer.vertex(mat, x0, y0, z0).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
    }
}
