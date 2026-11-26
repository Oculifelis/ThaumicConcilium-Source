package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.effect.WispEffects;
import com.leclowndu93150.thaumaturge.content.entity.EntityGolemOrb;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.leclowndu93150.thaumaturge.content.entity.boss.BossBar;
import com.leclowndu93150.thaumaturge.content.spell.block.PortableHoles;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.entity.ShardPowderEntity;
import com.thaumaturgicconcilium.spell.entity.ChargedWispEntity;
import com.thaumaturgicconcilium.spell.entity.ChargedWispTarget;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Brightest One (TC4 Concilium): a 3-block wisp of one primal-ish aspect (Ignis, Aer, Vacuos or Tenebrae) that a
 * charged wisp pulls out of a chained rift. Nothing hurts it; a hit only turns it on the attacker for ten seconds. Its
 * 15 "health" points are charges: each Charged Wisp that reaches it (after its first second) short-circuits one and
 * switches its aspect at random. At none it dies and drops a Primordial Life and a vis crystal of its last aspect.
 *
 * <p>It drifts between random waypoints within 16 blocks, hunts the nearest player within 64, and every 20 ticks of
 * line of sight within 16 blocks strikes by aspect:
 * <ul>
 * <li>Aer: a 25-damage zap, or ten ticks of golem orbs.</li>
 * <li>Ignis: ten ignis shard powders, or ten ticks of 30-ember volleys (25 damage each).</li>
 * <li>Vacuos: a 33-deep portable hole under the target for 6 seconds, or a void zap of 25/4.</li>
 * <li>Tenebrae: ten ticks of shadow beams, or blindness for 30 seconds on every player within 32.</li>
 * </ul>
 * After each strike, one time in three, 3 to 5 wisps of random aspects join it while fewer than 8 are near.
 */
public class BrightestOneEntity extends Monster implements ChargedWispTarget, com.thaumaturgicconcilium.spell.entity.GlowingOrb {
    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(BrightestOneEntity.class, EntityDataSerializers.STRING);
    private static final List<ResourceKey<IAspect>> TYPES = List.of(TTAspects.IGNIS, TTAspects.AER, TTAspects.VACUOS, TTAspects.TENEBRAE);
    private static final double ATTACK_RANGE = 16.0;
    private static final double SEEK_RANGE = 64.0;
    private static final int AGGRO_TICKS = 200;
    private static final float SOUND_VOLUME = 0.25F;

    private final BossBar bossBar = new BossBar(this);
    private double waypointX;
    private double waypointY;
    private double waypointZ;
    private int courseChangeCooldown;
    private int attackCounter;
    private int aggroCooldown;
    private boolean continuousAttack;
    private @Nullable LivingEntity targeted;

