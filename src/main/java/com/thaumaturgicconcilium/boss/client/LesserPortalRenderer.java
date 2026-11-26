package com.thaumaturgicconcilium.boss.client;

import com.leclowndu93150.thaumaturge.client.entity.CultistPortalRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.boss.LesserPortalEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * TC4 Concilium's LesserPortalRenderer is TC4's cultist-portal billboard (grows in over 50 ticks, shrinks and flickers
 * with damage, swells on a pulse). Thaumaturge already ports that renderer for its own lesser portal, so this fills
 * Thaumaturge's render state from the Concilium portal and lets Thaumaturge draw it.
 */
public final class LesserPortalRenderer extends EntityRenderer<LesserPortalEntity, CultistPortalRenderer.State> {
    private final CultistPortalRenderer portal;

    public LesserPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.portal = CultistPortalRenderer.lesser(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public CultistPortalRenderer.State createRenderState() {
        return new CultistPortalRenderer.State();
    }

    @Override
    public void extractRenderState(LesserPortalEntity entity, CultistPortalRenderer.State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.active = true;
        state.activeCounter = entity.tickCount + partialTicks;
        state.hurtTime = entity.hurtTime;
        state.pulse = entity.pulse;
        state.healthFraction = entity.getHealth() / entity.getMaxHealth();
        state.halfHeight = entity.getBbHeight() / 2.0F;
    }

    @Override
    public void submit(CultistPortalRenderer.State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        portal.submit(state, poseStack, collector, camera);
    }
}
