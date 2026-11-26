package com.thaumaturgicconcilium.compat.witchery;

import dev.sterner.witchery.content.entity.WitcheryThrownPotion;
import dev.sterner.witchery.feature.affliction.vampire.VampirePlayerAttachment;
import dev.sterner.witchery.feature.affliction.werewolf.WerewolfPlayerAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Burned Witch (TC4 {@code BurnedWitch}): the ghost of a witch the hunters burned, flying on her broom (1 x 2, 30 HP,
 * fire-proof, 10 XP). She circles the nearest player within 100 blocks 8 blocks above them (radius 4-8, a slow bob)
 * and, while she can see them within 64, throws a Witchery brew every 30 ticks: one time in three the ill-fitting brew,
 * otherwise an attack brew (the anti-vampire or anti-werewolf one against a cursed player). The brews are data
 * ({@link BurnedWitchBrew}). Ghast voice a fifth up; drops from her loot table; gone in Peaceful; one per spawn group.
 *
 * <p>Port fixes: the original's "can see the target within 64" test squared the range twice (it reached 4096 blocks),
 * and its warning cry could never play (it checked the counter before the throw's own value). Witchery 0.5 has no gas
 * mask effect: she is instead immune to harmful effects, so her own splashes cannot hurt her.
 */
public class BurnedWitchEntity extends Monster {
    private static final double ORBIT_HEIGHT = 8.0;
    private static final double ORBIT_SPEED = 0.06;
    private static final double ORBIT_RADIUS_MIN = 4.0;
    private static final double ORBIT_RADIUS_MAX = 8.0;
    private static final double BOB_AMPLITUDE = 0.6;
    private static final double BOB_SPEED = 2.0;
    private static final double SEE_RANGE = 64.0;

    private double orbitAngle;
    private double orbitRadius;
    private int courseChangeCooldown;
    private double waypointX;
    private double waypointY;
    private double waypointZ;
    private @Nullable Player targeted;
    private int aggroCooldown;
    private int attackCounter;

    public BurnedWitchEntity(EntityType<? extends BurnedWitchEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.FOLLOW_RANGE, 100.0)
                .add(Attributes.FLYING_SPEED, 0.1);
    }

    public static boolean checkWitchSpawnRules(EntityType<BurnedWitchEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos,
                                               RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (targeted != null && (!targeted.isAlive() || targeted.isSpectator() || targeted.isCreative())) {
            targeted = null;
        }
        if (targeted == null || aggroCooldown-- <= 0) {
            Player nearest = level.getNearestPlayer(this, 100.0);
            targeted = nearest != null && !nearest.isCreative() && !nearest.isSpectator() ? nearest : null;
            if (targeted != null) {
                aggroCooldown = 20;
                orbitRadius = ORBIT_RADIUS_MIN + random.nextDouble() * (ORBIT_RADIUS_MAX - ORBIT_RADIUS_MIN);
                orbitAngle = random.nextDouble() * Math.PI * 2.0;
            }
        }
        setTarget(targeted);
        if (targeted != null) {
            orbitAngle += ORBIT_SPEED;
            waypointX = targeted.getX() + Math.cos(orbitAngle) * orbitRadius;
            waypointZ = targeted.getZ() + Math.sin(orbitAngle) * orbitRadius;
            waypointY = targeted.getY() + ORBIT_HEIGHT + Math.sin(orbitAngle * BOB_SPEED) * BOB_AMPLITUDE;
            if (courseChangeCooldown > 3) {
                courseChangeCooldown = 0;
            }
        } else {
            double d = distSqTo(waypointX, waypointY, waypointZ);
            if (d < 1.0 || d > 3600.0) {
                waypointX = getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                waypointY = getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                waypointZ = getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
            }
        }
        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += random.nextInt(5) + 2;
            double dist = Math.sqrt(distSqTo(waypointX, waypointY, waypointZ));
            if (dist > 0.0 && courseClear(dist)) {
                Vec3 v = getDeltaMovement();
                setDeltaMovement(v.add((waypointX - getX()) / dist * 0.1, (waypointY - getY()) / dist * 0.1, (waypointZ - getZ()) / dist * 0.1));
            } else {
                waypointX = getX();
                waypointY = getY();
                waypointZ = getZ();
            }
        }
        if (targeted != null && targeted.distanceToSqr(this) < SEE_RANGE * SEE_RANGE) {
            float yaw = -((float) Mth.atan2(targeted.getX() - getX(), targeted.getZ() - getZ())) * Mth.RAD_TO_DEG;
            setYRot(yaw);
            yBodyRot = yaw;
            if (hasLineOfSight(targeted)) {
                attackCounter++;
                if (attackCounter == 10) {
                    level.levelEvent(null, LevelEvent.SOUND_GHAST_WARNING, blockPosition(), 0);
                    throwBrew(level, targeted);
                    attackCounter = -20;
                }
            } else if (attackCounter > 0) {
                attackCounter--;
            }
        } else {
            Vec3 v = getDeltaMovement();
            if (v.horizontalDistanceSqr() > 1.0E-6) {
                float yaw = -((float) Mth.atan2(v.x, v.z)) * Mth.RAD_TO_DEG;
                setYRot(yaw);
                yBodyRot = yaw;
            }
            if (attackCounter > 0) {
                attackCounter--;
            }
        }
    }

    private double distSqTo(double x, double y, double z) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /** TC4 {@code isCourseTraversable}: the box swept toward the waypoint one block at a time hits nothing. */
    private boolean courseClear(double dist) {
        double sx = (waypointX - getX()) / dist;
        double sy = (waypointY - getY()) / dist;
        double sz = (waypointZ - getZ()) / dist;
        AABB box = getBoundingBox();
        for (int i = 1; i < dist; i++) {
            box = box.move(sx, sy, sz);
            if (!level().noCollision(this, box)) {
                return false;
            }
        }
        return true;
    }

    private void throwBrew(ServerLevel level, Player target) {
        ResourceKey<BurnedWitchBrew> kind = switch (random.nextInt(3)) {
            case 0 -> BurnedWitchBrew.ILL_FITTING;
            case 1 -> VampirePlayerAttachment.getData(target).getLevel() > 0 ? BurnedWitchBrew.ATTACK_VAMPIRE
                    : WerewolfPlayerAttachment.getData(target).getLevel() > 0 ? BurnedWitchBrew.ATTACK_WEREWOLF : BurnedWitchBrew.ATTACK;
            default -> BurnedWitchBrew.ATTACK;
        };
        ItemStack brew = BurnedWitchBrew.make(level, kind);
        if (brew.isEmpty()) {
            return;
        }
        WitcheryThrownPotion potion = new WitcheryThrownPotion(level, this, brew);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getEyeY() - 1.1 - potion.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        potion.shoot(dx, dy + dist * 0.2, dz, 0.75F, 8.0F);
        level.addFreshEntity(potion);
    }

    // ---- flying ----

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    /** In place of the 1.7.10 Witchery gas mask: no harmful effect takes hold (her own brews splash around her). */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(effect);
    }

    // ---- sounds (TC4: the ghast's, pitch 1.5) ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }
}
