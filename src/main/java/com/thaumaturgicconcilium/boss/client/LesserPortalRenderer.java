package com.thaumaturgicconcilium.boss.client;

import com.leclowndu93150.thaumaturge.client.effect.OccludingEffectRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.LesserPortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * TC4 Concilium's LesserPortalRenderer is TC4's cultist-portal billboard (grows in over 50 ticks, shrinks and flickers
 * with damage, swells on a pulse). Thaumaturge draws its own lesser portal through its occluding effect pass; this
 * works out the same size, frame and opacity from the Concilium portal and queues it there.
 */
public final class LesserPortalRenderer extends EntityRenderer<LesserPortalEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumaturge:textures/misc/cultist_portal.png");
    private static final float FULL_HEIGHT = 2.9F;
    private static final float FULL_WIDTH = 2.3F;

    public LesserPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(LesserPortalEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTicks;
        float damage = Mth.clamp(1.0F - entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
        float flinch = Mth.clamp((entity.hurtTime - partialTicks) / 10.0F, 0.0F, 1.0F);
        float beat = entity.pulse <= 0 ? 0.0F : Mth.clamp(1.0F - (entity.pulse - partialTicks) / 10.0F, 0.0F, 1.0F);
        float swell = beat <= 0.0F ? 0.0F : Mth.sin(beat * (float) Math.PI);

        float halfHeight = FULL_HEIGHT * 0.5F * (1.0F - damage * 0.16F * breathing(damage, age, 1.0F, 0.0F))
                * (1.0F - 0.14F * flinch) * (1.0F + 0.28F * swell);
        float halfWidth = FULL_WIDTH * 0.5F * (1.0F - damage * 0.22F * breathing(damage, age, 1.37F, 1.1F))
                * (1.0F + 0.1F * flinch) * (1.0F + 0.28F * swell) * openWidth(age);
        float u0 = Math.floorMod((int) age, 32) * 0.03125F;
        float alpha = Mth.clamp(1.0F - damage * 0.62F * breathing(damage, age, 1.0F, 0.0F) - damage * 0.08F * trough(age * 0.9F), 0.2F, 1.0F);
        int color = FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);

        Vec3 pos = entity.getPosition(partialTicks).add(0.0, entity.getBbHeight() / 2.0F, 0.0);
        OccludingEffectRenderer.enqueuePortal(pos, halfWidth, halfHeight, u0, u0 + 0.03125F, color, 15728860);
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
    }

    private static float openWidth(float age) {
        float t = Mth.clamp(age / 50.0F, 0.0F, 1.0F);
        return Mth.lerp(t * t * (3.0F - 2.0F * t), 0.04F, 1.0F);
    }

    private static float breathing(float damage, float age, float rate, float phase) {
        float calm = trough(age * 0.035F * rate + phase);
        float agitated = trough(age * 0.16F * rate + phase);
        return Mth.lerp(damage, calm, agitated);
    }

    private static float trough(float x) {
        return 0.5F + 0.5F * Mth.sin(x);
    }

    @Override
    public ResourceLocation getTextureLocation(LesserPortalEntity entity) {
        return TEXTURE;
    }
}
