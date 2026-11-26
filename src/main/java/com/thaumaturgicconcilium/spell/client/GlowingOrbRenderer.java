package com.thaumaturgicconcilium.spell.client;

import com.leclowndu93150.thaumaturge.client.render.TTRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thaumaturgicconcilium.spell.entity.GlowingOrb;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/**
 * Draws a {@link GlowingOrb} as Thaumaturge's wisp does: two additive billboards from its aura-node sheet, a tinted
 * halo and a white core. No texture of our own.
 */
public final class GlowingOrbRenderer<T extends Entity & GlowingOrb> extends EntityRenderer<T> {
    private static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/misc/auranodes.png");
    private static final int GRID = 32;
    private static final int FRAME_START = 800;
    private static final int FRAME_SPREAD = 16;
    private static final int LIGHT = LightTexture.pack(14, 0);

    public GlowingOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
        RenderType type = TTRenderTypes.fxAdditiveAlphaTest(NODES);
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        int frame = FRAME_START + entity.tickCount % FRAME_SPREAD;
        Matrix4f mat = poseStack.last().pose();
        quad(buffers.getBuffer(type), mat, frame, entity.glowScale(), entity.glowColor(), 0.5F);
        quad(buffers.getBuffer(type), mat, frame, entity.glowScale() * 0.55F, 0xFFFFFF, 0.9F);
        poseStack.popPose();
    }

    private static void quad(VertexConsumer buffer, Matrix4f mat, int frame, float scale, int color, float alpha) {
        float step = 1.0F / GRID;
        float u0 = (frame % GRID) * step;
        float v0 = (frame / GRID) * step;
        float u1 = u0 + step;
        float v1 = v0 + step;
        float half = scale * 0.5F;
        int tint = FastColor.ARGB32.colorFromFloat(alpha, FastColor.ARGB32.red(color) / 255.0F, FastColor.ARGB32.green(color) / 255.0F, FastColor.ARGB32.blue(color) / 255.0F);
        buffer.addVertex(mat, -half, -half, 0.0F).setUv(u1, v1).setColor(tint).setLight(LIGHT);
        buffer.addVertex(mat, -half, half, 0.0F).setUv(u1, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(mat, half, half, 0.0F).setUv(u0, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(mat, half, -half, 0.0F).setUv(u0, v1).setColor(tint).setLight(LIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return NODES;
    }
}
