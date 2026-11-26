package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.ProtobodyItemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A dropped Protobody, as TC4's ProtobodyRenderer drew it on the ground: a small whole body of starfield (here half
 * player size), turning slowly and bobbing like any item.
 */
public final class ProtobodyRenderer extends EntityRenderer<ProtobodyItemEntity> {
    public ProtobodyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(ProtobodyItemEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
        float age = entity.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F + Mth.sin(age / 10.0F) * 0.05F, 0.0F);
        VoidShapes.biped(buffers, poseStack, 0.5F, age * 2.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ProtobodyItemEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
