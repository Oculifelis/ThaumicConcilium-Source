package com.thaumaturgicconcilium.compat.witchery.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.witchery.WitcheryMobs;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client side of the Witchery mobs: the original's CC0 64x32 sheets on TC4's biped shapes. The Chort has its tail, the
 * Sloppy Alchemist goes on all fours while ridden (and stands as a slightly puffed-out biped when free, TC4
 * {@code ModelBiped(0.5F)}), and the Burned Witch sits on Witchery's own broom model. The spit is drawn as a slimeball.
 */
public final class WitcheryClient {
    /** TC4 ChortModel: a biped with a flat 0 x 11 x 3 tail at the body's back, tilted 45 degrees (a child of the body, whose pivot is the model origin, so the 1.21.1 biped draws it). */
    public static final ModelLayerLocation CHORT = new ModelLayerLocation(ThaumaturgicConcilium.id("chort"), "main");
    /** The plain TC4 biped, for the ridden Sloppy Alchemist and the Burned Witch. */
    public static final ModelLayerLocation BIPED = new ModelLayerLocation(ThaumaturgicConcilium.id("witchery_biped"), "main");
    /** TC4 ModelBiped(0.5F): every box grown by 0.5 (the free Sloppy Alchemist). */
    public static final ModelLayerLocation BIPED_PUFFED = new ModelLayerLocation(ThaumaturgicConcilium.id("witchery_biped"), "puffed");

    private WitcheryClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(WitcheryClient::layers);
        modBus.addListener(WitcheryClient::renderers);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CHORT, () -> {
            MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
            mesh.getRoot().getChild("body").addOrReplaceChild("tail", CubeListBuilder.create().texOffs(56, 18).mirror().addBox(0.0F, -4.0F, 8.0F, 0.0F, 11.0F, 3.0F),
                    PartPose.rotation(-0.7853982F, 0.0F, 0.0F));
            return LayerDefinition.create(mesh, 64, 32);
        });
        event.registerLayerDefinition(BIPED, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32));
        event.registerLayerDefinition(BIPED_PUFFED, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0F), 64, 32));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WitcheryMobs.CHORT.get(), ChortRenderer::new);
        event.registerEntityRenderer(WitcheryMobs.SLOPPY_ALCHEMIST.get(), SloppyAlchemistRenderer::new);
        event.registerEntityRenderer(WitcheryMobs.BURNED_WITCH.get(), BurnedWitchRenderer::new);
        event.registerEntityRenderer(WitcheryMobs.CHORT_SPIT.get(), ThrownItemRenderer::new);
    }
}
