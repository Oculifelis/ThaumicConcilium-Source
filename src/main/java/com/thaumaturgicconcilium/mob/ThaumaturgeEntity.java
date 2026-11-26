package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTWandParts;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import org.jspecify.annotations.Nullable;

/**
 * Thaumaturge (TC4 Concilium): a neutral wandering wizard. With its wand it throws five embers; without it, it fights
 * in melee. It hunts cultists, zombies, skeletons, eldritch and tainted creatures, and players only when angry: a
 * player who hits one angers every Thaumaturge within 32x16x32 for 400-799 ticks. A player who has finished the
 * Thaumaturges research can trade with it (see {@link ThaumaturgeTrade}); others get a taunt.
 */
public class ThaumaturgeEntity extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(ThaumaturgeEntity.class, EntityDataSerializers.INT);
    public static final Identifier TRADE_RESEARCH = ThaumaturgicConcilium.id("thaumaturges");
    private static final int EMBERS = 5;

    public ThaumaturgeEntity(EntityType<? extends ThaumaturgeEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGER, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 20, 40, 15.0F) {
            @Override
            public boolean canUse() {
                return hasWand() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.6, false) {
            @Override
            public boolean canUse() {
                return !hasWand() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
        goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> true));
        goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
        goalSelector.addGoal(11, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (t, l) -> isAngry()));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                (t, l) -> t.typeHolder().is(ThaumaturgeEntityTypeTags.ELDRITCH)));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                (t, l) -> t.typeHolder().is(ThaumaturgeEntityTypeTags.TAINTED)));
    }

    public boolean hasWand() {
        return getMainHandItem().is(TTItems.WAND.get());
    }

    public boolean isAngry() {
        return entityData.get(ANGER) > 0;
    }

    public void anger(int ticks) {
        entityData.set(ANGER, ticks);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, ItemWand.create(TTItems.WAND.get(), TTWandParts.CAP_IRON.get(), TTWandParts.ROD_WOOD.get(), false));
        setDropChance(EquipmentSlot.MAINHAND, 0.1F);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return out;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (isAngry() && random.nextInt(5) == 0) {
                level().addParticle(ParticleTypes.ANGRY_VILLAGER, getRandomX(0.6), getRandomY() + 0.5, getRandomZ(0.6), 0, 0, 0);
            }
            return;
        }
        int anger = entityData.get(ANGER);
        if (anger > 0) {
            entityData.set(ANGER, anger - 1);
        }
        if (tickCount % 40 == 0 && getHealth() < getMaxHealth()) {
            heal(1.0F);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof Player player && !player.isCreative()) {
            List<ThaumaturgeEntity> kin = level.getEntitiesOfClass(ThaumaturgeEntity.class, getBoundingBox().inflate(32.0, 16.0, 32.0));
            for (ThaumaturgeEntity t : kin) {
                t.anger(400 + random.nextInt(400));
                t.setTarget(player);
            }
        }
        return hurt;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        Vec3 from = new Vec3(getX(), getEyeY() - 0.1, getZ());
        Vec3 dir = target.getEyePosition().subtract(from);
        for (int i = 0; i < EMBERS; i++) {
            EmberEntity ember = new EmberEntity(level(), this, from);
            ember.shoot(dir.x, dir.y, dir.z, 1.5F, 4.5F);
            level().addFreshEntity(ember);
        }
        playSound(SoundEvents.FIRECHARGE_USE, 0.4F, 1.0F + random.nextFloat() * 0.2F);
        swing(InteractionHand.MAIN_HAND);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() || held.is(Items.NAME_TAG) || isAngry() || !isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer sp) {
            if (KnowledgeAccess.of(sp).isResearchComplete(TRADE_RESEARCH)) {
                sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ThaumaturgeMenu(id, inv, this), getDisplayName()),
                        buf -> buf.writeVarInt(getId()));
            } else {
                MobUtil.chat(sp, "message.thaumaturgicconcilium.thaumaturge.taunt", 4, random);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A wanderer, not a creature of the dark: spawns and strolls in daylight too. */
    @Override
    public float getWalkTargetValue(net.minecraft.core.BlockPos pos, net.minecraft.world.level.LevelReader level) {
        return 0.0F;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobUtil.tcSound("think").get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("anger", entityData.get(ANGER));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(ANGER, in.getIntOr("anger", 0));
    }
}
