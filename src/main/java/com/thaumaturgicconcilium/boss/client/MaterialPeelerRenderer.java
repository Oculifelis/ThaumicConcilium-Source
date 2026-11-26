package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.MaterialPeelerEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** TC4 MaterialPeelerRenderer: a one-block cube of starfield turning slowly about its diagonal. */
public final class MaterialPeelerRenderer extends EntityRenderer<MaterialPeelerEntity, MaterialPeelerRenderer.State> {
    public MaterialPeelerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static final class State extends EntityRenderState {
        float rotation;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MaterialPeelerEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.rotation = (entity.innerRotation + partialTicks) * 0.5F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        VoidShapes.spinningBox(collector, poseStack, 1.0F, 1.0F, 1.0F, state.rotation);
        poseStack.popPose();
    }
}
