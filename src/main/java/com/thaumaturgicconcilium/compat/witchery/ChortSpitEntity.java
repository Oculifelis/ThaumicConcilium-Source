package com.thaumaturgicconcilium.compat.witchery;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Chort's spit (TC4 {@code ChortSpitEntity}): a light throwable (gravity 0.01) drawn as a slimeball. What it hits
 * takes the thrower's attack damage as thrown damage; it splats with a small-slime sound.
 *
 * <p>Port fix: the original popped on its own thrower or the Sloppy Alchemist the Chort rode (doing no harm, but wasting
 * the shot); here it passes through both.
 */
public class ChortSpitEntity extends ThrowableItemProjectile {
    public ChortSpitEntity(EntityType<? extends ChortSpitEntity> type, Level level) {
        super(type, level);
    }

    public ChortSpitEntity(Level level, LivingEntity thrower) {
        super(WitcheryMobs.CHORT_SPIT.get(), thrower, level, new ItemStack(Items.SLIME_BALL));
    }

    /**
     * Aims at the target's eyes from the thrower's eyes, the direction turned by {@code yawOffsetDeg} around the vertical
     * (TC4's three-spit fan).
     */
    public void aim(LivingEntity thrower, LivingEntity target, float yawOffsetDeg, float velocity, float inaccuracy) {
        double dx = target.getX() - thrower.getX();
        double dy = target.getEyeY() - thrower.getEyeY();
        double dz = target.getZ() - thrower.getZ();
        double yaw = Math.toRadians(yawOffsetDeg);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        shoot(dx * cos - dz * sin, dy, dx * sin + dz * cos, velocity, inaccuracy);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (owner != null && (target == owner || target == owner.getVehicle())) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel level && getOwner() instanceof LivingEntity thrower) {
            float damage = thrower.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                    ? (float) thrower.getAttributeValue(Attributes.ATTACK_DAMAGE) : 1.0F;
            hit.getEntity().hurtServer(level, damageSources().thrown(this, thrower), damage);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.5F, 1.0F);
            discard();
        }
    }
}
