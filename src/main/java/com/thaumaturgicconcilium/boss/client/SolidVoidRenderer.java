package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.SolidVoidBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** TC4 SolidVoidTileRenderer: a full block of starfield on each face open to the air. */
public final class SolidVoidRenderer extends AbstractEndPortalRenderer<SolidVoidBlockEntity, EndPortalRenderState> {
    @Override
    public EndPortalRenderState createRenderState() {
        return new EndPortalRenderState();
    }

    @Override
    public void submit(EndPortalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        submitCube(state.facesToShow, RenderTypes.endPortal(), poseStack, collector);
    }
}
