package com.thaumaturgicconcilium.crimson;

import com.forsakenmagic.FMRegistry;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.entity.ai.CultistHurtByTargetGoal;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTWandParts;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Crimson Paladin: a crimson cultist caster (60 HP) that some Crimson Knights turn into when they spawn (the data map
 * {@code spawn_replacement}). It carries a greatwood staff with Forsaken Magic's alchemical caps. From 8 to 24 blocks
 * it casts every 20 to 40 ticks, half the time Ethereal Shackles at a player, otherwise a boomerang Crimson Orb; up
 * close it hits for 8.
 */
public class CrimsonPaladin extends EntityCultist implements RangedAttackMob {
    private static final ResourceKey<LootTable> EQUIPMENT = ResourceKey.create(Registries.LOOT_TABLE, ThaumaturgicConcilium.id("equipment/crimson_paladin"));

    public CrimsonPaladin(EntityType<? extends CrimsonPaladin> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCultist.createCultistAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24.0F) {
            @Override
            public boolean canUse() {
                return farEnough() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return farEnough() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, false, (t, l) -> TCCrimson.isThaumaturge(t)));
    }

    /** TC4's AILongRangeAttack only cast from 8 blocks out; closer the melee goal takes over. */
    private boolean farEnough() {
        LivingEntity t = getTarget();
        return t != null && distanceToSqr(t) >= 64.0;
    }

    @Override
    protected ResourceKey<LootTable> equipmentTable() {
        return EQUIPMENT;
    }

    /** The staff: a Thaumaturge wand item with the greatwood staff rod and Forsaken Magic's alchemical caps. */
    public static ItemStack staff() {
        return ItemWand.create(TTItems.WAND.get(), FMRegistry.CAP_ALCHEMICAL.get(), TTWandParts.STAFF_GREATWOOD.get(), false);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        setItemSlot(EquipmentSlot.MAINHAND, staff());
        return data;
    }

    /** Plain mob damage, no weapon bonus (the staff is not a weapon). */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        swing(InteractionHand.MAIN_HAND);
        boolean hurt = target.hurtServer(level, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (hurt) {
            setLastHurtMob(target);
        }
        return hurt;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, getX(), getY(), getZ(), TCCrimson.sound("spell"), SoundSource.HOSTILE, 1.0F, 1.0F);
        // Port fix: the original's shackles roll did nothing against a non-player; it casts the orb instead.
        if (target instanceof Player && getRandom().nextBoolean()) {
            EtherealShacklesEntity shackles = new EtherealShacklesEntity(level, this);
            double dx = target.getX() + target.getDeltaMovement().x - getX();
            double dy = target.getY(0.5) - shackles.getY();
            double dz = target.getZ() + target.getDeltaMovement().z - getZ();
            shackles.shoot(dx, dy, dz, 2.0F, 2.0F);
            level.addFreshEntity(shackles);
            level.playSound(null, shackles.getX(), shackles.getY(), shackles.getZ(), TCCrimson.sound("shackles_throw"), SoundSource.HOSTILE, 0.5F, 1.0F);
        } else {
            Vec3 look = target.getEyePosition().subtract(getEyePosition()).normalize();
            lookAt(target, 100.0F, 100.0F);
            level.addFreshEntity(new CrimsonOrbEntity(level, getX() + look.x, getEyeY(), getZ() + look.z, look, this));
        }
        swing(InteractionHand.MAIN_HAND);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TCCrimson.sound("chant");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TCCrimson.sound("growl");
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 450;
    }
}