    public BrightestOneEntity(EntityType<? extends BrightestOneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.ATTACK_DAMAGE, 25.0)
                .add(Attributes.FOLLOW_RANGE, SEEK_RANGE).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, TTAspects.IGNIS.location().toString());
    }

    public ResourceKey<IAspect> aspect() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(TYPE));
        return id == null ? TTAspects.IGNIS : ResourceKey.create(IAspect.REGISTRY_KEY, id);
    }

    public void setAspect(ResourceKey<IAspect> aspect) {
        entityData.set(TYPE, aspect.location().toString());
    }

    /** The aspect's colour (client render tint); white when the aspect is unknown. */
    @Override
    public int glowColor() {
        return aspectColor();
    }

    /** TC4 drew it as a node about three and a half blocks across. */
    @Override
    public float glowScale() {
        return 3.5F;
    }

    public int aspectColor() {
        return level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(aspect()).map(h -> h.value().color()).orElse(0xFFFFFF);
    }

    /** TC4 changeType: a random aspect of the four. */
    public void changeType() {
        setAspect(TYPES.get(getRandom().nextInt(TYPES.size())));
    }

    /**
     * One charge lost. At none the Brightest One dies, once (TC4 kept it alive at 0 and dropped its items again on
     * every later short circuit).
     */
    public void shortCircuit() {
        if (!(level() instanceof ServerLevel level) || isDeadOrDying()) {
            return;
        }
        float left = Math.max(0.0F, getHealth() - 1.0F);
        setHealth(left);
        playSound(SoundEvents.FIRE_EXTINGUISH, SOUND_VOLUME, 1.0F);
        if (left <= 0.0F) {
            die(level.damageSources().magic());
        }
    }

    @Override
    public boolean onChargedWisp(ChargedWispEntity wisp) {
        if (tickCount <= 20) {
            return false;
        }
        changeType();
        shortCircuit();
        return true;
    }

    // ---------------------------------------------------------------- immunity
    /** Only /kill and the void get through; anything else only draws its anger. */
    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, damage);
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, damage);
        }
        if (source.getEntity() instanceof BrightestOneEntity || source.getDirectEntity() instanceof BrightestOneEntity) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            targeted = attacker;
            aggroCooldown = AGGRO_TICKS;
        } else if (source.getDirectEntity() instanceof LivingEntity attacker) {
            targeted = attacker;
            aggroCooldown = AGGRO_TICKS;
        }
        playSound(SoundEvents.FIRE_EXTINGUISH, SOUND_VOLUME, 1.0F);
        return false;
    }

    @Override
    public void heal(float amount) {
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /** TC4 EntityFlying: free flight with 0.91 drag. */
    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91F));
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.show(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.hide(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount <= 1) {
                level().addParticle(WispEffects.burst(1.0F), getX(), getY() + 0.45, getZ(), 0.0, 0.0, 0.0);
            }
            if (random.nextBoolean()) {
                level().addParticle(WispEffects.mote(random, aspectColor()), getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0.0, 0.0, 0.0);
            }
        }
    }

    // ---------------------------------------------------------------- behaviour (TC4 updateEntityActionState)
    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
        bossBar.update();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        fly();
        if (targeted != null && !targeted.isAlive()) {
            targeted = null;
        }
        --aggroCooldown;
        if (targeted == null || aggroCooldown-- <= 0) {
            targeted = level.getNearestPlayer(this, SEEK_RANGE);
            if (targeted instanceof Player p && (p.isCreative() || p.isSpectator())) {
                targeted = null;
            }
            if (targeted != null) {
                aggroCooldown = 50;
            }
        }
        if (targeted != null && targeted.distanceToSqr(this) < ATTACK_RANGE * ATTACK_RANGE) {
            double dx = targeted.getX() - getX();
            double dz = targeted.getZ() - getZ();
            setYRot((float) (-Math.atan2(dx, dz) * Mth.RAD_TO_DEG));
            yBodyRot = getYRot();
            if (hasLineOfSight(targeted)) {
                attackCounter++;
                if (continuousAttack && attackCounter < 10) {
                    continuous(level, targeted);
                } else if (attackCounter > 10) {
                    continuousAttack = false;
                }
                if (attackCounter == 20) {
                    strike(level, targeted);
                    callWisps(level);
                    attackCounter = -20 + random.nextInt(20);
                }
            } else if (attackCounter > 0) {
                attackCounter--;
            }
        } else {
            Vec3 v = getDeltaMovement();
            setYRot((float) (-Math.atan2(v.x, v.z) * Mth.RAD_TO_DEG));
            yBodyRot = getYRot();
            if (attackCounter > 0) {
                attackCounter--;
            }
        }
    }

    private void fly() {
        double dx = waypointX - getX();
        double dy = waypointY - getY();
        double dz = waypointZ - getZ();
        double d = dx * dx + dy * dy + dz * dz;
        if (d < 1.0 || d > 3600.0) {
            waypointX = getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
            waypointY = getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
            waypointZ = getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        }
        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += random.nextInt(5) + 2;
            d = Math.sqrt(d);
            if (d > 0.0 && courseTraversable(d)) {
                setDeltaMovement(getDeltaMovement().add(dx / d * 0.1, dy / d * 0.1, dz / d * 0.1));
            } else {
                waypointX = getX();
                waypointY = getY();
                waypointZ = getZ();
            }
        }
    }

    /** No block in the way, the waypoint not in liquid, and ground within 10 blocks below it. */
    private boolean courseTraversable(double distance) {
        double sx = (waypointX - getX()) / distance;
        double sy = (waypointY - getY()) / distance;
        double sz = (waypointZ - getZ()) / distance;
        AABB box = getBoundingBox();
        for (int i = 1; i < distance; i++) {
            box = box.move(sx, sy, sz);
            if (!level().noCollision(this, box)) {
                return false;
            }
        }
        BlockPos at = BlockPos.containing(waypointX, waypointY, waypointZ);
        if (!level().getFluidState(at).isEmpty()) {
            return false;
        }
        for (int a = 0; a < 11; a++) {
            if (!level().getBlockState(at.below(a)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private void continuous(ServerLevel level, LivingEntity target) {
        ResourceKey<IAspect> type = aspect();
        if (type.equals(TTAspects.AER)) {
            Vec3 look = getViewVector(1.0F);
            EntityGolemOrb orb = new EntityGolemOrb(level, this, target, false);
            orb.setPos(orb.getX() + look.x + (random.nextFloat() - 0.5F) * 5.0F, orb.getY() + 0.5, orb.getZ() + look.z + (random.nextFloat() - 0.5F) * 5.0F);
            double tx = target.getX() + target.getDeltaMovement().x - getX();
            double ty = target.getY() - getY() - target.getBbHeight() / 2.0F;
            double tz = target.getZ() + target.getDeltaMovement().z - getZ();
            orb.shoot(tx, ty, tz, 0.66F, 5.0F);
            level.playSound(null, getX(), getY(), getZ(), TTSounds.EGATTACK.get(), SoundSource.HOSTILE, 1.0F, 1.0F + random.nextFloat() * 0.1F);
            level.addFreshEntity(orb);
        } else if (type.equals(TTAspects.IGNIS)) {
            Vec3 look = getViewVector(1.0F);
            Vec3 eyes = getEyePosition();
            for (int i = 0; i < 30; i++) {
                Vec3 dir = look.add(random.nextGaussian() * 0.25, random.nextGaussian() * 0.25, random.nextGaussian() * 0.25).normalize();
                Vec3 motion = new Vec3(dir.x * (random.nextDouble() - 0.5), dir.y * random.nextDouble(), dir.z * (random.nextDouble() - 0.5));
                Vec3 from = eyes.add(motion.scale(8.0 + random.nextDouble()));
                level.addFreshEntity(new BrightEmberEntity(level, this, from, motion));
            }
        } else if (type.equals(TTAspects.TENEBRAE)) {
            lookAt(target, 100.0F, 100.0F);
            Shadowbeam.fire(level, this, 20);
        }
    }

    private void strike(ServerLevel level, LivingEntity target) {
        ResourceKey<IAspect> type = aspect();
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (type.equals(TTAspects.AER)) {
            if (random.nextInt(10) % 2 == 0) {
                zap(level, target);
                target.hurt(damageSources().mobAttack(this), damage);
            } else {
                continuousAttack = true;
            }
        } else if (type.equals(TTAspects.IGNIS)) {
            if (random.nextInt(6) % 2 == 0) {
                Vec3 look = getViewVector(1.0F);
                float yaw = getYRot() * Mth.DEG_TO_RAD;
                for (int i = 0; i < 10; i++) {
                    double x = getX() + look.x - Mth.cos(yaw) * 0.32F;
                    double y = getEyeY() - 0.3;
                    double z = getZ() + look.z - Mth.sin(yaw) * 0.32F;
                    ShardPowderEntity powder = new ShardPowderEntity(level, x, y, z, TTAspects.IGNIS, this, 0);
                    powder.setDeltaMovement(look.x * random.nextDouble(), look.y, look.z * random.nextDouble());
                    level.addFreshEntity(powder);
                }
            } else {
                continuousAttack = true;
            }
        } else if (type.equals(TTAspects.VACUOS)) {
            if (random.nextInt(10) % 2 == 0) {
                BlockPos floor = BlockPos.containing(target.getX(), target.getY() - 1.0, target.getZ());
                PortableHoles.open(level, floor, Direction.UP, 33, 120);
            } else {
                zap(level, target);
                target.hurt(damageSources().fellOutOfWorld(), damage / 4.0F);
            }
        } else if (type.equals(TTAspects.TENEBRAE)) {
            if (random.nextInt(10) % 2 == 0) {
                continuousAttack = true;
            } else {
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32.0))) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 30));
                }
            }
        }
    }

    private void zap(ServerLevel level, LivingEntity target) {
        level.playSound(null, getX(), getY(), getZ(), TTSounds.ZAP.get(), SoundSource.HOSTILE, 1.0F, 1.1F);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(this,
                new com.leclowndu93150.thaumaturge.network.ClientboundWispZapPayload(getId(), target.getId()));
    }

    private void callWisps(ServerLevel level) {
        List<WispEntity> near = level.getEntitiesOfClass(WispEntity.class, getBoundingBox().inflate(32.0));
        if (near.size() >= 8 || random.nextInt(3) != 2) {
            return;
        }
        int count = random.nextInt(3) + 3;
        for (int i = 0; i < count; i++) {
            WispEntity wisp = TTEntities.WISP.get().create(level);
            if (wisp == null) {
                return;
            }
            wisp.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360.0F, 0.0F);
            wisp.setAspect(TYPES.get(random.nextInt(TYPES.size())).location());
            level.addFreshEntity(wisp);
        }
    }

    // ---------------------------------------------------------------- drops, sounds, save
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        Holder<IAspect> aspect = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(aspect()).map(h -> (Holder<IAspect>) h).orElse(null);
        if (aspect != null) {
            ItemStack crystal = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get());
            crystal.set(TTDataComponents.CRYSTAL_ASPECT.get(), new AspectInstance(aspect, 1));
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1.5, getZ(), crystal));
        }
        level.sendParticles(WispEffects.burst(4.0F), getX(), getY() + 0.45, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    protected float getSoundVolume() {
        return SOUND_VOLUME;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return TTSounds.WISPLIVE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TTSounds.WISPDEAD.get();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
        super.addAdditionalSaveData(out.tag());
        out.putString("Type", entityData.get(TYPE));
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
        super.readAdditionalSaveData(in.tag());
        entityData.set(TYPE, in.getStringOr("Type", TTAspects.IGNIS.location().toString()));
    }
}
