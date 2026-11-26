package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;

/**
 * Alive Limb (TC4 {@code ThaumGib}): one piece of a burst Overanimated, still fighting. Type 0 is the head, 1 the torso,
 * 2-3 the arms, 4-5 the legs (the renderer draws only that part). It leaps at its prey and hops when 2-6 blocks away.
 */
public class AliveLimbEntity extends Monster {
    public static final int TYPES = 6;
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(AliveLimbEntity.class, EntityDataSerializers.INT);

    public AliveLimbEntity(EntityType<? extends AliveLimbEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.85).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
    }

    public int limbType() {
        return entityData.get(TYPE);
    }

    public void setLimbType(int type) {
        entityData.set(TYPE, Math.floorMod(type, TYPES));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.63F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.6, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = getTarget();
        if (!level().isClientSide() && target != null && onGround()) {
            double d = distanceTo(target);
            if (d > 2.0 && d < 6.0 && random.nextInt(10) == 0) {
                setDeltaMovement(getDeltaMovement().x, 0.4, getDeltaMovement().z);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
        super.addAdditionalSaveData(out.tag());
        out.putInt("Type", limbType());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
        super.readAdditionalSaveData(in.tag());
        setLimbType(in.getIntOr("Type", 0));
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 65;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobUtil.tcSound("gargle_one").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobUtil.tcSound("gargle_pain").get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobUtil.tcSound("gargle_one").get();
    }
}
