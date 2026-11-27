package com.thaumaturgicconcilium.mob.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.EnslavePayload;
import com.thaumaturgicconcilium.mob.TCMobs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client side of the phase 2 mobs: renderers, the biped layer, the trade screen and the golem's mind control. */
public final class TCMobsClient {
    /** TC4's ModelBiped on a 64x32 sheet. */
    public static final ModelLayerLocation BIPED = new ModelLayerLocation(ThaumaturgicConcilium.id("biped"), "main");

    private TCMobsClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(TCMobsClient::layers);
        modBus.addListener(TCMobsClient::renderers);
        modBus.addListener(TCMobsClient::screens);
        payloads();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> EnslavedInput.tick(Minecraft.getInstance().player));
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BIPED, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TCMobs.THAUMATURGE.get(), TCBipedRenderer.of("thaumaturge"));
        event.registerEntityRenderer(TCMobs.MAD_THAUMATURGE.get(), TCBipedRenderer.of("mad_thaumaturge"));
        event.registerEntityRenderer(TCMobs.DISSOLVED.get(), TCBipedRenderer.ender());
        event.registerEntityRenderer(TCMobs.QUICKSILVER_ELEMENTAL.get(), TCBipedRenderer.of("quicksilver_elemental"));
        event.registerEntityRenderer(TCMobs.VENGEFUL_GOLEM.get(), TCBipedRenderer.of("vengeful_golem"));
        event.registerEntityRenderer(TCMobs.OVERANIMATED.get(), TCBipedRenderer.of("overanimated"));
        event.registerEntityRenderer(TCMobs.ALIVE_LIMB.get(), TCBipedRenderer.of("overanimated"));
        event.registerEntityRenderer(TCMobs.TAINT_SPIDER_SMART.get(), TaintSpiderSmartRenderer::new);
        event.registerEntityRenderer(TCMobs.NETHER_EXPLORER.get(), NetherExplorerRenderer::new);
        event.registerEntityRenderer(TCMobs.UPCOMING_HOLE.get(), NoopRenderer::new);
        event.registerEntityRenderer(TCMobs.EMBER.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.5F, true));
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(TCMobs.THAUMATURGE_MENU.get(), ThaumaturgeScreen::new);
    }

    private static void payloads() {
        com.thaumaturgicconcilium.mob.TCMobs.ENSLAVE_HANDLER = payload -> EnslavedInput.set(payload.enslaved());
    }
}
