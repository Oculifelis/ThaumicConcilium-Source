package com.thaumaturgicconcilium.compat.witchery;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Chort (TC4 {@code Chort}): a fire-proof imp with a tail (25 HP, 8 attack, speed 0.28, follow range 32) that spits three
 * gobs in a fan at anything within 16 blocks every 40 ticks. It grunts like a pig an octave down. It usually rides a
 * Sloppy Alchemist and shares its target with its mount. Drops (loot table): Foul Fume or Exhale of the Horned One
 * (0-3), or a Mandrake Root.
 *
 * <p>Cut: the original left alone players holding an imp "pact" (a 1.7.10 Witchery imp-gift hook). Witchery 0.5's imp
 * has no gift or deal to hook, so every player is a target.
 */
public class ChortEntity extends Monster {
    public ChortEntity(EntityType<? extends ChortEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 25.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new SpitGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The Sloppy Alchemist under it keeps its own behaviour (the Chort only aims and spits). */
    @Override
    public boolean canControlVehicle() {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && getVehicle() instanceof SloppyAlchemistEntity mount) {
            LivingEntity t = mount.getTarget();
            if (t != null && t.isAlive()) {
                setTarget(t);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }


    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    /**
     * TC4 {@code ChortAttackAI}: within 16 blocks, looks at the target and spits three gobs at -4, 0 and +4 degrees
     * (velocity 1.8, inaccuracy 1), then waits 40 ticks. Look only, so it also works from the saddle.
     */
    static final class SpitGoal extends Goal {
        private static final float RANGE = 16.0F;
        private final Mob mob;
        private @Nullable LivingEntity target;
        private int cooldown;

        SpitGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive() || mob.distanceToSqr(t) > RANGE * RANGE) {
                return false;
            }
            target = t;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && mob.getTarget() == target;
        }

        @Override
        public void start() {
            cooldown = 0;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            for (int i = -1; i < 2; i++) {
                ChortSpitEntity spit = new ChortSpitEntity(mob.level(), mob);
                spit.aim(mob, target, 4.0F * i, 1.8F, 1.0F);
                mob.level().addFreshEntity(spit);
            }
            cooldown = 40;
        }
    }
}
