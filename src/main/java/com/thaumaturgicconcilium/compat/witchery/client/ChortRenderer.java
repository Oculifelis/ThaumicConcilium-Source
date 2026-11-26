package com.thaumaturgicconcilium.compat.witchery.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.witchery.ChortEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The Chort: TC4's biped with a tail, on the original's CC0 sheet (chort.png). TC4 never drew its hat layer. */
public class ChortRenderer extends HumanoidMobRenderer<ChortEntity, ChortRenderer.Model> {
    private static final ResourceLocation TEXTURE = ThaumaturgicConcilium.id("textures/entity/chort.png");

    public ChortRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(WitcheryClient.CHORT)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ChortEntity entity) {
        return TEXTURE;
    }

    public static class Model extends HumanoidModel<ChortEntity> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(ChortEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch);
            hat.visible = false;
        }
    }
}
