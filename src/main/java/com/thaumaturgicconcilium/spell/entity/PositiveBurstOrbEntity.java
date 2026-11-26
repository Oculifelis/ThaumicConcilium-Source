package com.thaumaturgicconcilium.spell.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Positive Burst orb: falls and bounces like an experience orb, drifts toward the nearest player within
 * {@code home_radius} other than the caster, and gives whoever touches it (the caster too, as in TC4) its boon.
 */
public class PositiveBurstOrbEntity extends Entity implements GlowingOrb {
    /** What an orb gives on touch. */
    public enum Kind implements StringRepresentable {
        HEAL("heal"), ABSORPTION("absorption"), BUFF("buff");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * An orb's settings.
     *
     * @param kind             heal (plain), absorption (Fulfillment) or a random buff (Vitaminize)
     * @param heal             health healed by a plain orb
     * @param absorptionCap    Fulfillment: absorption is raised by the orb's power up to this
     * @param buffTicks        Vitaminize: base duration
     * @param buffTicksPerPower Vitaminize: extra duration per power
     * @param buffs            Vitaminize: one of these, amplifier = power
     * @param life             ticks before the orb fades
     * @param health           hits it takes
     * @param homeRadius       range it seeks players in
     */
    public record Boon(Kind kind, float heal, float absorptionCap, int buffTicks, int buffTicksPerPower, List<Holder<MobEffect>> buffs, int life, float health,
            double homeRadius) {
        public static final MapCodec<Boon> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Kind.CODEC.optionalFieldOf("kind", Kind.HEAL).forGetter(Boon::kind),
                Codec.FLOAT.optionalFieldOf("heal", 0.5F).forGetter(Boon::heal),
                Codec.FLOAT.optionalFieldOf("absorption_cap", 20.0F).forGetter(Boon::absorptionCap),
                Codec.INT.optionalFieldOf("buff_ticks", 100).forGetter(Boon::buffTicks),
                Codec.INT.optionalFieldOf("buff_ticks_per_power", 40).forGetter(Boon::buffTicksPerPower),
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().listOf().optionalFieldOf("buffs", List.of()).forGetter(Boon::buffs),
                Codec.INT.optionalFieldOf("life", 150).forGetter(Boon::life),
                Codec.FLOAT.optionalFieldOf("health", 5.0F).forGetter(Boon::health),
                Codec.DOUBLE.optionalFieldOf("home_radius", 8.0).forGetter(Boon::homeRadius)).apply(i, Boon::new));

        public static final Boon DEFAULT = new Boon(Kind.HEAL, 0.5F, 20.0F, 100, 40, List.of(), 150, 5.0F, 8.0);
    }

    private Boon boon = Boon.DEFAULT;
    private int power;
    private int age;
    private float health = 5.0F;
    private Optional<UUID> caster = Optional.empty();

    public PositiveBurstOrbEntity(EntityType<? extends PositiveBurstOrbEntity> type, Level level) {
        super(type, level);
    }

    public static PositiveBurstOrbEntity spawn(ServerLevel level, Vec3 at, Vec3 velocity, Boon boon, int power, Entity caster) {
        PositiveBurstOrbEntity orb = new PositiveBurstOrbEntity(TCSpells.POSITIVE_BURST_ORB.get(), level);
        orb.setPos(at.x, at.y, at.z);
        orb.setDeltaMovement(velocity);
        orb.boon = boon;
        orb.power = power;
        orb.health = boon.health();
        orb.caster = caster == null ? Optional.empty() : Optional.of(caster.getUUID());
        level.addFreshEntity(orb);
        return orb;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public int glowColor() {
        return 0xA5A1C1;
    }

    @Override
    public float glowScale() {
        return 0.35F;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement().add(0, -0.03, 0);
        if (level() instanceof ServerLevel server) {
            Player seek = server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(boon.homeRadius()),
                    p -> p.isAlive() && !p.isSpectator() && caster.map(id -> !id.equals(p.getUUID())).orElse(true))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (seek != null) {
                Vec3 to = seek.getEyePosition().subtract(position()).scale(1.0 / boon.homeRadius());
                double pull = 1.0 - to.length();
                if (pull > 0) {
                    v = v.add(to.normalize().scale(pull * pull * 0.1));
                }
            }
        }
        setDeltaMovement(v);
        move(MoverType.SELF, getDeltaMovement());
        float friction = onGround() ? 0.588F : 0.98F;
        setDeltaMovement(getDeltaMovement().multiply(friction, 0.98, friction));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1, -0.9, 1));
        }
        if (level() instanceof ServerLevel server) {
            for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.3), p -> p.isAlive() && !p.isSpectator())) {
                give(server, p);
                discard();
                return;
            }
            if (++age >= boon.life()) {
                discard();
            }
        } else if (random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    private void give(ServerLevel level, Player player) {
        switch (boon.kind()) {
            case ABSORPTION -> player.setAbsorptionAmount(Math.min(boon.absorptionCap(), player.getAbsorptionAmount() + power));
            case BUFF -> {
                if (!boon.buffs().isEmpty()) {
                    Holder<MobEffect> effect = boon.buffs().get(random.nextInt(boon.buffs().size()));
                    player.addEffect(new MobEffectInstance(effect, boon.buffTicks() + boon.buffTicksPerPower() * power, power));
                }
            }
            default -> player.heal(boon.heal());
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F,
                0.5F * ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.8F));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source)) {
            return false;
        }
        health -= amount;
        if (health <= 0) {
            discard();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        boon = in.read(Boon.CODEC).orElse(Boon.DEFAULT);
        power = in.getIntOr("power", 0);
        age = in.getIntOr("age", 0);
        health = in.getFloatOr("health", boon.health());
        caster = in.read("caster", UUIDUtil.CODEC);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.store(Boon.CODEC, boon);
        out.putInt("power", power);
        out.putInt("age", age);
        out.putFloat("health", health);
        caster.ifPresent(id -> out.store("caster", UUIDUtil.CODEC, id));
    }
}
