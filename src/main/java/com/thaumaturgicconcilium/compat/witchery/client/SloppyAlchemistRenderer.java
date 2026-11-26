package com.thaumaturgicconcilium.compat.witchery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.witchery.SloppyAlchemistEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Sloppy Alchemist (the original's CC0 excsi.png sheet). Ridden by a Chort it is drawn with TC4's
 * {@code SloppyAlchemistModel}: on all fours, body flat, the four limbs swinging like a quadruped's. Free, it is TC4's
 * puffed-out {@code ModelBiped(0.5F)}.
 */
public class SloppyAlchemistRenderer extends MobRenderer<SloppyAlchemistEntity, SloppyAlchemistRenderer.State, SloppyAlchemistRenderer.Model> {
    private static final Identifier TEXTURE = ThaumaturgicConcilium.id("textures/entity/sloppy_alchemist.png");
    private final Model free;
    private final Model ridden;

    public SloppyAlchemistRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(WitcheryClient.BIPED_PUFFED), false), 0.5F);
        this.free = model;
        this.ridden = new Model(context.bakeLayer(WitcheryClient.BIPED), true);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SloppyAlchemistEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ridden = entity.isRidden();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        model = state.ridden ? ridden : free;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    public static class State extends HumanoidRenderState {
        public boolean ridden;
    }

    public static class Model extends HumanoidModel<State> {
        private final boolean quadruped;

        public Model(ModelPart root, boolean quadruped) {
            super(root);
            this.quadruped = quadruped;
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            if (!quadruped) {
                return;
            }
            float pos = state.walkAnimationPos;
            float speed = state.walkAnimationSpeed;
            head.setPos(0.0F, 13.0F, -9.0F);
            head.xRot = state.xRot * Mth.DEG_TO_RAD;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            head.zRot = 0.0F;
            hat.setPos(0.0F, 13.0F, -9.0F);
            hat.xRot = 0.0F;
            hat.yRot = 0.0F;
            hat.zRot = 0.0F;
            body.setPos(0.0F, 13.0F, -5.0F);
            body.xRot = (float) Math.PI / 2.0F;
            body.yRot = 0.0F;
            body.zRot = 0.0F;
            // TC4 arm boxes start at y 0 (the biped's at -2): the pivots move down 2 to keep the shapes in place.
            rightArm.setPos(-5.0F, 14.0F, -5.0F);
            leftArm.setPos(5.0F, 14.0F, -5.0F);
            rightLeg.setPos(-2.0F, 12.0F, 5.0F);
            leftLeg.setPos(2.0F, 12.0F, 5.0F);
            rightArm.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
            leftArm.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            rightLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            leftLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
            for (ModelPart limb : new ModelPart[]{rightArm, leftArm, rightLeg, leftLeg}) {
                limb.yRot = 0.0F;
                limb.zRot = 0.0F;
            }
        }
    }
}
