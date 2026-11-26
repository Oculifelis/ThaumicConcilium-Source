package com.thaumaturgicconcilium.rift.client;

import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.client.effect.LateWorldRenderQueue;
import com.leclowndu93150.thaumaturge.client.entity.FluxRiftRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.rift.RiftEntity;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * TC4 {@code RiftRenderer}: the rift's tube (a 6-sided poly-cone through its points) drawn four times, three additive
 * shells of 2.25, 1.75 and 1.25 times the girth and a solid core, all through the end-portal starfield. TC4 used GLE and
 * its end shader; this uses Thaumaturge's own Flux Rift render types (looked up once; vanilla's end portal type if they
 * are missing) through its own-shader queue. The innermost shell ignores depth for players wearing revealing goggles,
 * as in TC4. Stability is fixed at TC4's 1.5.
 */
public final class RiftRenderer extends EntityRenderer<RiftEntity> {
    private static final int SIDES = 6;
    private static final float STAB = 1.5F;
    private static final float[] SHELLS = {2.25F, 1.75F, 1.25F};
    private static Method passType;
    private static boolean looked;

    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    /** Thaumaturge's rift pass types: 0-2 glow (2 without depth when asked), 3 the core. */
    private static RenderType type(int pass, boolean noDepth) {
        if (!looked) {
            looked = true;
            try {
                passType = FluxRiftRenderer.class.getDeclaredMethod("renderTypeForPass", int.class, boolean.class);
                passType.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException e) {
                ThaumaturgicConcilium.LOG.warn("Thaumaturge rift render types not found; rifts use the end portal look", e);
            }
        }
        if (passType != null) {
            try {
                return (RenderType) passType.invoke(null, pass, noDepth);
            } catch (ReflectiveOperationException | RuntimeException e) {
                passType = null;
            }
        }
        return RenderType.endPortal();
    }

    @Override
    public void render(RiftEntity rift, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(rift, yaw, partialTicks, poseStack, buffers, light);
        int count = Math.min(rift.points.size(), rift.widths.size());
        if (count <= 2) {
            return;
        }
        var player = Minecraft.getInstance().player;
        boolean goggles = player != null && GogglesAccess.wearsRevealingGear(player);
        Tube tube = tube(rift.points, rift.widths, (float) rift.yOffset(), rift.tickCount + partialTicks, count);
        for (int q = 0; q < SHELLS.length; q++) {
            float scale = SHELLS[q];
            LateWorldRenderQueue.drawOwnShader(poseStack, buffers, type(q, goggles), (pose, buffer) -> tube.write(buffer, pose.pose(), scale));
        }
        LateWorldRenderQueue.drawOwnShader(poseStack, buffers, type(3, false), (pose, buffer) -> tube.write(buffer, pose.pose(), 1.0F));
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity rift) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    /** TC4's wobble: each point drifts on three sines whose phase shifts 10 ticks per point away from the middle. */
    private static Tube tube(List<Vec3> points, List<Float> widths, float lift, float time, int count) {
        float[] c = new float[count * 3];
        float[] r = new float[count];
        int mid = count / 2;
        for (int a = 0; a < count; a++) {
            float t = time;
            if (a > mid) {
                t -= a * 10;
            } else if (a < mid) {
                t += a * 10;
            }
            Vec3 p = points.get(a);
            c[a * 3] = (float) p.x + Mth.sin(t / 50.0F) * 0.1F * STAB;
            c[a * 3 + 1] = (float) p.y + lift + Mth.sin(t / 60.0F) * 0.1F * STAB;
            c[a * 3 + 2] = (float) p.z + Mth.sin(t / 70.0F) * 0.1F * STAB;
            r[a] = widths.get(a) * (1.0F - Mth.sin(t / 8.0F) * 0.1F * STAB);
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
        void write(VertexConsumer buffer, Matrix4f matrix, float scale) {
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

        private void vertex(VertexConsumer buffer, Matrix4f matrix, int ring, int side, float scale) {
            int o = (ring * SIDES + side) * 3;
            float radius = radii[ring] * scale;
            buffer.addVertex(matrix, centers[ring * 3] + offsets[o] * radius, centers[ring * 3 + 1] + offsets[o + 1] * radius,
                    centers[ring * 3 + 2] + offsets[o + 2] * radius);
        }
    }
}
