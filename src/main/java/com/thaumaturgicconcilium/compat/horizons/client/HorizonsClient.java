package com.thaumaturgicconcilium.compat.horizons.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.horizons.HorizonsContent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client side of the Horizons-made creatures: Golem Bydlo and (with Automagy) the Red-Powered Mind, creature and block. */
public final class HorizonsClient {
    public static final ModelLayerLocation GOLEM_BYDLO = new ModelLayerLocation(ThaumaturgicConcilium.id("golem_bydlo"), "main");
    public static final ModelLayerLocation RED_POWERED_MIND = new ModelLayerLocation(ThaumaturgicConcilium.id("red_powered_mind"), "main");

    private HorizonsClient() {
    }

    public static void init(IEventBus modBus, boolean automagy) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            event.registerLayerDefinition(GOLEM_BYDLO, GolemBydloModel::createLayer);
            if (automagy) {
                event.registerLayerDefinition(RED_POWERED_MIND, RedPoweredMindModel::createLayer);
            }
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(HorizonsContent.GOLEM_BYDLO.get(), GolemBydloRenderer::new);
            if (automagy && HorizonsContent.hasMind()) {
                event.registerEntityRenderer(HorizonsContent.RED_POWERED_MIND.get(), RedPoweredMindRenderer::new);
                event.registerBlockEntityRenderer(HorizonsContent.RED_POWERED_MIND_BE.get(), RedPoweredMindBlockRenderer::new);
            }
        });
    }
}
