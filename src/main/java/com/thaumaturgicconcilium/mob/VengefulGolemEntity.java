package com.thaumaturgicconcilium.mob;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Vengeful Golem (TC4 Concilium, Automagy content): a golem with a brain in a jar that never fights with its hands.
 * Its "ranged attack" is mind control: a player in its sight is, 80% of the time, sent an {@link EnslavePayload} that
 * makes them stumble about at random (with a 40% chance of a taunt); another creature is sent walking to a random spot
 * within 10 blocks. Always faces its target.
 */
public class VengefulGolemEntity extends Monster implements RangedAttackMob {
    public VengefulGolemEntity(EntityType<? extends VengefulGolemEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RangedAttackGoal(this, 0.5, 20, 40, 24.0F));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = getTarget();
        if (target != null) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (target instanceof ServerPlayer player) {
            if (random.nextFloat() < 0.8F && hasLineOfSight(player)) {
                swing(InteractionHand.MAIN_HAND);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, getSoundSource(), 1.0F,
                        0.5F + random.nextFloat() * 0.3F);
                if (random.nextFloat() < 0.4F) {
                    MobUtil.chat(player, "message.thaumaturgicconcilium.golem.taunt", 4, random);
                }
                PacketDistributor.sendToPlayer(player, new EnslavePayload(true));
            } else {
                PacketDistributor.sendToPlayer(player, new EnslavePayload(false));
            }
        } else if (target instanceof Mob mob && random.nextFloat() < 0.9F) {
            swing(InteractionHand.MAIN_HAND);
            mob.getNavigation().moveTo(mob.getX() + random.nextInt(21) - 10, mob.getY() + random.nextInt(3) - 1, mob.getZ() + random.nextInt(21) - 10, 0.7);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobUtil.tcSound("gnaw").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobUtil.tcSound("gnaw").get();
    }
}
