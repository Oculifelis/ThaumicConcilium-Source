package com.thaumaturgicconcilium.rift.client;

import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.client.effect.rendertype.TTFXRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thaumaturgicconcilium.rift.RiftEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * TC4 {@code RiftRenderer}: the rift's tube (a 6-sided poly-cone through its points) drawn four times, three additive
 * shells of 2.25, 1.75 and 1.25 times the girth and a solid core, all through the end-portal starfield. TC4 used GLE and
 * its end shader; this uses Thaumaturge's own rift render types (the same look its Flux Rift has). The innermost shell
 * ignores depth for players wearing revealing goggles, as in TC4. Stability is fixed at TC4's 1.5.
 */
public final class RiftRenderer extends EntityRenderer<RiftEntity, RiftRenderer.State> {
    private static final int SIDES = 6;
    private static final float STAB = 1.5F;
    private static final float[] SHELLS = {2.25F, 1.75F, 1.25F};

    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static final class State extends EntityRenderState {
        final List<Vec3> points = new ArrayList<>();
        final List<Float> widths = new ArrayList<>();
        float lift;
        float time;
        boolean goggles;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RiftEntity rift, State state, float partialTicks) {
        super.extractRenderState(rift, state, partialTicks);
        state.points.clear();
        state.points.addAll(rift.points);
        state.widths.clear();
        state.widths.addAll(rift.widths);
        state.lift = (float) rift.yOffset();
        state.time = rift.tickCount + partialTicks;
        state.goggles = GogglesAccess.wearsRevealingGear(Minecraft.getInstance().player);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        int count = Math.min(state.points.size(), state.widths.size());
        if (count <= 2) {
            return;
        }
        Tube tube = tube(state, count);
        for (int q = 0; q < SHELLS.length; q++) {
            RenderType type = q == 2 && state.goggles ? TTFXRenderTypes.RIFT_GLOW_NO_DEPTH : TTFXRenderTypes.RIFT_GLOW;
            float scale = SHELLS[q];
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> tube.write(buffer, pose.pose(), scale));
        }
        collector.submitCustomGeometry(poseStack, TTFXRenderTypes.RIFT_SOLID, (pose, buffer) -> tube.write(buffer, pose.pose(), 1.0F));
    }

    /** TC4's wobble: each point drifts on three sines whose phase shifts 10 ticks per point away from the middle. */
    private static Tube tube(State state, int count) {
        float[] c = new float[count * 3];
        float[] r = new float[count];
        int mid = count / 2;
        for (int a = 0; a < count; a++) {
            float t = state.time;
            if (a > mid) {
                t -= a * 10;
            } else if (a < mid) {
                t += a * 10;
            }
            Vec3 p = state.points.get(a);
            c[a * 3] = (float) p.x + Mth.sin(t / 50.0F) * 0.1F * STAB;
            c[a * 3 + 1] = (float) p.y + state.lift + Mth.sin(t / 60.0F) * 0.1F * STAB;
            c[a * 3 + 2] = (float) p.z + Mth.sin(t / 70.0F) * 0.1F * STAB;
            r[a] = state.widths.get(a) * (1.0F - Mth.sin(t / 8.0F) * 0.1F * STAB);
        }
        float[] o = new float[count * SIDES * 3];
        Vector3f dir = new Vector3f();
        Vector3f ref = new Vector3f();
        Vector3f across = new Vector3f();
        Vector3f along = new Vector3f();
        for (int i = 0; i < count; i++) {
            int prev = Math.max(i - 1, 0);
            int next = Math.min(i + 1, count - 1);
            dir.set(c[next * 3] - c[prev * 3], c[next * 3 + 1] - c[prev * 3 + 1], c[next * 3 + 2] - c[prev * 3 + 2]);
            if (dir.length() < 0.001F) {
                dir.set(0.0F, 1.0F, 0.0F);
            }
            dir.normalize();
            boolean vertical = Math.abs(dir.y) > 0.9F;
            ref.set(vertical ? 1.0F : 0.0F, vertical ? 0.0F : 1.0F, 0.0F);
            ref.cross(dir, across).normalize();
            dir.cross(across, along);
            for (int side = 0; side < SIDES; side++) {
                float angle = Mth.TWO_PI * side / SIDES;
                int b = (i * SIDES + side) * 3;
                o[b] = across.x * Mth.cos(angle) + along.x * Mth.sin(angle);
                o[b + 1] = across.y * Mth.cos(angle) + along.y * Mth.sin(angle);
                o[b + 2] = across.z * Mth.cos(angle) + along.z * Mth.sin(angle);
            }
        }
        return new Tube(count, c, r, o);
    }

    private record Tube(int count, float[] centers, float[] radii, float[] offsets) {
        void write(VertexConsumer buffer, Matrix4fc matrix, float scale) {
            for (int i = 0; i < count - 1; i++) {
                for (int side = 0; side < SIDES; side++) {
                    int next = (side + 1) % SIDES;
                    vertex(buffer, matrix, i, side, scale);
                    vertex(buffer, matrix, i, next, scale);
                    vertex(buffer, matrix, i + 1, next, scale);
                    vertex(buffer, matrix, i + 1, side, scale);
                }
            }
        }

        private void vertex(VertexConsumer buffer, Matrix4fc matrix, int ring, int side, float scale) {
            int o = (ring * SIDES + side) * 3;
            float radius = radii[ring] * scale;
            buffer.addVertex(matrix, centers[ring * 3] + offsets[o] * radius, centers[ring * 3 + 1] + offsets[o + 1] * radius,
                    centers[ring * 3 + 2] + offsets[o + 2] * radius);
        }
    }
}
