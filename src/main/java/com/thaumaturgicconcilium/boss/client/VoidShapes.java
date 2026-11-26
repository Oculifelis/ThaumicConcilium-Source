package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Boxes of End-portal sky, the 26.1 stand-in for TC4 Concilium's "end shader" (its own GLSL starfield bound to the
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
    static void spinningBox(SubmitNodeCollector collector, PoseStack poseStack, float sx, float sy, float sz, float degrees) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateAxis(degrees * ((float) Math.PI / 180.0F), DIAGONAL));
        box(collector, poseStack, sx, sy, sz);
        poseStack.popPose();
    }

    /** A box of the given size (blocks) centred on the pose origin. */
    static void box(SubmitNodeCollector collector, PoseStack poseStack, float sx, float sy, float sz) {
        poseStack.pushPose();
        poseStack.scale(sx, sy, sz);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        AbstractEndPortalRenderer.submitSpecial(RenderTypes.endPortal(), poseStack, collector);
        poseStack.popPose();
    }

    /** A whole void biped standing on the pose origin, {@code scale} of player size, turned {@code yaw} degrees. */
    static void biped(SubmitNodeCollector collector, PoseStack poseStack, float scale, float yaw) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        float px = scale / 16.0F;
        for (float[] part : BIPED) {
            poseStack.pushPose();
            poseStack.translate(part[0] * px, part[1] * px, part[2] * px);
            box(collector, poseStack, part[3] * px, part[4] * px, part[5] * px);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
