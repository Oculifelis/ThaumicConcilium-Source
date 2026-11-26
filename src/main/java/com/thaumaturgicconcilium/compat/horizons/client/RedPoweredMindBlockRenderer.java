package com.thaumaturgicconcilium.compat.horizons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.compat.horizons.RedPoweredMindBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** TC4 Concilium {@code RedPoweredMindTileRenderer}: the brain without its cord, turning toward the nearest player. */
public final class RedPoweredMindBlockRenderer implements BlockEntityRenderer<RedPoweredMindBlockEntity, RedPoweredMindBlockRenderer.State> {
    private final RedPoweredMindModel model;

    public RedPoweredMindBlockRenderer(BlockEntityRendererProvider.Context context) {
        model = new RedPoweredMindModel(context.bakeLayer(HorizonsClient.RED_POWERED_MIND));
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        float yaw;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RedPoweredMindBlockEntity be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breaking);
        state.time = be.getLevel() == null ? 0.0F : be.getLevel().getGameTime() + partialTicks;
        state.yaw = be.rotb + RedPoweredMindBlockEntity.wrap(be.rota - be.rotb) * partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        model.submit(poseStack, collector, state.lightCoords, state.time, state.yaw, false);
        poseStack.popPose();
    }
}
