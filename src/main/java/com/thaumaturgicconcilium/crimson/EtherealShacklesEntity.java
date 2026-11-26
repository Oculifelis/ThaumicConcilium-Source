package com.thaumaturgicconcilium.crimson;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Ethereal Shackles, thrown by the Crimson Paladin: no gravity, gone after 100 ticks. A player it hits is chained for
 * 5 seconds (no interacting, using items or attacking, see {@link Chains}); any other mob is slowed (Slowness V, 1 s).
 * Drawn as a spinning chain sprite (the original's shackles.obj is not ported).
 */
public class EtherealShacklesEntity extends ThrowableItemProjectile {
    public EtherealShacklesEntity(EntityType<? extends EtherealShacklesEntity> type, Level level) {
        super(type, level);
    }

    public EtherealShacklesEntity(Level level, LivingEntity owner) {
        super(TCCrimson.ETHEREAL_SHACKLES.get(), owner, level);
        setItem(new ItemStack(Items.CHAIN));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.CHAIN;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100 && !level().isClientSide()) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (hit.getEntity() instanceof Player player) {
            Chains.chain(player, Chains.DURATION);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TCCrimson.sound("shackles"), SoundSource.HOSTILE, 0.9F, 0.9F);
        } else if (hit.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4), getOwner());
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            discard();
        }
    }
}
