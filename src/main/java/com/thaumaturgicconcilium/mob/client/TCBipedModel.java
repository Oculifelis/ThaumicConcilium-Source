package com.thaumaturgicconcilium.mob.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * TC4's {@code ModelBiped} on a 64x32 sheet (the original's 128x64 sheets are the same layout at double size), with
 * three extras set by the renderer before each draw: the Quicksilver Elemental's wobble (the original's wiggle shader, done as
 * a jelly sway of every part), the Alive Limb's single part, and the Vengeful Golem's squat head.
 */
public class TCBipedModel<T extends LivingEntity> extends HumanoidModel<T> {
    /** Quicksilver wobble strength (0 = none). */
    public float wobble;
    /** Alive Limb part 0-5, or -1 for a whole body. */
    public int limb = -1;
    /** Vengeful Golem's squat head (TC4 scaled it 0.65 x 0.75). */
    public boolean golemHead;
    /** Nether Explorer: no limb swing or head turn. */
    public boolean still;

    public TCBipedModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        for (ModelPart p : new ModelPart[] {head, hat, body, rightArm, leftArm, rightLeg, leftLeg}) {
            p.resetPose();
            p.xScale = 1.0F;
            p.yScale = 1.0F;
            p.zScale = 1.0F;
        }
        if (still) {
            super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        } else {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
        if (golemHead) {
            head.xScale = 0.65F;
            head.yScale = 0.75F;
            head.zScale = 0.65F;
        }
        if (wobble > 0.0F) {
            wobble(ageInTicks, wobble);
        }
        if (limb >= 0) {
            limb(limb);
        }
    }

    private void wobble(float t, float strength) {
        ModelPart[] parts = {head, body, rightArm, leftArm, rightLeg, leftLeg};
        for (int i = 0; i < parts.length; i++) {
            ModelPart p = parts[i];
            float phase = t * 0.35F + i * 1.3F;
            float sx = 1.0F + Mth.sin(phase) * 0.06F * strength;
            float sy = 1.0F + Mth.cos(phase * 0.8F) * 0.05F * strength;
            p.xScale *= sx;
            p.zScale *= sx;
            p.yScale *= sy;
            p.x += Mth.sin(phase * 1.7F) * 0.25F * strength;
            p.zRot += Mth.sin(phase * 0.6F) * 0.05F * strength;
        }
    }

    /** Shows one part, set down so it rests on the ground (the gib's 0.5-tall box). */
    private void limb(int type) {
        head.visible = type == 0;
        hat.visible = type == 0;
        body.visible = type == 1;
        rightArm.visible = type == 2;
        leftArm.visible = type == 3;
        rightLeg.visible = type == 4;
        leftLeg.visible = type == 5;
        ModelPart shown = switch (type) {
            case 0 -> head;
            case 1 -> body;
            case 2 -> rightArm;
            case 3 -> leftArm;
            case 4 -> rightLeg;
            default -> leftLeg;
        };
        shown.x = 0.0F;
        shown.z = 0.0F;
        switch (type) {
            case 0 -> {
                head.y = 24.0F;
                hat.copyFrom(head);
            }
            case 1 -> {
                body.y = 22.0F;
                body.xRot = (float) Math.PI / 2.0F;
            }
            case 2, 3 -> {
                shown.y = 22.0F;
                shown.xRot = (float) Math.PI / 2.0F;
                shown.zRot = 0.0F;
            }
            default -> {
                shown.y = 22.0F;
                shown.xRot = -(float) Math.PI / 2.0F;
            }
        }
    }
}
