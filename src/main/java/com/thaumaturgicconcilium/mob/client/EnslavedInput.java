package com.thaumaturgicconcilium.mob.client;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The Vengeful Golem's mind control on the client (TC4 {@code ClientProxy.sendLocalMovementData}): while enslaved the
 * player's own input is swapped for this one, which walks forward with a random strafe and never jumps or sneaks; the
 * yaw is thrown about every 60 ticks and the pitch wobbles. It wears off at random (50% a tick) once no Vengeful Golem
 * or Crimson Pontifex is within 32 blocks, or at once when the server says so.
 */
public final class EnslavedInput extends Input {
    private static final ResourceLocation GOLEM = ResourceLocation.parse("thaumaturgicconcilium:vengeful_golem");
    private static final ResourceLocation PONTIFEX = ResourceLocation.parse("thaumaturgicconcilium:crimson_pontifex");
    private static boolean enslaved;

    private final Input original;
    private final RandomSource random = RandomSource.create();
    private float strafe;

    private EnslavedInput(Input original) {
        this.original = original;
    }

    public static void set(boolean value) {
        enslaved = value;
    }

    @Override
    public void tick(boolean sneaking, float sneakSpeed) {
        original.tick(sneaking, sneakSpeed);
        if (random.nextInt(10) == 0) {
            strafe = random.nextFloat() * 2.0F - 1.0F;
        }
        up = true;
        down = false;
        left = strafe > 0.3F;
        right = strafe < -0.3F;
        jumping = false;
        shiftKeyDown = false;
        float len = Mth.sqrt(strafe * strafe + 1.0F);
        leftImpulse = strafe / len;
        forwardImpulse = 1.0F / len;
    }

    /** Runs every client tick. */
    public static void tick(LocalPlayer player) {
        if (player == null) {
            enslaved = false;
            return;
        }
        if (enslaved && !controllerNear(player) && player.getRandom().nextBoolean()) {
            enslaved = false;
        }
        if (enslaved) {
            if (!(player.input instanceof EnslavedInput)) {
                player.input = new EnslavedInput(player.input);
            }
            if (player.tickCount % 60 == 0) {
                player.setYRot(player.getYRot() + player.getRandom().nextFloat() * 360.0F - 180.0F);
            }
            player.setXRot(Mth.clamp(player.getXRot() + Mth.cos(player.tickCount * 0.2F) * 2.0F, -60.0F, 60.0F));
        } else if (player.input instanceof EnslavedInput wrapped) {
            player.input = wrapped.original;
        }
    }

    private static boolean controllerNear(LocalPlayer player) {
        var golem = BuiltInRegistries.ENTITY_TYPE.getOptional(GOLEM).orElse(null);
        var pontifex = BuiltInRegistries.ENTITY_TYPE.getOptional(PONTIFEX).orElse(null);
        return !player.level().getEntities(player, player.getBoundingBox().inflate(32.0),
                e -> e.isAlive() && (e.getType() == golem || (pontifex != null && e.getType() == pontifex))).isEmpty();
    }
}
