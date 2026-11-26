package com.thaumaturgicconcilium.spell.client;

import com.leclowndu93150.thaumaturge.client.effect.rendertype.TTFXRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thaumaturgicconcilium.spell.entity.GlowingOrb;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4fc;

/**
 * Draws a {@link GlowingOrb} as Thaumaturge's wisp does: two additive billboards from its aura-node sheet, a tinted
 * halo and a white core. No texture of our own.
 */
public final class GlowingOrbRenderer<T extends Entity & GlowingOrb> extends EntityRenderer<T, GlowingOrbRenderer.State> {
    private static final Identifier NODES = Identifier.fromNamespaceAndPath("thaumaturge", "textures/misc/auranodes.png");
    private static final RenderType NODES_TYPE = TTFXRenderTypes.additiveAlphaTest(NODES);
    private static final int GRID = 32;
    private static final int FRAME_START = 800;
    private static final int FRAME_SPREAD = 16;
    private static final int LIGHT = LightCoordsUtil.pack(14, 0);

    public GlowingOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static final class State extends EntityRenderState {
        int tick;
        int color;
        float scale;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tick = entity.tickCount;
        state.color = entity.glowColor();
        state.scale = entity.glowScale();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight * 0.5F, 0.0F);
        poseStack.mulPose(camera.orientation);
        int frame = FRAME_START + state.tick % FRAME_SPREAD;
        quad(collector, poseStack, frame, state.scale, state.color, 0.5F);
        quad(collector, poseStack, frame, state.scale * 0.55F, 0xFFFFFF, 0.9F);
        poseStack.popPose();
    }

    private static void quad(SubmitNodeCollector collector, PoseStack poseStack, int frame, float scale, int color, float alpha) {
        float step = 1.0F / GRID;
        float u0 = (frame % GRID) * step;
        float v0 = (frame / GRID) * step;
        float u1 = u0 + step;
        float v1 = v0 + step;
        float half = scale * 0.5F;
        int tint = ARGB.colorFromFloat(alpha, ARGB.red(color) / 255.0F, ARGB.green(color) / 255.0F, ARGB.blue(color) / 255.0F);
        collector.submitCustomGeometry(poseStack, NODES_TYPE, (pose, buffer) -> vertices(buffer, pose.pose(), half, u0, v0, u1, v1, tint));
    }

    private static void vertices(VertexConsumer buffer, Matrix4fc mat, float half, float u0, float v0, float u1, float v1, int tint) {
        buffer.addVertex(mat, -half, -half, 0.0F).setUv(u1, v1).setColor(tint).setLight(LIGHT);
        buffer.addVertex(mat, -half, half, 0.0F).setUv(u1, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(mat, half, half, 0.0F).setUv(u0, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(mat, half, -half, 0.0F).setUv(u0, v1).setColor(tint).setLight(LIGHT);
    }
}
