package com.thaumaturgicconcilium.compat.bases.link.client;

import com.thaumaturgicconcilium.compat.bases.link.BasesLink;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client side of the Doped Squid. */
public final class BasesLinkClient {
    private BasesLinkClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(BasesLink.DOPE_SQUID.get(),
                DopeSquidRenderer::new));
    }
}
