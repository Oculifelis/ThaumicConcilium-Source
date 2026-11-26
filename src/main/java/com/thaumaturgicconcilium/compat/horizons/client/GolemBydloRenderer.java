package com.thaumaturgicconcilium.compat.horizons.client;

import com.thaumaturgicconcilium.compat.horizons.GolemBydloEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * TC4 Concilium {@code GolemBydloRenderer} (shadow 1.5). TC4 drew it with Thaumcraft's {@code golem_thaumium} sheet;
 * Thaumaturge has no golem sheet in that layout, so the vanilla iron golem sheet stands in (same UV layout).
 */
public class GolemBydloRenderer extends MobRenderer<GolemBydloEntity, GolemBydloRenderState, GolemBydloModel> {
    private static final Identifier TEXTURE = com.thaumaturgicconcilium.ThaumaturgicConcilium.id("textures/entity/golem_bydlo.png");

    public GolemBydloRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemBydloModel(context.bakeLayer(HorizonsClient.GOLEM_BYDLO)), 1.5F);
    }

    @Override
    public GolemBydloRenderState createRenderState() {
        return new GolemBydloRenderState();
    }

    @Override
    public void extractRenderState(GolemBydloEntity entity, GolemBydloRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.action = entity.actionTimer() > 0 ? entity.actionTimer() - partialTicks : 0.0F;
    }

    @Override
    public Identifier getTextureLocation(GolemBydloRenderState state) {
        return TEXTURE;
    }
}
