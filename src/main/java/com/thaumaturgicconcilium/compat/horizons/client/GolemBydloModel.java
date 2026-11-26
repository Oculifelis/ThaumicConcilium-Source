package com.thaumaturgicconcilium.compat.horizons.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * TC4 Concilium {@code GolemBydloModel}: TC4's thaumium golem boxes raised 12 pixels and drawn 1.3 x 1.3 x 1.4 about the
 * model origin. UVs follow the vanilla iron golem sheet (the stand-in texture; TC4's golem_thaumium used the same layout
 * with the arm and leg mirrored). TC4 bent an unridden golem's head back by 57.3 radians (a degrees value set as
 * radians); the head now just follows the look pitch.
 */
public class GolemBydloModel extends EntityModel<GolemBydloRenderState> {
    private static final float RAISE = -12.0F;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GolemBydloModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -11.0F, -5.5F, 8.0F, 9.0F, 8.0F),
                PartPose.offset(0.0F, RAISE, -2.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 40).addBox(-8.0F, -2.0F, -6.0F, 16.0F, 12.0F, 11.0F)
                .texOffs(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, RAISE, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(60, 21).addBox(-12.0F, -2.5F, -3.0F, 4.0F, 25.0F, 6.0F),
                PartPose.offset(0.0F, RAISE, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(60, 58).addBox(8.0F, -2.5F, -3.0F, 4.0F, 25.0F, 6.0F),
                PartPose.offset(0.0F, RAISE, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(-4.0F, 18.0F + RAISE, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(5.0F, 18.0F + RAISE, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GolemBydloRenderState state) {
        super.setupAnim(state);
        root().xScale = 1.3F;
        root().yScale = 1.3F;
        root().zScale = 1.4F;
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        rightLeg.xRot = -1.5F * Mth.triangleWave(pos, 13.0F) * speed;
        leftLeg.xRot = 1.5F * Mth.triangleWave(pos, 13.0F) * speed;
        if (state.action > 0.0F) {
            rightArm.xRot = -2.0F + 1.5F * Mth.triangleWave(state.action, 5.0F);
            leftArm.xRot = -2.0F + 1.5F * Mth.triangleWave(state.action, 5.0F);
        } else {
            rightArm.xRot = (-0.2F + 1.5F * Mth.triangleWave(pos, 13.0F)) * speed;
            leftArm.xRot = (-0.2F - 1.5F * Mth.triangleWave(pos, 13.0F)) * speed;
        }
    }
}
