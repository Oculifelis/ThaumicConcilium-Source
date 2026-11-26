package com.thaumaturgicconcilium.compat.witchery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.witchery.BurnedWitchEntity;
import dev.sterner.witchery.client.model.BroomEntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * The Burned Witch: TC4's biped in the riding pose (legs forward, arms raised a little) on the original's CC0 sheet
 * (burned_witch.png), seated on Witchery 0.5's own broom model and texture. TC4 tinted its broom brown because the old
 * broom texture was grey; Witchery 0.5's broom texture is already coloured, so it is drawn as it is.
 */
public class BurnedWitchRenderer extends HumanoidMobRenderer<BurnedWitchEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier TEXTURE = ThaumaturgicConcilium.id("textures/entity/burned_witch.png");

    public BurnedWitchRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(WitcheryClient.BIPED)), 0.5F);
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
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(BurnedWitchEntity entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isPassenger = true;
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }

    /**
     * Witchery's broom under the witch. Witchery draws its broom entity unflipped, 1 block down; this layer undoes the
     * living-entity flip and offset and draws it the same way, raised so the stick sits under her hips.
     */
    static final class BroomLayer extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
        private static final Identifier BROOM = Identifier.fromNamespaceAndPath("witchery", "textures/entity/broom.png");
        private static final float LIFT = 0.4F;
        private final ModelPart broom;

        BroomLayer(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent, ModelPart broom) {
            super(parent);
            this.broom = broom;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yRot, float xRot) {
            if (state.isInvisible) {
                return;
            }
            pose.pushPose();
            pose.translate(0.0F, 1.501F, 0.0F);
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(0.0F, -1.0F + LIFT, 0.0F);
            collector.submitModelPart(broom, pose, RenderTypes.entityTranslucent(BROOM), light, OverlayTexture.NO_OVERLAY, (net.minecraft.client.renderer.texture.TextureAtlasSprite) null);
            pose.popPose();
        }
    }
}
