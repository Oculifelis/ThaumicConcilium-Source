package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.boss.FleshCrucibleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** TC4 FleshCrucibleTileRenderer: the liquid flesh surface, 0.3 + amount / 100 above the floor of the block. */
public final class FleshCrucibleRenderer implements BlockEntityRenderer<FleshCrucibleBlockEntity> {
    private static final ResourceLocation LIQUID = ThaumaturgicConcilium.id("textures/block/liquid_flesh.png");

    public FleshCrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FleshCrucibleBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        int flesh = be.flesh();
        if (flesh <= 0) {
            return;
        }
        float y = 0.3F + flesh / 100.0F;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(LIQUID));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        vertex(buffer, mat, pose, 0.0F, y, 0.0F, 0.0F, 0.0F, light);
        vertex(buffer, mat, pose, 0.0F, y, 1.0F, 0.0F, 1.0F, light);
        vertex(buffer, mat, pose, 1.0F, y, 1.0F, 1.0F, 1.0F, light);
        vertex(buffer, mat, pose, 1.0F, y, 0.0F, 1.0F, 0.0F, light);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(mat, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
