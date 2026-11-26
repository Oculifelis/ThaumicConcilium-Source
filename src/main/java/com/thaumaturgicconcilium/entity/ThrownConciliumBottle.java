package com.thaumaturgicconcilium.entity;

import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.TCUtil;
import com.thaumaturgicconcilium.item.ConciliumBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown Concilium bottle (light gravity 0.05). Clear Water puts out every creature within 5 blocks, hurts blazes and
 * magma cubes for 10 and clears fire nearby. Thick Taint gives Flux Taint for 5 seconds to every living, non-tainted,
 * non-undead creature within 5 blocks and taints the ground.
 */
public class ThrownConciliumBottle extends ThrowableItemProjectile {
    private static final String FLUX_TAINT = "thaumaturge:flux_taint";

    public ThrownConciliumBottle(EntityType<? extends ThrownConciliumBottle> type, Level level) {
        super(type, level);
    }

    public ThrownConciliumBottle(Level level, LivingEntity owner, ItemStack stack) {
        super(TCRegistry.THROWN_BOTTLE.get(), owner, level);
        setItem(stack);
    }

    public ThrownConciliumBottle(Level level, double x, double y, double z, ItemStack stack) {
        super(TCRegistry.THROWN_BOTTLE.get(), x, y, z, level);
        setItem(stack);
    }

    @Override
    protected Item getDefaultItem() {
        return TCRegistry.BOTTLE_OF_CLEAR_WATER.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        boolean taint = getItem().getItem() instanceof ConciliumBottleItem b && b.kind() == ConciliumBottleItem.Kind.THICK_TAINT;
        AABB area = getBoundingBox().inflate(5.0);
        if (taint) {
            for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!living.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD) && !living.getType().builtInRegistryHolder().is(com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags.TAINTED)) {
                    TCUtil.giveEffect(living, FLUX_TAINT, 100, 0);
                }
            }
            TCUtil.taintArea(server, blockPosition(), 5);
            server.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 40, 1.5, 0.5, 1.5, 0.1);
        } else {
            for (Entity e : server.getEntitiesOfClass(Entity.class, area)) {
                e.clearFire();
                if (e instanceof Blaze || e instanceof MagmaCube) {
                    e.hurt(damageSources().thrown(this, getOwner()), 10.0F);
                }
            }
            BlockPos c = blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, 0, -3), c.offset(2, 1, 2))) {
                if (server.getBlockState(p).getBlock() instanceof BaseFireBlock) {
                    server.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
            }
            server.levelEvent(2002, c, 0x3F76E4);
        }
        server.playSound(null, blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        discard();
    }
}
