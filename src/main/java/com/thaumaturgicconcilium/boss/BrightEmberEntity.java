package com.thaumaturgicconcilium.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An ignis Brightest One's ember (TC4 {@code EntityEmber} with damage 25 and {@code firey = 0}): a small fireball
 * that coasts without speeding up, burns out after 20 ticks and sets nothing alight.
 */
public class BrightEmberEntity extends Fireball {
    private static final int LIFE = 20;
    private static final float DAMAGE = 25.0F;

    public BrightEmberEntity(EntityType<? extends BrightEmberEntity> type, Level level) {
        super(type, level);
        accelerationPower = 0.0;
    }

    public BrightEmberEntity(Level level, LivingEntity owner, Vec3 from, Vec3 motion) {
        super(TCBosses.BRIGHT_EMBER.get(), from.x, from.y, from.z, Vec3.ZERO, level);
        setOwner(owner);
        setDeltaMovement(motion);
        accelerationPower = 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > LIFE) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !(target instanceof BrightestOneEntity) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel level) {
            hit.getEntity().hurtServer(level, damageSources().fireball(this, getOwner()), DAMAGE);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
