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
    public static final DynamicRenderType<WorkableElectricMultiblockMachine, TreeOfImaginaryRender> TYPE =
            new DynamicRenderType<>(CODEC);

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

    public TreeOfImaginaryRender() {}

    @Override
    public DynamicRenderType<WorkableElectricMultiblockMachine, TreeOfImaginaryRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(WorkableElectricMultiblockMachine machine, Vec3 cameraPos) {
        if (!machine.isFormed()) return false;
        if (!machine.getRecipeLogic().isWorking() && fadeTimer <= 0.0f) return false;

        Direction back = RelativeDirection.BACK.getRelative(machine.getFrontFacing(), machine.getUpwardsFacing(), machine.isFlipped());
        double cx = machine.getPos().getX() + 0.5 + back.getStepX() * 16.0;
        double cy = machine.getPos().getY() + 0.5 + 17.0;
        double cz = machine.getPos().getZ() + 0.5 + back.getStepZ() * 16.0;

        double dx = cameraPos.x - cx;
        double dy = cameraPos.y - cy;
        double dz = cameraPos.z - cz;
        return (dx * dx + dy * dy + dz * dz) <= (64.0 * 64.0);
    }

    @Override
    public boolean shouldRenderOffScreen(WorkableElectricMultiblockMachine machine) {
        return machine.getRecipeLogic().isWorking() || fadeTimer > 0.0f;
    }

    @Override
    public AABB getRenderBoundingBox(WorkableElectricMultiblockMachine machine) {
        Direction back = RelativeDirection.BACK.getRelative(machine.getFrontFacing(), machine.getUpwardsFacing(), machine.isFlipped());
        BlockPos centerPos = machine.getPos().offset(back.getStepX() * 16, 17, back.getStepZ() * 16);
        return new AABB(centerPos).inflate(18.0);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(WorkableElectricMultiblockMachine machine, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        boolean working = machine.getRecipeLogic().isWorking();
        float deltaTicks = Minecraft.getInstance().getDeltaFrameTime();
        if (working) {
            fadeTimer = Math.min(MAX_FADE, fadeTimer + deltaTicks);
        } else {
            fadeTimer = Math.max(0.0f, fadeTimer - deltaTicks);
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
        int ux = up.getStepX(),    uy = up.getStepY(),    uz = up.getStepZ();
        int bx = back.getStepX(),  by = back.getStepY(),  bz = back.getStepZ();

        float sec = (machine.getOffsetTimer() + partialTick) * 0.05f;

        // 1. Central Core Beam (White-Gold Radiant Inner Column)
        float rot1 = sec * 0.6f;
        float c1 = Mth.cos(rot1), s1 = Mth.sin(rot1);
        float r1 = 0.45f;
        float cr = 1.0f, cg = 0.96f, cb = 0.88f, ca = 0.70f * fade;
        for (int i = 0; i < SEGMENTS; i++) {
            float u0 = r1 * (COS_16[i] * c1 - SIN_16[i] * s1);
            float v0 = r1 * (SIN_16[i] * c1 + COS_16[i] * s1);
            float u1 = r1 * (COS_16[i + 1] * c1 - SIN_16[i + 1] * s1);
            float v1 = r1 * (SIN_16[i + 1] * c1 + COS_16[i + 1] * s1);

            float x0 = baseX + rx * u0 + ux * 1.0f + bx * v0;
            float y0 = baseY + ry * u0 + uy * 1.0f + by * v0;
            float z0 = baseZ + rz * u0 + uz * 1.0f + bz * v0;

            float x1 = baseX + rx * u0 + ux * 33.0f + bx * v0;
            float y1 = baseY + ry * u0 + uy * 33.0f + by * v0;
            float z1 = baseZ + rz * u0 + uz * 33.0f + bz * v0;

            float x2 = baseX + rx * u1 + ux * 33.0f + bx * v1;
            float y2 = baseY + ry * u1 + uy * 33.0f + by * v1;
            float z2 = baseZ + rz * u1 + uz * 33.0f + bz * v1;

            float x3 = baseX + rx * u1 + ux * 1.0f + bx * v1;
            float y3 = baseY + ry * u1 + uy * 1.0f + by * v1;
            float z3 = baseZ + rz * u1 + uz * 1.0f + bz * v1;

            addQuad(buffer, mat, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, cr, cg, cb, ca);
        }

        // 2. Outer Resonant Sheath (Azure Cyan Breathing Cylinder)
        float rot2 = -sec * 0.45f;
        float c2 = Mth.cos(rot2), s2 = Mth.sin(rot2);
        float r2 = 0.88f + 0.08f * Mth.sin(sec * 1.2f);
        float or = 0.15f, og = 0.75f, ob = 1.0f, oa = 0.42f * fade;
        for (int i = 0; i < SEGMENTS; i++) {
            float u0 = r2 * (COS_16[i] * c2 - SIN_16[i] * s2);
            float v0 = r2 * (SIN_16[i] * c2 + COS_16[i] * s2);
            float u1 = r2 * (COS_16[i + 1] * c2 - SIN_16[i + 1] * s2);
            float v1 = r2 * (SIN_16[i + 1] * c2 + COS_16[i + 1] * s2);

            float x0 = baseX + rx * u0 + ux * 1.5f + bx * v0;
            float y0 = baseY + ry * u0 + uy * 1.5f + by * v0;
            float z0 = baseZ + rz * u0 + uz * 1.5f + bz * v0;

            float x1 = baseX + rx * u0 + ux * 32.5f + bx * v0;
            float y1 = baseY + ry * u0 + uy * 32.5f + by * v0;
            float z1 = baseZ + rz * u0 + uz * 32.5f + bz * v0;

            float x2 = baseX + rx * u1 + ux * 32.5f + bx * v1;
            float y2 = baseY + ry * u1 + uy * 32.5f + by * v1;
            float z2 = baseZ + rz * u1 + uz * 32.5f + bz * v1;

            float x3 = baseX + rx * u1 + ux * 1.5f + bx * v1;
            float y3 = baseY + ry * u1 + uy * 1.5f + by * v1;
            float z3 = baseZ + rz * u1 + uz * 1.5f + bz * v1;

            addQuad(buffer, mat, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, or, og, ob, oa);
        }

        // 3. Tree Heart Resonance Inner Halo (Gold Ring)
        float rotH1 = sec * 0.8f;
        float ch1 = Mth.cos(rotH1), sh1 = Mth.sin(rotH1);
        float rInner = 2.4f;
        float w1 = 0.25f;
        float hr1 = 1.0f, hg1 = 0.82f, hb1 = 0.30f, ha1 = 0.65f * fade;
        for (int i = 0; i < SEGMENTS; i++) {
            float cos0 = COS_16[i] * ch1 - SIN_16[i] * sh1;
            float sin0 = SIN_16[i] * ch1 + COS_16[i] * sh1;
            float cos1 = COS_16[i + 1] * ch1 - SIN_16[i + 1] * sh1;
            float sin1 = SIN_16[i + 1] * ch1 + COS_16[i + 1] * sh1;

            // Vertical cylinder band
            float u0 = rInner * cos0, v0 = rInner * sin0;
            float u1 = rInner * cos1, v1 = rInner * sin1;

            float x0 = baseX + rx * u0 + ux * (16.5f - w1) + bx * v0;
            float y0 = baseY + ry * u0 + uy * (16.5f - w1) + by * v0;
            float z0 = baseZ + rz * u0 + uz * (16.5f - w1) + bz * v0;

            float x1 = baseX + rx * u0 + ux * (16.5f + w1) + bx * v0;
            float y1 = baseY + ry * u0 + uy * (16.5f + w1) + by * v0;
            float z1 = baseZ + rz * u0 + uz * (16.5f + w1) + bz * v0;

            float x2 = baseX + rx * u1 + ux * (16.5f + w1) + bx * v1;
            float y2 = baseY + ry * u1 + uy * (16.5f + w1) + by * v1;
            float z2 = baseZ + rz * u1 + uz * (16.5f + w1) + bz * v1;

            float x3 = baseX + rx * u1 + ux * (16.5f - w1) + bx * v1;
            float y3 = baseY + ry * u1 + uy * (16.5f - w1) + by * v1;
            float z3 = baseZ + rz * u1 + uz * (16.5f - w1) + bz * v1;

            addQuad(buffer, mat, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, hr1, hg1, hb1, ha1);

            // Horizontal washer disc
            float rin = rInner - w1;
            float rout = rInner + w1;

            float hx0 = baseX + rx * (rin * cos0) + ux * 16.5f + bx * (rin * sin0);
            float hy0 = baseY + ry * (rin * cos0) + uy * 16.5f + by * (rin * sin0);
            float hz0 = baseZ + rz * (rin * cos0) + uz * 16.5f + bz * (rin * sin0);

            float hx1 = baseX + rx * (rout * cos0) + ux * 16.5f + bx * (rout * sin0);
            float hy1 = baseY + ry * (rout * cos0) + uy * 16.5f + by * (rout * sin0);
            float hz1 = baseZ + rz * (rout * cos0) + uz * 16.5f + bz * (rout * sin0);

            float hx2 = baseX + rx * (rout * cos1) + ux * 16.5f + bx * (rout * sin1);
            float hy2 = baseY + ry * (rout * cos1) + uy * 16.5f + by * (rout * sin1);
            float hz2 = baseZ + rz * (rout * cos1) + uz * 16.5f + bz * (rout * sin1);

            float hx3 = baseX + rx * (rin * cos1) + ux * 16.5f + bx * (rin * sin1);
            float hy3 = baseY + ry * (rin * cos1) + uy * 16.5f + by * (rin * sin1);
            float hz3 = baseZ + rz * (rin * cos1) + uz * 16.5f + bz * (rin * sin1);

            addQuad(buffer, mat, hx0, hy0, hz0, hx1, hy1, hz1, hx2, hy2, hz2, hx3, hy3, hz3, hr1, hg1, hb1, ha1);
        }

        // 4. Tree Heart Resonance Outer Halo (Azure Cyan Breathing Ring)
        float rotH2 = -sec * 0.5f;
        float ch2 = Mth.cos(rotH2), sh2 = Mth.sin(rotH2);
        float rOuter = 4.2f + 0.30f * Mth.sin(sec * 1.5f);
        float w2 = 0.30f;
        float hr2 = 0.20f, hg2 = 0.80f, hb2 = 1.0f, ha2 = 0.50f * fade;
        for (int i = 0; i < SEGMENTS; i++) {
            float cos0 = COS_16[i] * ch2 - SIN_16[i] * sh2;
            float sin0 = SIN_16[i] * ch2 + COS_16[i] * sh2;
            float cos1 = COS_16[i + 1] * ch2 - SIN_16[i + 1] * sh2;
            float sin1 = SIN_16[i + 1] * ch2 + COS_16[i + 1] * sh2;

            // Vertical cylinder band
            float u0 = rOuter * cos0, v0 = rOuter * sin0;
            float u1 = rOuter * cos1, v1 = rOuter * sin1;

            float x0 = baseX + rx * u0 + ux * (16.5f - w2) + bx * v0;
            float y0 = baseY + ry * u0 + uy * (16.5f - w2) + by * v0;
            float z0 = baseZ + rz * u0 + uz * (16.5f - w2) + bz * v0;

            float x1 = baseX + rx * u0 + ux * (16.5f + w2) + bx * v0;
            float y1 = baseY + ry * u0 + uy * (16.5f + w2) + by * v0;
            float z1 = baseZ + rz * u0 + uz * (16.5f + w2) + bz * v0;

            float x2 = baseX + rx * u1 + ux * (16.5f + w2) + bx * v1;
            float y2 = baseY + ry * u1 + uy * (16.5f + w2) + by * v1;
            float z2 = baseZ + rz * u1 + uz * (16.5f + w2) + bz * v1;

            float x3 = baseX + rx * u1 + ux * (16.5f - w2) + bx * v1;
            float y3 = baseY + ry * u1 + uy * (16.5f - w2) + by * v1;
            float z3 = baseZ + rz * u1 + uz * (16.5f - w2) + bz * v1;

            addQuad(buffer, mat, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, hr2, hg2, hb2, ha2);

            // Horizontal washer disc
            float rin = rOuter - w2;
            float rout = rOuter + w2;

            float hx0 = baseX + rx * (rin * cos0) + ux * 16.5f + bx * (rin * sin0);
            float hy0 = baseY + ry * (rin * cos0) + uy * 16.5f + by * (rin * sin0);
            float hz0 = baseZ + rz * (rin * cos0) + uz * 16.5f + bz * (rin * sin0);

            float hx1 = baseX + rx * (rout * cos0) + ux * 16.5f + bx * (rout * sin0);
            float hy1 = baseY + ry * (rout * cos0) + uy * 16.5f + by * (rout * sin0);
            float hz1 = baseZ + rz * (rout * cos0) + uz * 16.5f + bz * (rout * sin0);

            float hx2 = baseX + rx * (rout * cos1) + ux * 16.5f + bx * (rout * sin1);
            float hy2 = baseY + ry * (rout * cos1) + uy * 16.5f + by * (rout * sin1);
            float hz2 = baseZ + rz * (rout * cos1) + uz * 16.5f + bz * (rout * sin1);

            float hx3 = baseX + rx * (rin * cos1) + ux * 16.5f + bx * (rin * sin1);
            float hy3 = baseY + ry * (rin * cos1) + uy * 16.5f + by * (rin * sin1);
            float hz3 = baseZ + rz * (rin * cos1) + uz * 16.5f + bz * (rin * sin1);

            addQuad(buffer, mat, hx0, hy0, hz0, hx1, hy1, hz1, hx2, hy2, hz2, hx3, hy3, hz3, hr2, hg2, hb2, ha2);
        }

        // 5. Canopy Resonance Nodes (8 Floating Star Cores)
        float rNode = 10.0f;
        float s = 0.40f;
        for (int k = 0; k < 8; k++) {
            float hk = 25.5f + 0.35f * Mth.sin(sec * 2.0f + k * 0.7853982f);
            int idx = (k * 2) % SEGMENTS;
            float uk = rNode * COS_16[idx];
            float vk = rNode * SIN_16[idx];

            float nr = (k % 2 == 0) ? 0.25f : 1.0f;
            float ng = (k % 2 == 0) ? 0.85f : 0.85f;
            float nb = (k % 2 == 0) ? 1.0f  : 0.35f;
            float na = 0.65f * fade;

            float cx = baseX + rx * uk + ux * hk + bx * vk;
            float cy = baseY + ry * uk + uy * hk + by * vk;
            float cz = baseZ + rz * uk + uz * hk + bz * vk;

            // Quad A (Right-Up plane)
            float ax0 = cx - rx * s - ux * s, ay0 = cy - ry * s - uy * s, az0 = cz - rz * s - uz * s;
            float ax1 = cx + rx * s - ux * s, ay1 = cy + ry * s - uy * s, az1 = cz + rz * s - uz * s;
            float ax2 = cx + rx * s + ux * s, ay2 = cy + ry * s + uy * s, az2 = cz + rz * s + uz * s;
            float ax3 = cx - rx * s + ux * s, ay3 = cy - ry * s + uy * s, az3 = cz - rz * s + uz * s;
            addQuad(buffer, mat, ax0, ay0, az0, ax1, ay1, az1, ax2, ay2, az2, ax3, ay3, az3, nr, ng, nb, na);

            // Quad B (Back-Up plane)
            float bx0 = cx - bx * s - ux * s, by0 = cy - by * s - uy * s, bz0 = cz - bz * s - uz * s;
            float bx1 = cx + bx * s - ux * s, by1 = cy + by * s - uy * s, bz1 = cz + bz * s - uz * s;
            float bx2 = cx + bx * s + ux * s, by2 = cy + by * s + uy * s, bz2 = cz + bz * s + uz * s;
            float bx3 = cx - bx * s + ux * s, by3 = cy - by * s + uy * s, bz3 = cz - bz * s + uz * s;
            addQuad(buffer, mat, bx0, by0, bz0, bx1, by1, bz1, bx2, by2, bz2, bx3, by3, bz3, nr, ng, nb, na);
        }
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
