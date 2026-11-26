package com.thaumaturgicconcilium.boss.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.thaumaturgicconcilium.boss.RobeTogglePayload;
import com.thaumaturgicconcilium.boss.TCBosses;
import com.thaumaturgicconcilium.crimson.client.CrimsonHumanoidRenderer;
import com.thaumaturgicconcilium.spell.client.GlowingOrbRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** Client side of the phase 3 bosses: renderers, block-entity renderers and the Pontifex robe's toggle key. */
public final class TCBossesClient {
    private static final Identifier CULTIST = Identifier.fromNamespaceAndPath("thaumaturge", "textures/entity/cultist.png");
    /** TC4 bound "Toggle Pontifex Robe" to L in the misc category. */
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumaturgicconcilium", "main"));
    public static final KeyMapping TOGGLE_ROBE = new KeyMapping("key.thaumaturgicconcilium.toggle_pontifex_robe", InputConstants.KEY_L,
            CATEGORY);

    private TCBossesClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(TCBossesClient::renderers);
        modBus.addListener(TCBossesClient::keys);
        NeoForge.EVENT_BUS.addListener(TCBossesClient::tick);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TCBosses.BRIGHTEST_ONE.get(), GlowingOrbRenderer::new);
        event.registerEntityRenderer(TCBosses.LESSER_PORTAL.get(), LesserPortalRenderer::new);
        event.registerEntityRenderer(TCBosses.CRIMSON_PONTIFEX.get(), c -> new CrimsonHumanoidRenderer<>(c, CULTIST, 1.0F));
        event.registerEntityRenderer(TCBosses.MATERIAL_PEELER.get(), MaterialPeelerRenderer::new);
        event.registerEntityRenderer(TCBosses.PROTOLIMB.get(), ProtolimbRenderer::new);
        event.registerEntityRenderer(TCBosses.PROTOBODY_ITEM.get(), ProtobodyRenderer::new);
        event.registerEntityRenderer(TCBosses.BRIGHT_EMBER.get(), c -> new ThrownItemRenderer<>(c, 0.75F, true));
        event.registerBlockEntityRenderer(TCBosses.FLESH_CRUCIBLE_BE.get(), FleshCrucibleRenderer::new);
        event.registerBlockEntityRenderer(TCBosses.SOLID_VOID_BE.get(), c -> new SolidVoidRenderer());
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_ROBE);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE_ROBE.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                ClientPacketDistributor.sendToServer(RobeTogglePayload.INSTANCE);
            }
        }
    }
}
