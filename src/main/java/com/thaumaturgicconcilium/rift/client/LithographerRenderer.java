package com.thaumaturgicconcilium.rift.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.rift.LithographerBlock;
import com.thaumaturgicconcilium.rift.LithographerBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * TC4 {@code LithographerTileRenderer}: three stacked sectors (lithographer2-4.obj) that rise out of the machine while it
 * projects (state 1 up, state 2 held, state 3 down again), at deploy / 150 blocks times 1, 3 and 5. Stand-in for the
 * .obj sectors: three square plates on top of the block, faced with the block's own texture (top and bottom whole, the
 * sides a 2px strip of its frame) so they read as parts of the same press.
 */
public final class LithographerRenderer implements BlockEntityRenderer<LithographerBlockEntity, LithographerRenderer.State> {
    private static final Identifier TEXTURE = ThaumaturgicConcilium.id("textures/block/lithographer.png");
    private static final RenderType TYPE = RenderTypes.entityCutout(TEXTURE);
    /** half width of each plate, from the bottom sector to the top one. */
    private static final float[] HALF = {0.42F, 0.34F, 0.26F};
    private static final float[] STEP = {1.0F, 3.0F, 5.0F};
    private static final float THICK = 2.0F / 16.0F;
    /** Lifts the stack off the block's top face so the lowest plate does not z-fight with it. */
    private static final float LIFT = 0.002F;
    /** V extent of the frame strip on the plate sides (2 of the texture's 16 px). */
    private static final float SIDE_V = 2.0F / 16.0F;

    public LithographerRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static final class State extends BlockEntityRenderState {
        int machineState;
        float deploy;
        boolean zAxis;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LithographerBlockEntity be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breaking);
        state.machineState = be.state();
        state.deploy = be.deploy();
        state.zAxis = be.getBlockState().hasProperty(LithographerBlock.AXIS) && be.getBlockState().getValue(LithographerBlock.AXIS) == Direction.Axis.Z;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float rise = switch (state.machineState) {
            case 1 -> state.deploy / 150.0F;
            case 3 -> (20.0F + state.deploy) / 150.0F;
            default -> 0.0F;
        };
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        if (!state.zAxis) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        for (int i = 0; i < HALF.length; i++) {
            float y = LIFT + i * THICK + rise * STEP[i];
            float h = HALF[i];
            collector.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> plate(buffer, pose, h, y, light));
        }
        poseStack.popPose();
    }

    private static void plate(VertexConsumer buffer, PoseStack.Pose pose, float h, float y0, int light) {
        Matrix4fc m = pose.pose();
        float y1 = y0 + THICK;
        // top and bottom
        quad(buffer, m, pose, -h, y1, -h, -h, y1, h, h, y1, h, h, y1, -h, 0, 1, 0, 0, 0, 1, 1, light);
        quad(buffer, m, pose, -h, y0, -h, h, y0, -h, h, y0, h, -h, y0, h, 0, -1, 0, 0, 0, 1, 1, light);
        // sides: the texture's top 2px (the frame), across the full width
        quad(buffer, m, pose, -h, y0, -h, -h, y1, -h, h, y1, -h, h, y0, -h, 0, 0, -1, 0, 0, 1, SIDE_V, light);
        quad(buffer, m, pose, h, y0, h, h, y1, h, -h, y1, h, -h, y0, h, 0, 0, 1, 0, 0, 1, SIDE_V, light);
        quad(buffer, m, pose, -h, y0, h, -h, y1, h, -h, y1, -h, -h, y0, -h, -1, 0, 0, 0, 0, 1, SIDE_V, light);
        quad(buffer, m, pose, h, y0, -h, h, y1, -h, h, y1, h, h, y0, h, 1, 0, 0, 0, 0, 1, SIDE_V, light);
    }

    private static void quad(VertexConsumer b, Matrix4fc m, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, float u0, float v0, float u1, float v1,
            int light) {
        vertex(b, m, pose, x0, y0, z0, u0, v1, nx, ny, nz, light);
        vertex(b, m, pose, x1, y1, z1, u0, v0, nx, ny, nz, light);
        vertex(b, m, pose, x2, y2, z2, u1, v0, nx, ny, nz, light);
        vertex(b, m, pose, x3, y3, z3, u1, v1, nx, ny, nz, light);
    }

    private static void vertex(VertexConsumer b, Matrix4fc m, PoseStack.Pose pose, float x, float y, float z, float u, float v,
            float nx, float ny, float nz, int light) {
        b.addVertex(m, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    @Override
    public AABB getRenderBoundingBox(LithographerBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }
}
