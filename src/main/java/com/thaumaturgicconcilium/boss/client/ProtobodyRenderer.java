package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.ProtobodyItemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * A dropped Protobody, as TC4's ProtobodyRenderer drew it on the ground: a small whole body of starfield (here half
 * player size), turning slowly and bobbing like any item.
 */
public final class ProtobodyRenderer extends EntityRenderer<ProtobodyItemEntity, ProtobodyRenderer.State> {
    public ProtobodyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.3F;
    }

    public static final class State extends EntityRenderState {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F + Mth.sin(state.ageInTicks / 10.0F) * 0.05F, 0.0F);
        VoidShapes.biped(collector, poseStack, 0.5F, state.ageInTicks * 2.0F);
        poseStack.popPose();
    }
}
