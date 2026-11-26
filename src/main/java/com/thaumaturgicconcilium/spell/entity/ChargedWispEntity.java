package com.thaumaturgicconcilium.spell.entity;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Charged Wisp: a wisp the Vis Conductor (Wisp Launcher) overcharged and threw. It flies straight on and, every tick,
 * zaps every creature within {@code zap_radius} but its caster: {@code damage + damage_per_power * power} magic damage
 * and Flux Phage (Infectious Vis Exhaust). Entities implementing {@link ChargedWispTarget} get the hook instead (the
 * Brightest One, chained rifts). {@link #spawn} is public for the rift events.
 */
public class ChargedWispEntity extends Entity implements GlowingOrb {
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(ChargedWispEntity.class, EntityDataSerializers.STRING);

    /**
     * The zap.
     *
     * @param zapRadius       reach of the zap
     * @param damage          base damage
     * @param damagePerPower  extra damage per power
     * @param phageTicks      Flux Phage duration per power (at least one power's worth)
     * @param phageAmplifier  Flux Phage amplifier
     * @param zap             sound played now and then
     */
    public record Zap(double zapRadius, float damage, float damagePerPower, int phageTicks, int phageAmplifier, Optional<Identifier> zap) {
        public static final MapCodec<Zap> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("zap_radius", 4.0).forGetter(Zap::zapRadius),
                Codec.FLOAT.optionalFieldOf("damage", 10.0F).forGetter(Zap::damage),
                Codec.FLOAT.optionalFieldOf("damage_per_power", 1.0F).forGetter(Zap::damagePerPower),
                Codec.INT.optionalFieldOf("phage_ticks", 1000).forGetter(Zap::phageTicks),
                Codec.INT.optionalFieldOf("phage_amplifier", 30).forGetter(Zap::phageAmplifier),
                Identifier.CODEC.optionalFieldOf("zap_sound").forGetter(Zap::zap)).apply(i, Zap::new));

        public static final Zap DEFAULT = new Zap(4.0, 10.0F, 1.0F, 1000, 30, Optional.of(Identifier.fromNamespaceAndPath("thaumaturge", "jacobs")));
    }

    private Zap zap = Zap.DEFAULT;
    private float power;
    private int life = 40;
    private int age;
    private Optional<UUID> caster = Optional.empty();

    public ChargedWispEntity(EntityType<? extends ChargedWispEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /**
     * Spawns a charged wisp.
     *
     * @param level    the level
     * @param at       where
     * @param velocity blocks a tick
     * @param aspect   its aspect (colour; the Brightest One reads it)
     * @param caster   who threw it, never zapped; may be null (rift events)
     * @param power    TC4 potency
     * @param life     ticks to live
     * @param zap      the zap settings, {@link Zap#DEFAULT} for TC4's
     */
    public static ChargedWispEntity spawn(ServerLevel level, Vec3 at, Vec3 velocity, @Nullable ResourceKey<IAspect> aspect, @Nullable Entity caster,
            float power, int life, Zap zap) {
        ChargedWispEntity wisp = new ChargedWispEntity(TCSpells.CHARGED_WISP.get(), level);
        wisp.setPos(at.x, at.y, at.z);
        wisp.setDeltaMovement(velocity);
        wisp.entityData.set(ASPECT, (aspect == null ? TTAspects.AURAM : aspect).identifier().toString());
        wisp.caster = caster == null ? Optional.empty() : Optional.of(caster.getUUID());
        wisp.power = power;
        wisp.life = life;
        wisp.zap = zap;
        level.addFreshEntity(wisp);
        return wisp;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ASPECT, TTAspects.AURAM.identifier().toString());
    }

    public ResourceKey<IAspect> aspect() {
        return ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.parse(entityData.get(ASPECT)));
    }

    public Optional<UUID> caster() {
        return caster;
    }

    public float power() {
        return power;
    }

    public int age() {
        return age;
    }

    @Override
    public int glowColor() {
        Holder<IAspect> aspect = level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(aspect()).map(h -> (Holder<IAspect>) h).orElse(null);
        return aspect == null ? 0xFFFFFF : aspect.value().color();
    }

    @Override
    public float glowScale() {
        return 0.8F;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        age++;
        if (level().isClientSide()) {
            int c = glowColor();
            level().addParticle(new DustParticleOptions(c, 1.0F), getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (age > life) {
            discard();
            return;
        }
        Entity owner = caster.map(level::getEntity).orElse(null);
        DamageSource source = level.damageSources().indirectMagic(this, owner);
        float damage = zap.damage() + zap.damagePerPower() * power;
        for (Entity near : level.getEntities(this, getBoundingBox().inflate(zap.zapRadius()), e -> e.isAlive() && e != owner)) {
            if (near instanceof ChargedWispTarget target) {
                if (target.onChargedWisp(this)) {
                    discard();
                    return;
                }
                continue;
            }
            if (!(near instanceof LivingEntity living)) {
                continue;
            }
            living.hurtServer(level, source, damage);
            MobEffectInstance phage = new MobEffectInstance(TTMobEffects.INFECTIOUS_VIS_EXHAUST, (int) (zap.phageTicks() * Math.max(1.0F, power)), zap.phageAmplifier());
            living.addEffect(phage);
            zapFx(level, living);
            if (random.nextInt(50) == 0 && zap.zap().isPresent()) {
                Identifier id = zap.zap().get();
                Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.get(id).map(h -> (Holder<SoundEvent>) h)
                        .orElseGet(() -> Holder.direct(SoundEvent.createVariableRangeEvent(id)));
                level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
    }

    private void zapFx(ServerLevel level, LivingEntity victim) {
        Vec3 from = position();
        Vec3 to = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(from);
        int steps = Math.min(16, (int) (to.length() * 3) + 1);
        for (int s = 0; s <= steps; s++) {
            Vec3 p = from.add(to.scale(s / (double) steps));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        zap = in.read(Zap.CODEC).orElse(Zap.DEFAULT);
        power = in.getFloatOr("power", 0);
        life = in.getIntOr("life", 40);
        age = in.getIntOr("age", 0);
        caster = in.read("caster", UUIDUtil.CODEC);
        entityData.set(ASPECT, in.getStringOr("aspect", TTAspects.AURAM.identifier().toString()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.store(Zap.CODEC, zap);
        out.putFloat("power", power);
        out.putInt("life", life);
        out.putInt("age", age);
        caster.ifPresent(id -> out.store("caster", UUIDUtil.CODEC, id));
        out.putString("aspect", entityData.get(ASPECT));
    }
}
