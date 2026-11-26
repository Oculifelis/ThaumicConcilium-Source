package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.ProtolimbEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** TC4 ProtolimbRenderer: one biped part (by the limb's kind) made of starfield, tumbling about its diagonal. */
public final class ProtolimbRenderer extends EntityRenderer<ProtolimbEntity, ProtolimbRenderer.State> {
    public ProtolimbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static final class State extends EntityRenderState {
        int kind;
        float rotation;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ProtolimbEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.kind = entity.kind();
        state.rotation = entity.innerRotation + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        float[] part = VoidShapes.BIPED[Math.floorMod(state.kind, VoidShapes.BIPED.length)];
        float px = 1.0F / 16.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight * 0.5F, 0.0F);
        VoidShapes.spinningBox(collector, poseStack, part[3] * px, part[4] * px, part[5] * px, state.rotation);
        poseStack.popPose();
    }
}
