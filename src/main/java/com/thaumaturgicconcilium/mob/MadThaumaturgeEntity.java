package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Mad Thaumaturge (TC4 Concilium): a melee lunatic with a void hoe or shovel that jumps on the back of whoever hit it
 * last (9%/tick within 2 blocks), hacks at its mount every 10-19 ticks and lets go 20% of the time. (Players cannot
 * carry passengers on a modern server, so it clings: it is held on the mount's back each tick instead of riding.) Hunts players,
 * Thaumaturges and cultists. A player kill gives a research hint with the data's {@code hint_chance} (20%); the item
 * drops are its loot table. Max health is 20, set explicitly (the original relied on the vanilla default).
 */
public class MadThaumaturgeEntity extends Monster {
    private int rideAttackIn;
    private @Nullable LivingEntity mount;

    public MadThaumaturgeEntity(EntityType<? extends MadThaumaturgeEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? TTItems.VOID_HOE.get() : TTItems.VOID_SHOVEL.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return out;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (mount != null) {
            if (!mount.isAlive() || mount.level() != level() || mount.isRemoved() || isPassenger()) {
                letGo();
                return;
            }
            // Players cannot carry passengers on the server, so it clings instead: kept on the mount's back each tick.
            moveTo(mount.getX(), mount.getY() + mount.getBbHeight() * 0.55, mount.getZ(), mount.getYRot(), getXRot());
            setDeltaMovement(mount.getDeltaMovement());
            fallDistance = 0.0F;
            if (--rideAttackIn <= 0) {
                rideAttackIn = 10 + random.nextInt(10);
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(mount);
                if (random.nextInt(5) == 0) {
                    letGo();
                }
            }
            return;
        }
        LivingEntity attacker = getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && !isPassenger() && distanceToSqr(attacker) <= 4.0 && random.nextInt(100) > 90
                && !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()))
                && level.getEntitiesOfClass(MadThaumaturgeEntity.class, attacker.getBoundingBox().inflate(1.0), m -> m.mount == attacker).isEmpty()) {
            mount = attacker;
            rideAttackIn = 10 + random.nextInt(10);
            setNoGravity(true);
        }
    }

    private void letGo() {
        mount = null;
        setNoGravity(false);
    }

    /** The creature it is clinging to (TC4: riding), or null. */
    public @Nullable LivingEntity mount() {
        return mount;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && source.getEntity() instanceof ServerPlayer player) {
            ThaumaturgeTrade.get(level, ThaumaturgeTrade.MAD_THAUMATURGE).ifPresent(t -> {
                if (random.nextFloat() < t.hintChance()) {
                    MobUtil.giveHint(player, t.hints(), random);
                }
            });
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobUtil.tcSound("sad").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobUtil.tcSound("sad").get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }
}
