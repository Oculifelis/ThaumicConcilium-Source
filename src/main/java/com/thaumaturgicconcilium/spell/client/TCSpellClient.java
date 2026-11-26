package com.thaumaturgicconcilium.spell.client;

import com.thaumaturgicconcilium.spell.TCSpells;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client side of the spell package: renderers for the focus entities (the cosmetic ones draw particles themselves). */
public final class TCSpellClient {
    private TCSpellClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, TCSpellClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TCSpells.COMPRESSED_BLAST.get(), GlowingOrbRenderer::new);
        event.registerEntityRenderer(TCSpells.POSITIVE_BURST_ORB.get(), GlowingOrbRenderer::new);
        event.registerEntityRenderer(TCSpells.WARP_CHARGE.get(), GlowingOrbRenderer::new);
        event.registerEntityRenderer(TCSpells.CHARGED_WISP.get(), GlowingOrbRenderer::new);
        event.registerEntityRenderer(TCSpells.WRATH_EFFECT.get(), NoopRenderer::new);
        event.registerEntityRenderer(TCSpells.DISTORTION_EFFECT.get(), NoopRenderer::new);
    }
}
