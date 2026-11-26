package com.thaumaturgicconcilium.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Thaumaturge's ember (TC4 {@code EntityEmber}: damage 3, lives 20 ticks, sets nothing alight). A small fireball
 * that slows down instead of speeding up, burns out after {@link #LIFE} ticks and never starts fires.
 */
public class EmberEntity extends Fireball {
    private static final int LIFE = 20;
    private static final float DAMAGE = 3.0F;

    public EmberEntity(EntityType<? extends EmberEntity> type, Level level) {
        super(type, level);
        accelerationPower = 0.0;
    }

    public EmberEntity(Level level, LivingEntity owner, Vec3 from) {
        super(TCMobs.EMBER.get(), from.x, from.y, from.z, Vec3.ZERO, level);
        setOwner(owner);
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
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel level) {
            hit.getEntity().hurt(damageSources().fireball(this, getOwner()), DAMAGE);
        }
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return !(target instanceof ThaumaturgeEntity) && super.canHitEntity(target);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            discard();
        }
    }
}
