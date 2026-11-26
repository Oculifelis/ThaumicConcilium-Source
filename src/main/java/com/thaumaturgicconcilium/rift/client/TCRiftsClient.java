package com.thaumaturgicconcilium.rift.client;

import com.thaumaturgicconcilium.rift.PartyStatusPayload;
import com.thaumaturgicconcilium.rift.TCRifts;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client side of the rift set: the rift, Hex and Lithographer renderers, the two screens, and the astral hole HUD. */
public final class TCRiftsClient {
    private TCRiftsClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(TCRiftsClient::renderers);
        modBus.addListener(TCRiftsClient::screens);
        modBus.addListener(TCRiftsClient::layers);
        modBus.addListener(TCRiftsClient::payloads);
        NeoForge.EVENT_BUS.addListener(AstralMonitorHandRenderer::onRenderHand);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TCRifts.FLUX_RIFT.get(), RiftRenderer::new);
        event.registerBlockEntityRenderer(TCRifts.HEX_BE.get(), HexRenderer::new);
        event.registerBlockEntityRenderer(TCRifts.LITHOGRAPHER_BE.get(), LithographerRenderer::new);
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(TCRifts.LITHOGRAPHER_MENU.get(), LithographerScreen::new);
        event.register(TCRifts.ASTRAL_MONITOR_MENU.get(), AstralMonitorScreen::new);
    }

    private static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(AstralHoleLayer.ID, new AstralHoleLayer());
    }

    private static void payloads(RegisterClientPayloadHandlersEvent event) {
        event.register(PartyStatusPayload.TYPE, (payload, context) -> AstralHoleLayer.receive(payload));
    }
}
