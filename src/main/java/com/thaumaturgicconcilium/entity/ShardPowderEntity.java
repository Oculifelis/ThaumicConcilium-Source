package com.thaumaturgicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.entity.EntityTaintacleSmall;
import com.leclowndu93150.thaumaturge.content.wands.EntityAspectOrb;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.TCUtil;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * A mote of ground vis crystal (the original's shard powder): it falls and bounces like an experience orb, and after 40
 * ticks does its aspect's trick and is gone. Taint motes seek out players; gluttony motes are eaten on touch.
 */
public class ShardPowderEntity extends Entity {
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(ShardPowderEntity.class, EntityDataSerializers.STRING);
    public static final int MAX_AGE = 40;

    private int age;
    private int chain;
    private @Nullable UUID casterId;

    public ShardPowderEntity(EntityType<? extends ShardPowderEntity> type, Level level) {
        super(type, level);
    }

    public ShardPowderEntity(Level level, double x, double y, double z, ResourceKey<IAspect> aspect, @Nullable Entity caster, int startAge) {
        this(TCRegistry.SHARD_POWDER.get(), level);
        setPos(x, y, z);
        entityData.set(ASPECT, aspect.location().toString());
        casterId = caster == null ? null : caster.getUUID();
        age = startAge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ASPECT, TTAspects.AER.location().toString());
    }

    public ResourceKey<IAspect> aspect() {
        return ResourceKey.create(IAspect.REGISTRY_KEY, ResourceLocation.parse(entityData.get(ASPECT)));
    }

    private String name() {
        return aspect().location().getPath();
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement().add(0, -0.03, 0);
        if (name().equals("vitium") && level() instanceof ServerLevel) {
            Player p = nearestPlayer(8);
            if (p != null) {
                v = v.add(p.getEyePosition().subtract(position()).normalize().scale(0.05));
            }
        }
        setDeltaMovement(v);
        move(MoverType.SELF, getDeltaMovement());
        float friction = onGround() ? 0.588F : 0.98F;
        setDeltaMovement(getDeltaMovement().multiply(friction, 0.98, friction));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1, -0.9, 1));
        }
        if (level() instanceof ServerLevel server) {
            touchPlayers(server);
            if (isRemoved()) {
                return;
            }
            if (++age >= MAX_AGE) {
                perform(server);
                discard();
            }
        } else if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    private @Nullable Player nearestPlayer(double range) {
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(range), p -> !p.getUUID().equals(casterId) && !p.isSpectator())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private Optional<LivingEntity> nearestLiving(double range) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range), l -> l.isAlive() && !l.getUUID().equals(casterId))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr));
    }

    private void touchPlayers(ServerLevel server) {
        String n = name();
        if (!n.equals("vitium") && !n.equals("gula")) {
            return;
        }
        for (ServerPlayer p : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(0.3), p -> !p.getUUID().equals(casterId))) {
            if (n.equals("gula")) {
                p.getFoodData().eat(1, 1.0F);
            } else {
                for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
                    for (ItemStack s : com.thaumaturgicconcilium.util.Inv.all(p.getInventory())) {
                        if (WandVisHelper.getMaxVis(s) > 0) {
                            WandVisHelper.storeVis(s, primal, Math.max(0, WandVisHelper.getVis(s, primal) - 10000));
                        }
                    }
                }
            }
            discard();
            return;
        }
    }

    private void perform(ServerLevel server) {
        BlockPos pos = blockPosition();
        switch (name()) {
            case "aer" -> {
                if (random.nextInt(10) == 0) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                    if (bolt != null) {
                        bolt.moveTo(position());
                        server.addFreshEntity(bolt);
                    }
                }
            }
            case "ignis" -> {
                if (random.nextInt(10) == 0) {
                    server.explode(this, getX(), getY(), getZ(), 1.5F, true, Level.ExplosionInteraction.NONE);
                }
            }
            case "aqua" -> {
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), server, pos.below());
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), server, pos);
            }
            case "terra" -> {
                if (server.getBlockState(pos).canBeReplaced()) {
                    server.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                }
            }
            case "ordo" -> nearestLiving(1.5).ifPresent(l -> {
                double dmg = l.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? l.getAttributeValue(Attributes.ATTACK_DAMAGE) : 1;
                l.hurt(server.damageSources().magic(), (float) dmg);
            });
            case "perditio" -> nearestLiving(1.5).ifPresent(l -> l.addEffect(new MobEffectInstance(MobEffects.WITHER, 90, 50)));
            case "ira" -> nearestLiving(1.5).ifPresent(l -> {
                l.igniteForTicks(50000);
                l.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 5));
                TCUtil.giveEffect(l, "thaumaturge:infectious_vis_exhaust", 200, 10);
            });
            case "invidia" -> {
                if (chain < 5) {
                    for (Projectile pr : server.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(10))) {
                        ShardPowderEntity next = new ShardPowderEntity(server, pr.getX(), pr.getY(), pr.getZ(), aspect(), null, 0);
                        next.chain = chain + 1;
                        pr.discard();
                        server.addFreshEntity(next);
                    }
                }
            }
            case "vitium" -> nearestLiving(1.5).ifPresent(l -> {
                if (l instanceof ServerPlayer p) {
                    WarpHelper.addWarp(p, 20, WarpType.TEMPORARY);
                } else {
                    TCUtil.giveEffect(l, "thaumaturge:flux_taint", 80, 50);
                }
            });
            case "superbia" -> nearestLiving(1.5).ifPresent(l -> {
                Vec3 away = l.position().subtract(position()).normalize().scale(3);
                l.push(away.x, 0.5, away.z);
                l.hurtMarked = true;
            });
            case "luxuria" -> {
                if (random.nextInt(10) == 0) {
                    server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(20),
                                    l -> !l.getType().builtInRegistryHolder().is(com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags.TAINTED))
                            .stream().min(Comparator.comparingDouble(this::distanceToSqr)).ifPresent(target -> {
                                EntityTaintacleSmall t = TTEntities.TAINTACLE_SMALL.get().create(server);
                                if (t != null) {
                                    t.setPos(getX(), getY(), getZ());
                                    t.setTarget(target);
                                    server.addFreshEntity(t);
                                }
                            });
                }
            }
            case "desidia" -> nearestLiving(1.5).ifPresent(l -> {
                l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 15));
                TCUtil.giveEffect(l, "thaumaturge:infectious_vis_exhaust", 60, 20);
            });
            case "desiderium" -> nearestLiving(1.5).ifPresent(l -> {
                Vec3 in = position().subtract(l.position()).normalize().scale(3);
                l.push(in.x, 0.3, in.z);
                l.hurtMarked = true;
            });
            case "gula" -> {
            }
            default -> {
                // Any other aspect (the old balanced shard): an aspect orb of the ground crystal's aspect.
                server.addFreshEntity(new EntityAspectOrb(server, getX(), getY(), getZ(), aspect(), 1));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
        entityData.set(ASPECT, in.getStringOr("aspect", TTAspects.AER.location().toString()));
        age = in.getIntOr("age", 0);
        chain = in.getIntOr("chain", 0);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
        out.putString("aspect", entityData.get(ASPECT));
        out.putInt("age", age);
        out.putInt("chain", chain);
    }
}
