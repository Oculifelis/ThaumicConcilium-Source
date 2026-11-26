package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Boxes of End-portal sky, the stand-in for TC4 Concilium's "end shader" (its own GLSL starfield bound to the
 * player's yaw and pitch). Used by the Peeler of Matter, the Protolimbs and the Protobody.
 */
final class VoidShapes {
    /** TC4 spun its void shapes about the (1, 0, 1) diagonal. */
    private static final Vector3f DIAGONAL = new Vector3f(0.7071F, 0.0F, 0.7071F);

    /** Biped parts in TC4's limb order (head, left arm, right arm, body, left leg, right leg): centre xyz, size xyz, in model pixels from the feet. */
    static final float[][] BIPED = {
            {0, 28, 0, 8, 8, 8},
            {6, 18, 0, 4, 12, 4},
            {-6, 18, 0, 4, 12, 4},
            {0, 18, 0, 8, 12, 4},
            {2, 6, 0, 4, 12, 4},
            {-2, 6, 0, 4, 12, 4},
    };

    private VoidShapes() {
    }

    /** A box of the given size (blocks) centred on the pose origin, turned {@code degrees} about the diagonal. */
    static void spinningBox(MultiBufferSource buffers, PoseStack poseStack, float sx, float sy, float sz, float degrees) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateAxis(degrees * ((float) Math.PI / 180.0F), DIAGONAL));
        box(buffers, poseStack, sx, sy, sz);
        poseStack.popPose();
    }

    /** A box of the given size (blocks) centred on the pose origin. */
    static void box(MultiBufferSource buffers, PoseStack poseStack, float sx, float sy, float sz) {
        poseStack.pushPose();
        poseStack.scale(sx, sy, sz);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        cube(buffers.getBuffer(RenderType.endPortal()), poseStack.last().pose());
        poseStack.popPose();
    }

    /** A whole void biped standing on the pose origin, {@code scale} of player size, turned {@code yaw} degrees. */
    static void biped(MultiBufferSource buffers, PoseStack poseStack, float scale, float yaw) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        float px = scale / 16.0F;
        for (float[] part : BIPED) {
            poseStack.pushPose();
            poseStack.translate(part[0] * px, part[1] * px, part[2] * px);
            box(buffers, poseStack, part[3] * px, part[4] * px, part[5] * px);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The unit cube, every face wound outward (as TheEndPortalRenderer draws its faces). */
    private static void cube(VertexConsumer b, Matrix4f m) {
        face(b, m, 0, 1, 0, 1, 1, 1, 1, 1);
        face(b, m, 0, 1, 1, 0, 0, 0, 0, 0);
        face(b, m, 1, 1, 1, 0, 0, 1, 1, 0);
        face(b, m, 0, 0, 0, 1, 0, 1, 1, 0);
        face(b, m, 0, 1, 0, 0, 0, 0, 1, 1);
        face(b, m, 0, 1, 1, 1, 1, 1, 0, 0);
    }

    private static void face(VertexConsumer b, Matrix4f m, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        b.addVertex(m, x0, y0, z0);
        b.addVertex(m, x1, y0, z1);
        b.addVertex(m, x1, y1, z2);
        b.addVertex(m, x0, y1, z3);
    }
}
