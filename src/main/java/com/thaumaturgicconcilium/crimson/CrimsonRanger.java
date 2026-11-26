package com.thaumaturgicconcilium.crimson;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.entity.ai.CultistHurtByTargetGoal;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.entity.ShardPowderEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Crimson Ranger: a crimson cultist (30 HP) that keeps its distance and every second flings a spray of 4-9 motes of
 * ground aer or ignis vis crystal (phase 1's {@link ShardPowderEntity}), each already half-way to going off.
 */
public class CrimsonRanger extends EntityCultist implements KitingAttackGoal.KitingMob {
    private static final ResourceKey<LootTable> EQUIPMENT = ResourceKey.create(Registries.LOOT_TABLE, ThaumaturgicConcilium.id("equipment/crimson_ranger"));

    public CrimsonRanger(EntityType<? extends CrimsonRanger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCultist.createCultistAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new KitingAttackGoal(this, 20, 20.0F));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, false, t -> TCCrimson.isThaumaturge(t)));
    }

    @Override
    protected ResourceKey<LootTable> equipmentTable() {
        return EQUIPMENT;
    }

    @Override
    public boolean meleeMode() {
        return false;
    }

    @Override
    public void kiteAttack(ServerLevel level, LivingEntity target) {
        level.playSound(null, getX(), getY(), getZ(), TTSounds.ICE.get(), SoundSource.HOSTILE, 0.3F, 0.8F + getRandom().nextFloat() * 0.1F);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float pitch = getXRot() * Mth.DEG_TO_RAD;
        Vec3 look = getLookAngle();
        Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch)).normalize();
        int motes = 4 + getRandom().nextInt(6);
        for (int i = 0; i < motes; i++) {
            double x = getX() + look.x - Mth.cos(yaw) * 0.16;
            double y = getEyeY() + look.y - 0.5;
            double z = getZ() + look.z - Mth.sin(yaw) * 0.16;
            ShardPowderEntity mote = new ShardPowderEntity(level, x, y, z, getRandom().nextBoolean() ? TTAspects.AER : TTAspects.IGNIS, this,
                    ShardPowderEntity.MAX_AGE / 2);
            mote.setDeltaMovement(dir.add(getRandom().nextGaussian() * 0.09, getRandom().nextGaussian() * 0.09, getRandom().nextGaussian() * 0.09));
            level.addFreshEntity(mote);
        }
    }
}
