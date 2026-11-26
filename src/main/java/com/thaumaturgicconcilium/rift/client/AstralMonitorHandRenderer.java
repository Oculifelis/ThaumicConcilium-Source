package com.thaumaturgicconcilium.rift.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thaumaturgicconcilium.rift.TCRifts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First-person hold for the Astral Monitor, matching the Thaumometer: held up in front of the eyes in both hands like a
 * map when the other hand is empty, otherwise one-handed off to the side. The monitor is drawn with no display context
 * and posed here, so its "head" transform stays free for armour stands.
 */
public final class AstralMonitorHandRenderer {
    /** The monitor mesh is 1 block wide; the Thaumometer's is 2.8, held at 0.45. */
    private static final float ITEM_SCALE = 0.45F * 2.8F;
    private static final float TWO_HAND_SCALE = 0.9F;
    private static final float ONE_HAND_SCALE = 0.45F;
    private static final float SWING = 0.15F;
    private static final float DRIFT = 0.1F;

    private AstralMonitorHandRenderer() {
    }

    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !event.getItemStack().is(TCRifts.ASTRAL_MONITOR.get())) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND && player.getOffhandItem().isEmpty()) {
            twoHanded(event, mc, player);
        } else {
            oneHanded(event, mc, player);
        }
    }

    private static void twoHanded(RenderHandEvent event, Minecraft mc, LocalPlayer player) {
        PoseStack pose = event.getPoseStack();
        MultiBufferSource out = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float partial = event.getPartialTick();
        float equip = event.getEquipProgress();
        float swing = event.getSwingProgress();
        float root = Mth.sqrt(swing);
        float lift = -0.2F * Mth.sin(swing * Mth.PI) * SWING;
        float push = -0.4F * Mth.sin(root * Mth.PI) * SWING;

        pose.pushPose();
        pose.translate(0.0F, -lift / 2.0F, push);
        pose.translate(0.0F, 0.04F + equip * -1.2F * 0.25F, -0.72F);
        pose.scale(TWO_HAND_SCALE, TWO_HAND_SCALE, TWO_HAND_SCALE);
        float pitch = Mth.lerp(partial, player.xRotO, player.getXRot());
        float yaw = Mth.lerp(partial, player.yRotO, player.getYRot());
        float bobPitch = Mth.lerp(partial, player.xBobO, player.xBob);
        float bobYaw = Mth.lerp(partial, player.yBobO, player.yBob);
        pose.mulPose(Axis.XP.rotationDegrees((pitch - bobPitch) * DRIFT));
        pose.mulPose(Axis.YP.rotationDegrees((yaw - bobYaw) * DRIFT));
        if (!player.isInvisible()) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            gripHand(mc, player, pose, out, light, HumanoidArm.RIGHT);
            gripHand(mc, player, pose, out, light, HumanoidArm.LEFT);
            pose.popPose();
        }
        pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(root * Mth.PI) * 20.0F * SWING));
        drawMonitor(mc, player, event.getItemStack(), pose, out, light);
        pose.popPose();
    }

    private static void oneHanded(RenderHandEvent event, Minecraft mc, LocalPlayer player) {
        PoseStack pose = event.getPoseStack();
        MultiBufferSource out = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float equip = event.getEquipProgress();
        float swing = event.getSwingProgress();
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        pose.pushPose();
        pose.translate(side * 0.125F, -0.125F, 0.0F);
        if (!player.isInvisible()) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(side * 10.0F));
            playerArm(mc, player, pose, out, light, equip, swing, arm);
            pose.popPose();
        }
        float root = Mth.sqrt(swing);
        float arc = Mth.sin(root * Mth.PI);
        pose.translate(side * 0.51F, -0.08F + equip * -1.2F, -0.75F);
        pose.translate(side * -0.5F * arc, 0.4F * Mth.sin(root * Mth.TWO_PI) - 0.3F * arc, -0.3F * Mth.sin(swing * Mth.PI));
        pose.mulPose(Axis.XP.rotationDegrees(arc * -45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * arc * -30.0F));
        pose.scale(ONE_HAND_SCALE, ONE_HAND_SCALE, ONE_HAND_SCALE);
        drawMonitor(mc, player, event.getItemStack(), pose, out, light);
        pose.popPose();
    }

    private static void drawMonitor(Minecraft mc, AbstractClientPlayer player, ItemStack stack, PoseStack pose, MultiBufferSource out, int light) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        mc.getItemRenderer().renderStatic(player, stack, ItemDisplayContext.NONE, false, pose, out, player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
        pose.popPose();
    }

    /** One arm reaching in to grip the monitor's edge, as for a map held in both hands. */
    private static void gripHand(Minecraft mc, AbstractClientPlayer player, PoseStack pose, MultiBufferSource out, int light, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(92.0F));
        pose.mulPose(Axis.XP.rotationDegrees(45.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -41.0F));
        pose.translate(side * 0.3F, -1.1F, 0.45F);
        submitArm(mc, player, pose, out, light, arm);
        pose.popPose();
    }

    /** The plain vanilla first-person arm, swing and equip dip included. */
    private static void playerArm(Minecraft mc, AbstractClientPlayer player, PoseStack pose, MultiBufferSource out, int light,
            float equip, float swing, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float root = Mth.sqrt(swing);
        pose.translate(side * (-0.3F * Mth.sin(root * Mth.PI) + 0.64000005F), 0.4F * Mth.sin(root * Mth.TWO_PI) - 0.6F + equip * -0.6F,
                -0.4F * Mth.sin(swing * Mth.PI) - 0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * Mth.sin(root * Mth.PI) * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swing * swing * Mth.PI) * -20.0F));
        pose.translate(side * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        pose.translate(side * 5.6F, 0.0F, 0.0F);
        submitArm(mc, player, pose, out, light, arm);
    }

    private static void submitArm(Minecraft mc, AbstractClientPlayer player, PoseStack pose, MultiBufferSource out, int light, HumanoidArm arm) {
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(pose, out, light, player);
        } else {
            renderer.renderLeftHand(pose, out, light, player);
        }
    }
}
