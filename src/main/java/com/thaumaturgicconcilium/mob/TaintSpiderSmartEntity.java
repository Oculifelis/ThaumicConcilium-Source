package com.thaumaturgicconcilium.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Enslaved Taint Spider (TC4 {@code TaintSpiderSmart}): what the Taint Animation and Thought Manifestation spells make of
 * a taint crawler or a mind spider. Thaumaturge has no taint spider, so it has the taint crawler's body and numbers,
 * but it never goes for players: it attacks only what its caster points it at, or what hurts it (players excepted).
 * No drops. It is in {@code thaumaturge:tainted}, so the spell can command it again.
 */
public class TaintSpiderSmartEntity extends Monster {
    public TaintSpiderSmartEntity(EntityType<? extends TaintSpiderSmartEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.275).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.3F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof Player ? null : target);
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobUtil.sound("thaumaturge:gore");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobUtil.sound("thaumaturge:gore");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobUtil.sound("thaumaturge:gore");
    }
}
