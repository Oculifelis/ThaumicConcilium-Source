package com.thaumaturgicconcilium.crimson.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.crimson.Chains;
import com.thaumaturgicconcilium.crimson.TCCrimson;
import com.thaumaturgicconcilium.spell.client.GlowingOrbRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client side of the crimson mobs. The original drew shackled arms (shackles.obj + chain.obj through its ethereal
 * shader) in place of the hands while chained; the port simply hides the first-person hands for that time.
 */
public final class TCCrimsonClient {
    /** The original's own sheets (CC0, 64x32). The Ranger wore TC4's plain cultist skin: Thaumaturge's is used. */
    private static final Identifier PARANOID_WARRIOR = ThaumaturgicConcilium.id("textures/entity/paranoid_warrior.png");
    private static final Identifier CRIMSON_PALADIN = ThaumaturgicConcilium.id("textures/entity/crimson_paladin.png");
    private static final Identifier CRIMSON_ARCHER = ThaumaturgicConcilium.id("textures/entity/crimson_archer.png");
    private static final Identifier CULTIST = Identifier.fromNamespaceAndPath("thaumaturge", "textures/entity/cultist.png");

    private TCCrimsonClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(TCCrimsonClient::renderers);
        NeoForge.EVENT_BUS.addListener(TCCrimsonClient::hideChainedHands);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TCCrimson.PARANOID_WARRIOR.get(), c -> new CrimsonHumanoidRenderer<>(c, PARANOID_WARRIOR, 0.5F));
        event.registerEntityRenderer(TCCrimson.CRIMSON_PALADIN.get(), c -> new CrimsonHumanoidRenderer<>(c, CRIMSON_PALADIN, 0.5F));
        event.registerEntityRenderer(TCCrimson.CRIMSON_ARCHER.get(), c -> new CrimsonHumanoidRenderer<>(c, CRIMSON_ARCHER, 0.5F));
        event.registerEntityRenderer(TCCrimson.CRIMSON_RANGER.get(), c -> new CrimsonHumanoidRenderer<>(c, CULTIST, 0.5F));
        event.registerEntityRenderer(TCCrimson.ETHEREAL_SHACKLES.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(TCCrimson.CRIMSON_ORB.get(), GlowingOrbRenderer::new);
    }

    private static void hideChainedHands(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player != null && Chains.isChained(player)) {
            event.setCanceled(true);
        }
    }
}
