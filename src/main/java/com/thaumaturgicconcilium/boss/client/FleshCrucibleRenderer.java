package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.boss.FleshCrucibleBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** TC4 FleshCrucibleTileRenderer: the liquid flesh surface, 0.3 + amount / 100 above the floor of the block. */
public final class FleshCrucibleRenderer implements BlockEntityRenderer<FleshCrucibleBlockEntity, FleshCrucibleRenderer.State> {
    private static final Identifier LIQUID = ThaumaturgicConcilium.id("textures/block/liquid_flesh.png");
    private static final RenderType TYPE = RenderTypes.entityTranslucent(LIQUID);

    public FleshCrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static final class State extends BlockEntityRenderState {
        int flesh;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FleshCrucibleBlockEntity be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breaking);
        state.flesh = be.flesh();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.flesh <= 0) {
            return;
        }
        float y = 0.3F + state.flesh / 100.0F;
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> surface(buffer, pose.pose(), pose, y, light));
    }

    private static void surface(VertexConsumer buffer, Matrix4fc mat, PoseStack.Pose pose, float y, int light) {
        vertex(buffer, mat, pose, 0.0F, y, 0.0F, 0.0F, 0.0F, light);
        vertex(buffer, mat, pose, 0.0F, y, 1.0F, 0.0F, 1.0F, light);
        vertex(buffer, mat, pose, 1.0F, y, 1.0F, 1.0F, 1.0F, light);
        vertex(buffer, mat, pose, 1.0F, y, 0.0F, 1.0F, 0.0F, light);
    }

    private static void vertex(VertexConsumer buffer, Matrix4fc mat, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(mat, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
