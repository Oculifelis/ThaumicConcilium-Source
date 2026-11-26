package com.thaumaturgicconcilium.compat.horizons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.compat.horizons.RedPoweredMindEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** TC4 Concilium {@code RedPoweredMindRender}: the brain with its cord hanging below, facing a fixed way. */
public final class RedPoweredMindRenderer extends EntityRenderer<RedPoweredMindEntity, EntityRenderState> {
    private final RedPoweredMindModel model;

    public RedPoweredMindRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new RedPoweredMindModel(context.bakeLayer(HorizonsClient.RED_POWERED_MIND));
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        model.submit(poseStack, collector, state.lightCoords, state.ageInTicks, 0.0F, true);
        super.submit(state, poseStack, collector, camera);
    }
}
