package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.MaterialPeelerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** TC4 MaterialPeelerRenderer: a one-block cube of starfield turning slowly about its diagonal. */
public final class MaterialPeelerRenderer extends EntityRenderer<MaterialPeelerEntity> {
    public MaterialPeelerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(MaterialPeelerEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        VoidShapes.spinningBox(buffers, poseStack, 1.0F, 1.0F, 1.0F, (entity.innerRotation + partialTicks) * 0.5F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MaterialPeelerEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
