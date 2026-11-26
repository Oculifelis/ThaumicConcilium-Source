package com.thaumaturgicconcilium.compat.horizons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * TC4 Concilium {@code RedPoweredMindModel} (128 x 64): the brain, its lower lobe, the stem, and the cord the creature
 * trails (the block leaves the cord out). {@link #submit} does the shared TC4 transform of both renderers: lifted 0.3,
 * flipped, bobbing, turned, a quarter turn back and drawn at 0.6.
 */
public final class RedPoweredMindModel {
    public static final Identifier TEXTURE = ThaumaturgicConcilium.id("textures/entity/red_powered_mind.png");
    private static final RenderType TYPE = RenderTypes.entityCutout(TEXTURE);

    private final ModelPart brain;
    private final ModelPart lobe;
    private final ModelPart stem;
    private final ModelPart cord;

    public RedPoweredMindModel(ModelPart root) {
        brain = root.getChild("brain");
        lobe = root.getChild("lobe");
        stem = root.getChild("stem");
        cord = root.getChild("cord");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("brain", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 12.0F, 10.0F, 16.0F),
                PartPose.offset(-6.0F, 8.0F, -8.0F));
        root.addOrReplaceChild("lobe", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 8.0F, 3.0F, 7.0F),
                PartPose.offset(-4.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-1.0F, 18.0F, -2.0F, 0.4089647F, 0.0F, 0.0F));
        // TC4 offsetY 1.0 / offsetZ 0.12 were in blocks: 16 and 1.92 pixels.
        root.addOrReplaceChild("cord", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 16.0F, 2.0F),
                PartPose.offset(-1.0F, 18.0F + 16.0F, -2.0F + 1.92F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * @param time ticks for the bob
     * @param yawRadians the turn toward the nearest player (block) or 0 (creature)
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, float yawRadians, boolean withCord) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.3F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        float bob = Mth.sin(time / 14.0F) * 0.03F + 0.03F;
        poseStack.translate(0.0F, -0.8F + bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(yawRadians));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.scale(0.6F, 0.6F, 0.6F);
        collector.submitModelPart(brain, poseStack, TYPE, light, OverlayTexture.NO_OVERLAY, null);
        collector.submitModelPart(lobe, poseStack, TYPE, light, OverlayTexture.NO_OVERLAY, null);
        collector.submitModelPart(stem, poseStack, TYPE, light, OverlayTexture.NO_OVERLAY, null);
        if (withCord) {
            collector.submitModelPart(cord, poseStack, TYPE, light, OverlayTexture.NO_OVERLAY, null);
        }
        poseStack.popPose();
    }
}
