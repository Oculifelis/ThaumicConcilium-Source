package com.thaumaturgicconcilium.crimson;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/**
 * Paranoid Warrior (TC4 Concilium's "Samurai"). 50 HP, a flat damage divisor instead of armour, a hard-hitting blade,
 * and below a third of its health it backs off and throws bolts. Its kind (Thaumium / Void / Shadow) is a
 * {@link ParanoidWarriorVariant} picked by weight when it spawns; port fix: the original rolled the weights and then
 * overwrote them with a uniform pick, so the 50 / 40 / 10 weights never applied.
 */
public class ParanoidWarrior extends Monster implements KitingAttackGoal.KitingMob {
    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    private @Nullable ResourceKey<ParanoidWarriorVariant> variantKey;
    private @Nullable ParanoidWarriorVariant variant;

    public ParanoidWarrior(EntityType<? extends ParanoidWarrior> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        for (EquipmentSlot slot : SLOTS) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ARMOR, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new KitingAttackGoal(this, 40, 20.0F));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, t -> TCCrimson.isThaumaturge(t)));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
    }

    // ---- variant ----

    public ParanoidWarriorVariant variant() {
        if (variant == null && variantKey != null) {
            variant = level().registryAccess().registry(ParanoidWarriorVariant.REGISTRY_KEY).flatMap(r -> r.getOptional(variantKey)).orElse(null);
        }
        return variant != null ? variant : ParanoidWarriorVariant.FALLBACK;
    }

    /** Weighted pick over every variant in the registry (empty registry: the fallback). */
    private void pickVariant() {
        Optional<Registry<ParanoidWarriorVariant>> reg = level().registryAccess().registry(ParanoidWarriorVariant.REGISTRY_KEY);
        List<Holder.Reference<ParanoidWarriorVariant>> all = new ArrayList<>();
        reg.ifPresent(r -> r.holders().forEach(all::add));
        int total = all.stream().mapToInt(h -> h.value().weight()).sum();
        if (total <= 0) {
            variantKey = null;
            variant = ParanoidWarriorVariant.FALLBACK;
            return;
        }
        int roll = getRandom().nextInt(total);
        for (Holder.Reference<ParanoidWarriorVariant> h : all) {
            roll -= h.value().weight();
            if (roll < 0) {
                variantKey = h.key();
                variant = h.value();
                return;
            }
        }
    }

    private void dress() {
        for (EquipmentSlot slot : SLOTS) {
            setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
        equip(variant().equipment(), java.util.Map.of());
        for (EquipmentSlot slot : SLOTS) {
            setDropChance(slot, 0.0F);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData groupData) {
        pickVariant();
        dress();
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public void tick() {
        super.tick();
        if (variantKey == null && variant == null && level() instanceof ServerLevel) {
            // Made without finalizeSpawn (a structure without finalisation, a command with data): pick now.
            pickVariant();
            dress();
        }
    }

    // ---- combat ----

    @Override
    public boolean meleeMode() {
        return getHealth() > getMaxHealth() / 3.0F;
    }

    @Override
    public void meleeTick(LivingEntity target, double horizontalDistSq) {
        if (getHealth() > getMaxHealth() / 2.0F && horizontalDistSq > 64.0 && getRandom().nextInt(10) > 7) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 3));
        }
    }

    /** A bolt of the variant's colour for 2 x (kind + 1) mob damage (the original's PacketFXLightning, as Thaumaturge's bolt). */
    @Override
    public void kiteAttack(ServerLevel level, LivingEntity target) {
        ParanoidWarriorVariant v = variant();
        level.playSound(null, getX(), getY(), getZ(), TTSounds.SHOCK.get(), SoundSource.HOSTILE, 0.25F, 1.0F);
        target.hurt(damageSources().mobAttack(this), v.boltDamage());
        int c = v.boltColor();
        level.sendParticles(new BoltParticleOptions(target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                        ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, 0.3F),
                getX(), getY() + getBbHeight() - 0.5, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        ServerLevel level = (ServerLevel) this.level();
        AttributeBonus bonus = new AttributeBonus(this, variant().bonusDamage());
        try {
            return super.doHurtTarget(target);
        } finally {
            bonus.remove();
        }
    }

    /** The original divided incoming damage by the kind's divisor and skipped armour altogether. */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float damage) {
        return damage / variant().damageDivisor();
    }

    @Override
    public int getArmorValue() {
        return 20;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof ParanoidWarrior || super.isAlliedTo(other);
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    // ---- sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return TCCrimson.sound("speech");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TCCrimson.sound("uagh");
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 450;
    }

    // ---- loot ----

    /** The kind's own table (its nuggets) on top of the warrior's loot table. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(variant().loot());
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, position()).withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
                .create(LootContextParamSets.ENTITY);
        table.getRandomItems(params, s -> spawnAtLocation(s));
    }

    // ---- save ----

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
        super.addAdditionalSaveData(out.tag());
        if (variantKey != null) {
            out.putString("variant", variantKey.location().toString());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
        super.readAdditionalSaveData(in.tag());
        in.getString("variant").ifPresent(s -> {
            variantKey = ResourceKey.create(ParanoidWarriorVariant.REGISTRY_KEY, ResourceLocation.parse(s));
            variant = null;
        });
    }

    /** A transient attack-damage bonus for one hit (the kind's extra blade damage). */
    private record AttributeBonus(ParanoidWarrior mob, float amount) {
        private static final ResourceLocation ID = com.thaumaturgicconcilium.ThaumaturgicConcilium.id("paranoid_warrior_kind");

        AttributeBonus {
            var attr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attr != null && amount != 0.0F) {
                attr.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ID, amount,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            }
        }

        void remove() {
            var attr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attr != null) {
                attr.removeModifier(ID);
            }
        }
    }
}
