package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.ProtolimbEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** TC4 ProtolimbRenderer: one biped part (by the limb's kind) made of starfield, tumbling about its diagonal. */
public final class ProtolimbRenderer extends EntityRenderer<ProtolimbEntity> {
    public ProtolimbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ProtolimbEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
        float[] part = VoidShapes.BIPED[Math.floorMod(entity.kind(), VoidShapes.BIPED.length)];
        float px = 1.0F / 16.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        VoidShapes.spinningBox(buffers, poseStack, part[3] * px, part[4] * px, part[5] * px, entity.innerRotation + partialTicks);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ProtolimbEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
