package com.thaumaturgicconcilium.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.NetherExplorerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The Nether Explorer lying flat on the lava, as the original drew it (a still biped turned 90 degrees). */
public class NetherExplorerRenderer extends MobRenderer<NetherExplorerEntity, TCBipedModel<NetherExplorerEntity>> {
    private static final ResourceLocation TEXTURE = ThaumaturgicConcilium.id("textures/entity/nether_explorer.png");

    public NetherExplorerRenderer(EntityRendererProvider.Context context) {
        super(context, new TCBipedModel<>(context.bakeLayer(TCMobsClient.BIPED)), 0.0F);
        model.still = true;
    }

    @Override
    protected void setupRotations(NetherExplorerEntity entity, PoseStack poseStack, float bob, float bodyRot, float partialTicks, float entityScale) {
        super.setupRotations(entity, poseStack, bob, bodyRot, partialTicks, entityScale);
        poseStack.translate(0.0F, 0.13F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, -0.9F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(NetherExplorerEntity entity) {
        return TEXTURE;
    }
}
