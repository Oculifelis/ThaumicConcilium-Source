package com.thaumaturgicconcilium.rift.client;

import com.leclowndu93150.thaumaturge.client.effect.rendertype.TTFXRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.rift.HexBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * TC4 {@code HexOfPredictabilityTileRenderer}, drawn on the master only. TC4 had three rune rings (hex1-3.obj) over the
 * 3x3 that turn, the middle one the other way, while a rift is chained, and runic caps whose glow pulses. Stand-in for
 * the .obj models: three flat rings of the original's rune tiles (models/hex_runes.png, a 4x4 sheet) floating just above
 * the hex, glowing additively; the pulse is their brightness.
 */
public final class HexRenderer implements BlockEntityRenderer<HexBlockEntity, HexRenderer.State> {
    private static final Identifier RUNES = ThaumaturgicConcilium.id("textures/block/hex_runes.png");
    private static final RenderType TYPE = TTFXRenderTypes.additive(RUNES);
    private static final int LIGHT = LightCoordsUtil.pack(15, 15);
    /** radius, tiles, tile size of each ring (TC4 hex1, hex2, hex3). */
    private static final float[][] RINGS = {{0.55F, 6, 0.32F}, {1.0F, 12, 0.3F}, {1.42F, 18, 0.28F}};

    public HexRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static final class State extends BlockEntityRenderState {
        boolean rift;
        float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexBlockEntity hex, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(hex, state, partialTicks, camera, breaking);
        state.rift = hex.hasRift();
        var player = Minecraft.getInstance().player;
        state.time = player == null ? 0.0F : player.tickCount + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // TC4's caps shader ran progress 0..60..0 one step a frame while a rift was chained
        float glow = state.rift ? 0.55F + 0.45F * Mth.sin(state.time * Mth.PI / 60.0F) : 0.6F;
        int tint = ARGB.colorFromFloat(1.0F, glow, glow, glow);
        for (int ring = 0; ring < RINGS.length; ring++) {
            float[] spec = RINGS[ring];
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.02F + ring * 0.01F, 0.5F);
            if (state.rift) {
                poseStack.mulPose(Axis.YP.rotationDegrees((ring == 1 ? -1 : 1) * (state.time % 360.0F)));
            }
            int seed = ring * 7;
            collector.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> ring(buffer, pose.pose(), spec, seed, tint));
            poseStack.popPose();
        }
    }

    private static void ring(VertexConsumer buffer, Matrix4fc mat, float[] spec, int seed, int tint) {
        float radius = spec[0];
        int tiles = (int) spec[1];
        float half = spec[2] / 2.0F;
        for (int i = 0; i < tiles; i++) {
            float angle = Mth.TWO_PI * i / tiles;
            float cx = Mth.cos(angle) * radius;
            float cz = Mth.sin(angle) * radius;
            // tangent and radial axes so each rune faces outwards
            float tx = -Mth.sin(angle) * half;
            float tz = Mth.cos(angle) * half;
            float rx = Mth.cos(angle) * half;
            float rz = Mth.sin(angle) * half;
            int tile = (i * 5 + seed) % 16;
            float u0 = (tile % 4) / 4.0F;
            float v0 = (tile / 4) / 4.0F;
            float u1 = u0 + 0.25F;
            float v1 = v0 + 0.25F;
            vertex(buffer, mat, cx - tx - rx, cz - tz - rz, u0, v1, tint);
            vertex(buffer, mat, cx + tx - rx, cz + tz - rz, u1, v1, tint);
            vertex(buffer, mat, cx + tx + rx, cz + tz + rz, u1, v0, tint);
            vertex(buffer, mat, cx - tx + rx, cz - tz + rz, u0, v0, tint);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4fc mat, float x, float z, float u, float v, int tint) {
        buffer.addVertex(mat, x, 0.0F, z).setUv(u, v).setColor(tint).setLight(LIGHT);
    }

    @Override
    public AABB getRenderBoundingBox(HexBlockEntity hex) {
        return new AABB(hex.getBlockPos()).inflate(1.5, 0.5, 1.5);
    }
}
