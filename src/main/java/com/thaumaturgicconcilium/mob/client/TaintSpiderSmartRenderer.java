package com.thaumaturgicconcilium.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.mob.TaintSpiderSmartEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.silverfish.SilverfishModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** The Enslaved Taint Spider wears Thaumaturge's taint crawler body (the original used TC4's taint spider renderer). */
public class TaintSpiderSmartRenderer extends MobRenderer<TaintSpiderSmartEntity, LivingEntityRenderState, SilverfishModel> {
    private static final Identifier TEXTURE = Identifier.parse("thaumaturge:textures/entity/taint_crawler.png");
    private static final float SCALE = 0.7F;

    public TaintSpiderSmartRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
