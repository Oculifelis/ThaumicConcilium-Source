package com.thaumaturgicconcilium.compat.bases.link.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.bases.link.DopeSquidEntity;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** TC4 Concilium {@code DopeSquidRenderer}: the vanilla squid model with the original's CC0 dope_squid skin. */
public class DopeSquidRenderer extends MobRenderer<DopeSquidEntity, SquidRenderState, SquidModel> {
    private static final Identifier TEXTURE = ThaumaturgicConcilium.id("textures/entity/dope_squid.png");

    public DopeSquidRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel(context.bakeLayer(ModelLayers.SQUID)), 0.7F);
    }

    @Override
    public SquidRenderState createRenderState() {
        return new SquidRenderState();
    }

    @Override
    public void extractRenderState(DopeSquidEntity entity, SquidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tentacleAngle = Mth.lerp(partialTicks, entity.oldTentacleAngle, entity.tentacleAngle);
        state.xBodyRot = Mth.lerp(partialTicks, entity.xBodyRotO, entity.xBodyRot);
        state.zBodyRot = Mth.lerp(partialTicks, entity.zBodyRotO, entity.zBodyRot);
    }

    @Override
    protected void setupRotations(SquidRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xBodyRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.zBodyRot));
        poseStack.translate(0.0F, -1.2F, 0.0F);
    }

    @Override
    public Identifier getTextureLocation(SquidRenderState state) {
        return TEXTURE;
    }
}
