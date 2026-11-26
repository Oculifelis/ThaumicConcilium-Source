package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Overanimated (TC4 Concilium, Thaumic Horizons content): a body held together by too much life. When it dies it does
 * not fall: it bursts in a spray of blood into six Alive Limbs (head, torso, two arms, two legs) that keep fighting.
 */
public class OveranimatedEntity extends Monster {
    public OveranimatedEntity(EntityType<? extends OveranimatedEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.6, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
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
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    protected void tickDeath() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            for (int type = 0; type < AliveLimbEntity.TYPES; type++) {
                AliveLimbEntity limb = new AliveLimbEntity(TCMobs.ALIVE_LIMB.get(), level);
                limb.setLimbType(type);
                double up = type == 0 ? 1.5 : type <= 3 ? 1.0 : 0.0;
                limb.snapTo(getX(), getBoundingBox().minY + up, getZ(), random.nextFloat() * 360.0F, 0.0F);
                double angle = Math.PI * 2.0 * type / AliveLimbEntity.TYPES;
                limb.setDeltaMovement(new Vec3(Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5));
                limb.setTarget(getTarget());
                level.addFreshEntity(limb);
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), getX(), getY() + 1.0, getZ(), 60,
                    0.4, 0.6, 0.4, 0.15);
            discard();
        }
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
        return MobUtil.tcSound("gargle_many").get();
    }
}
