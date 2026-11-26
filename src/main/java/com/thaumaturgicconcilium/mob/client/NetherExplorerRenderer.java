package com.thaumaturgicconcilium.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.NetherExplorerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** The Nether Explorer lying flat on the lava, as the original drew it (a still biped turned 90 degrees). */
public class NetherExplorerRenderer extends MobRenderer<NetherExplorerEntity, TCBipedState, TCBipedModel> {
    private static final Identifier TEXTURE = ThaumaturgicConcilium.id("textures/entity/nether_explorer.png");

    public NetherExplorerRenderer(EntityRendererProvider.Context context) {
        super(context, new TCBipedModel(context.bakeLayer(TCMobsClient.BIPED)), 0.0F);
    }

    @Override
    public TCBipedState createRenderState() {
        return new TCBipedState();
    }

    @Override
    public void extractRenderState(NetherExplorerEntity entity, TCBipedState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        state.xRot = 0.0F;
        state.yRot = 0.0F;
    }

    @Override
    protected void setupRotations(TCBipedState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.translate(0.0F, 0.13F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, -0.9F, 0.0F);
    }

    @Override
    public Identifier getTextureLocation(TCBipedState state) {
        return TEXTURE;
    }
}
