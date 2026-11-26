package com.thaumaturgicconcilium.compat.witchery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.witchery.BurnedWitchEntity;
import dev.sterner.witchery.client.model.BroomEntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Burned Witch: TC4's biped in the riding pose (legs forward, arms raised a little) on the original's CC0 sheet
 * (burned_witch.png), seated on Witchery 0.5's own broom model and texture. TC4 tinted its broom brown because the old
 * broom texture was grey; Witchery 0.5's broom texture is already coloured, so it is drawn as it is.
 */
public class BurnedWitchRenderer extends HumanoidMobRenderer<BurnedWitchEntity, BurnedWitchRenderer.Model> {
    private static final ResourceLocation TEXTURE = ThaumaturgicConcilium.id("textures/entity/burned_witch.png");

    public BurnedWitchRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(WitcheryClient.BIPED)), 0.5F);
        ModelPart broom = null;
        try {
            broom = context.bakeLayer(BroomEntityModel.Companion.getLAYER_LOCATION());
        } catch (RuntimeException e) {
            ThaumaturgicConcilium.LOG.warn("Witchery broom model layer missing; the Burned Witch flies without a broom", e);
        }
        if (broom != null) {
            addLayer(new BroomLayer(this, broom));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BurnedWitchEntity entity) {
        return TEXTURE;
    }

    /** The plain biped, always in the riding pose. */
    public static class Model extends HumanoidModel<BurnedWitchEntity> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(BurnedWitchEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
            riding = true;
            super.setupAnim(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch);
        }
    }

    /**
     * Witchery's broom under the witch. Witchery draws its broom entity unflipped, 1 block down; this layer undoes the
     * living-entity flip and offset and draws it the same way, raised so the stick sits under her hips.
     */
    static final class BroomLayer extends RenderLayer<BurnedWitchEntity, Model> {
        private static final ResourceLocation BROOM = ResourceLocation.fromNamespaceAndPath("witchery", "textures/entity/broom.png");
        private static final float LIFT = 0.4F;
        private final ModelPart broom;

        BroomLayer(RenderLayerParent<BurnedWitchEntity, Model> parent, ModelPart broom) {
            super(parent);
            this.broom = broom;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, BurnedWitchEntity entity, float limbSwing,
                float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            pose.pushPose();
            pose.translate(0.0F, 1.501F, 0.0F);
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(0.0F, -1.0F + LIFT, 0.0F);
            broom.render(pose, buffers.getBuffer(RenderType.entityTranslucent(BROOM)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
