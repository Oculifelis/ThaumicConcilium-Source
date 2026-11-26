package com.thaumaturgicconcilium.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.mob.TaintSpiderSmartEntity;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The Enslaved Taint Spider wears Thaumaturge's taint crawler body (the original used TC4's taint spider renderer). */
public class TaintSpiderSmartRenderer extends MobRenderer<TaintSpiderSmartEntity, SilverfishModel<TaintSpiderSmartEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumaturge:textures/entity/taint_crawler.png");
    private static final float SCALE = 0.7F;

    public TaintSpiderSmartRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    protected void scale(TaintSpiderSmartEntity entity, PoseStack poseStack, float partialTicks) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(TaintSpiderSmartEntity entity) {
        return TEXTURE;
    }
}
