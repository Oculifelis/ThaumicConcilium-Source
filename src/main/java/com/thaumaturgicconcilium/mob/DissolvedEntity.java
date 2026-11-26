package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import com.thaumaturgicconcilium.spell.SpellUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Dissolved (TC4 Concilium): a robed void walker that only falling out of the world or suffocating can hurt. Its touch
 * is out-of-world damage (ignores armour), and every 200 ticks it opens a portable hole under its target (see
 * {@link UpcomingHoleEntity}). Right-clicked with a caster holding Thaumaturge's Rift spell (the Portable Hole focus's
 * successor) it is torn apart.
 */
public class DissolvedEntity extends Monster {
    private static final ResourceKey<SpellPart> RIFT = ResourceKey.create(SpellPart.REGISTRY_KEY, Identifier.parse("thaumaturge:rift"));
    private static final int HOLE_INTERVAL = 200;

    public DissolvedEntity(EntityType<? extends DissolvedEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.4, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.4));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, MadThaumaturgeEntity.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !(source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
                || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean hit = target.hurtServer(level, damageSources().source(DamageTypes.FELL_OUT_OF_WORLD, this), damage);
        if (hit) {
            setLastHurtMob(target);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && tickCount % HOLE_INTERVAL == 0) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && target.onGround()) {
                UpcomingHoleEntity hole = new UpcomingHoleEntity(TCMobs.UPCOMING_HOLE.get(), level);
                hole.snapTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
                level.addFreshEntity(hole);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (SpellUtil.holdsPart(player.getItemInHand(hand), RIFT)) {
            if (level() instanceof ServerLevel level) {
                hurtServer(level, damageSources().source(DamageTypes.FELL_OUT_OF_WORLD, player), 9000.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new SparkleParticleOptions(0xFF4040FF, 1.0F, 0, 1.0F, 0.0F, 8, false), getX(), getY() + 1.0, getZ(), 15,
                    0.4, 0.8, 0.4, 0.02);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobUtil.sound("thaumaturge:egidle");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobUtil.sound("thaumaturge:egdeath");
    }
}
