package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Quicksilver Elemental (TC4 Concilium): a living blob of quicksilver that only fire, or Slowness on it, can hurt. Its
 * size follows its health (hp/30). Its blows hit as hard as the victim's own attack damage (anvil damage), and every 40
 * ticks it gives its target Thaumaturge's Flux Phage (infectious vis exhaust) and strips one beneficial effect.
 * Hurt while standing in a Thaumaturge crucible, it melts into it: the crucible becomes a Quicksilver Crucible holding
 * 20 Permutatio, and players within 16 learn the Quicksilver Crucible research.
 */
public class QuicksilverElementalEntity extends Monster {
    public static final ResourceLocation CRUCIBLE = ResourceLocation.parse("thaumaturge:crucible");
    public static final ResourceLocation RESEARCH = ThaumaturgicConcilium.id("quicksilver_crucible");
    private static final ResourceLocation PHAGE = ResourceLocation.parse("thaumaturge:infectious_vis_exhaust");
    private static final float FULL = 30.0F;
    private float lastScale = 1.0F;

    public QuicksilverElementalEntity(EntityType<? extends QuicksilverElementalEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.6).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
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

    /** Health as a fraction of full health: its size and render scale. */
    public float sizeScale() {
        return Math.max(0.2F, getHealth() / FULL);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(sizeScale());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        boolean allowed = source.is(DamageTypeTags.IS_FIRE) || hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        return !allowed || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, amount);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive()) {
            meltIntoCrucible(level);
        }
        return hurt;
    }

    private void meltIntoCrucible(ServerLevel level) {
        Block crucible = BuiltInRegistries.BLOCK.getOptional(CRUCIBLE).orElse(null);
        if (crucible == null) {
            return;
        }
        for (BlockPos pos : new BlockPos[] {blockPosition(), blockPosition().below()}) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(crucible)) {
                continue;
            }
            level.removeBlockEntity(pos);
            level.setBlockAndUpdate(pos, TCMobs.QUICKSILVER_CRUCIBLE.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof QuicksilverCrucibleBlockEntity be) {
                be.setPermutatio(QuicksilverCrucibleBlockEntity.CONVERTED_START);
            }
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16.0))) {
                MobUtil.unlockResearch(player, RESEARCH);
            }
            playSound(MobUtil.tcSound("melted").get(), 2.0F, 1.0F);
            discard();
            return;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        ServerLevel level = (ServerLevel) this.level();
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (target instanceof LivingEntity living && living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            damage = (float) living.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        boolean hit = target.hurt(damageSources().anvil(this), damage);
        if (hit) {
            setLastHurtMob(target);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        float scale = sizeScale();
        if (scale != lastScale) {
            lastScale = scale;
            refreshDimensions();
        }
        if (!(level() instanceof ServerLevel) || tickCount % 40 != 0) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        BuiltInRegistries.MOB_EFFECT.getHolder(PHAGE).ifPresent(h -> target.addEffect(new MobEffectInstance(h, 1000, 20), this));
        List<Holder<MobEffect>> good = new ArrayList<>();
        for (MobEffectInstance e : target.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                good.add(e.getEffect());
            }
        }
        if (!good.isEmpty()) {
            target.removeEffect(good.get(random.nextInt(good.size())));
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 65;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobUtil.tcSound("melted").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobUtil.tcSound("melted").get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ITEM_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }
}
