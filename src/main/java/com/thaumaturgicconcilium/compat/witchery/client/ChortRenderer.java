package com.thaumaturgicconcilium.compat.witchery.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.witchery.ChortEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** The Chort: TC4's biped with a tail, on the original's CC0 sheet (chort.png). TC4 never drew its hat layer. */
public class ChortRenderer extends HumanoidMobRenderer<ChortEntity, HumanoidRenderState, ChortRenderer.Model> {
    private static final Identifier TEXTURE = ThaumaturgicConcilium.id("textures/entity/chort.png");

    public ChortRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(WitcheryClient.CHORT)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }

    public static class Model extends HumanoidModel<HumanoidRenderState> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(HumanoidRenderState state) {
            super.setupAnim(state);
            hat.visible = false;
        }
    }
}
