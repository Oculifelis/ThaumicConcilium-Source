package com.thaumaturgicconcilium.spell.entity;

import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Concentrated Warp Charge: a cube of its owner's warp that follows them around and hurts every creature inside it
 * by {@code permanent warp / 10 * power} a tick (unless the owner is under Warp Ward). The focus version feeds on the
 * owner's temporary warp (one per creature hit, and it dies when there is none); the {@link #byForce} version, the
 * Crimson Pontifex's spell, lives 200 ticks and also hurts its owner (the player it was cast on).
 */
public class ConcentratedWarpChargeEntity extends Entity implements GlowingOrb {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ConcentratedWarpChargeEntity.class, EntityDataSerializers.INT);

    /**
     * How a charge behaves.
     *
     * @param power             damage multiplier (TC4: potency + 1)
     * @param range             TC4 Enlarge rank, widens Mass Hysteria
     * @param byForce           the Pontifex version: hurts its owner, ignores temporary warp, dies after {@code life}
     * @param life              ticks to live, 0 for as long as the owner holds out
     * @param hysteria          Mass Hysteria: passes the owner's bad effects to others
     * @param flagellation      Self-Flagellation: takes other players' bad effects onto the owner
     * @param followDistSq      squared distance beyond which it is pulled back
     * @param teleportDistSq    squared distance beyond which it jumps to the owner
     * @param sink              fall speed a tick unless the owner flies
     * @param damping           velocity kept each tick
     * @param damagePerWarp     damage per point of the owner's permanent warp, times power
     * @param effectInterval    ticks between Mass Hysteria / Self-Flagellation transfers
     * @param effectRadius      base radius of those transfers
     * @param effectRadiusPerRange extra radius per range
     * @param whisperInterval   ticks between whispers
     * @param whisper           the whisper sound
     */
    public record Charge(float power, float range, boolean byForce, int life, boolean hysteria, boolean flagellation, double followDistSq,
            double teleportDistSq, double sink, double damping, float damagePerWarp, int effectInterval, double effectRadius,
            double effectRadiusPerRange, int whisperInterval, Optional<Identifier> whisper) {
        public static final MapCodec<Charge> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("power", 1.0F).forGetter(Charge::power),
                Codec.FLOAT.optionalFieldOf("range", 0.0F).forGetter(Charge::range),
                Codec.BOOL.optionalFieldOf("by_force", false).forGetter(Charge::byForce),
                Codec.INT.optionalFieldOf("life", 0).forGetter(Charge::life),
                Codec.BOOL.optionalFieldOf("mass_hysteria", false).forGetter(Charge::hysteria),
                Codec.BOOL.optionalFieldOf("self_flagellation", false).forGetter(Charge::flagellation),
                Codec.DOUBLE.optionalFieldOf("follow_dist_sq", 49.0).forGetter(Charge::followDistSq),
                Codec.DOUBLE.optionalFieldOf("teleport_dist_sq", 225.0).forGetter(Charge::teleportDistSq),
                Codec.DOUBLE.optionalFieldOf("sink", 0.1).forGetter(Charge::sink),
                Codec.DOUBLE.optionalFieldOf("damping", 0.7).forGetter(Charge::damping),
                Codec.FLOAT.optionalFieldOf("damage_per_warp", 0.1F).forGetter(Charge::damagePerWarp),
                Codec.INT.optionalFieldOf("effect_interval", 10).forGetter(Charge::effectInterval),
                Codec.DOUBLE.optionalFieldOf("effect_radius", 5.0).forGetter(Charge::effectRadius),
                Codec.DOUBLE.optionalFieldOf("effect_radius_per_range", 4.0).forGetter(Charge::effectRadiusPerRange),
                Codec.INT.optionalFieldOf("whisper_interval", 120).forGetter(Charge::whisperInterval),
                Identifier.CODEC.optionalFieldOf("whisper").forGetter(Charge::whisper)).apply(i, Charge::new));

        public static final Charge DEFAULT = new Charge(1.0F, 0.0F, false, 0, false, false, 49.0, 225.0, 0.1, 0.7, 0.1F, 10, 5.0, 4.0, 120,
                Optional.of(Identifier.fromNamespaceAndPath("thaumaturge", "whispers")));

        public Charge withPower(float power, float range) {
            return new Charge(power, range, byForce, life, hysteria, flagellation, followDistSq, teleportDistSq, sink, damping, damagePerWarp, effectInterval,
                    effectRadius, effectRadiusPerRange, whisperInterval, whisper);
        }
    }

    /** The Crimson Pontifex's charge: power 5, range 1, 200 ticks, keeps within 3 blocks, hurts its owner too. */
    public static final Charge BY_FORCE = new Charge(5.0F, 1.0F, true, 200, false, false, 9.0, 225.0, 0.1, 0.7, 0.1F, 10, 5.0, 4.0, 120,
            Optional.of(Identifier.fromNamespaceAndPath("thaumaturge", "whispers")));

    private Charge charge = Charge.DEFAULT;
    private Optional<UUID> owner = Optional.empty();
    private int age;

    public ConcentratedWarpChargeEntity(EntityType<? extends ConcentratedWarpChargeEntity> type, Level level) {
        super(type, level);
    }

    /** Spawns a charge owned by {@code owner} at {@code at}. */
    public static ConcentratedWarpChargeEntity spawn(ServerLevel level, LivingEntity owner, Vec3 at, Charge charge) {
        ConcentratedWarpChargeEntity entity = new ConcentratedWarpChargeEntity(TCSpells.WARP_CHARGE.get(), level);
        entity.setPos(at.x, at.y, at.z);
        entity.charge = charge;
        entity.owner = Optional.of(owner.getUUID());
        entity.entityData.set(OWNER_ID, owner.getId());
        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * The Crimson Pontifex's spell (TC4 "byForce"): a charge on {@code victim}, power 5, range 1, 200 ticks, that hurts
     * the victim itself.
     */
    public static ConcentratedWarpChargeEntity byForce(ServerLevel level, LivingEntity victim) {
        return spawn(level, victim, victim.position().add(0, 1, 0), BY_FORCE);
    }

    public Optional<UUID> owner() {
        return owner;
    }

    public boolean isOwnedBy(Entity entity) {
        return owner.map(id -> id.equals(entity.getUUID())).orElse(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, -1);
    }

    @Override
    public int glowColor() {
        return 0x5D016B;
    }

    @Override
    public float glowScale() {
        return 1.0F + 0.1F * (float) Math.sin(tickCount * 0.3);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        Level level = level();
        if (level.isClientSide()) {
            clientFx(level);
            return;
        }
        ServerLevel server = (ServerLevel) level;
        LivingEntity master = owner.map(server::getEntity).filter(e -> e instanceof LivingEntity).map(LivingEntity.class::cast).orElse(null);
        if (master == null || !master.isAlive() || master.level() != level || (charge.life() > 0 && age > charge.life())) {
            discard();
            return;
        }
        Player player = master instanceof Player p ? p : null;
        if (!charge.byForce() && player != null && !player.hasInfiniteMaterials() && WarpHelper.getWarp(player).get(WarpType.TEMPORARY) <= 0) {
            player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.no_warp").withStyle(ChatFormatting.DARK_PURPLE));
            discard();
            return;
        }
        follow(master, player);
        hurtInside(server, master, player);
        if (charge.effectInterval() > 0 && age % charge.effectInterval() == 0) {
            double radius = charge.effectRadius() + charge.effectRadiusPerRange() * charge.range();
            if (charge.hysteria()) {
                passOn(server, master, radius);
            }
            if (charge.flagellation()) {
                takeOn(server, master, radius);
            }
        }
        if (charge.whisperInterval() > 0 && age % charge.whisperInterval() == 0 && charge.whisper().isPresent()) {
            Identifier id = charge.whisper().get();
            Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.get(id).map(h -> (Holder<SoundEvent>) h)
                    .orElseGet(() -> Holder.direct(SoundEvent.createVariableRangeEvent(id)));
            server.playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, 0.5F, 0.8F + random.nextFloat() * 0.4F);
        }
    }

    private void follow(LivingEntity master, @Nullable Player player) {
        double distSq = distanceToSqr(master);
        Vec3 v = getDeltaMovement();
        if (distSq > charge.teleportDistSq()) {
            setPos(master.getX(), master.getY() + 1, master.getZ());
            v = Vec3.ZERO;
        } else if (distSq > charge.followDistSq()) {
            v = master.position().add(0, 1, 0).subtract(position()).normalize();
        }
        boolean flying = player != null && player.getAbilities().flying;
        if (!flying) {
            v = v.add(0, -charge.sink(), 0);
        }
        v = v.scale(charge.damping());
        setDeltaMovement(v);
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
    }

    private void hurtInside(ServerLevel level, LivingEntity master, @Nullable Player player) {
        if (master.hasEffect(TTMobEffects.WARP_WARD)) {
            return;
        }
        int warp = player == null ? 0 : WarpHelper.getWarp(player).get(WarpType.PERMANENT);
        float damage = warp * charge.damagePerWarp() * charge.power();
        if (damage <= 0) {
            return;
        }
        DamageSource source = level.damageSources().indirectMagic(this, master);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox(), Entity::isAlive)) {
            if (victim instanceof WarpChargeImmune || (victim == master && !charge.byForce())) {
                continue;
            }
            if (victim.hurtServer(level, source, damage) && !charge.byForce() && player instanceof ServerPlayer sp && !sp.hasInfiniteMaterials()) {
                WarpHelper.getWarp(sp).reduce(WarpType.TEMPORARY, 1);
            }
        }
    }

    private static List<MobEffectInstance> bad(LivingEntity entity) {
        List<MobEffectInstance> out = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                out.add(effect);
            }
        }
        return out;
    }

    private void passOn(ServerLevel level, LivingEntity master, double radius) {
        List<MobEffectInstance> bad = bad(master);
        if (bad.isEmpty()) {
            return;
        }
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, master.getBoundingBox().inflate(radius), e -> e.isAlive() && e != master);
        if (near.isEmpty()) {
            return;
        }
        MobEffectInstance moved = bad.get(random.nextInt(bad.size()));
        master.removeEffect(moved.getEffect());
        near.get(random.nextInt(near.size())).addEffect(new MobEffectInstance(moved));
    }

    private void takeOn(ServerLevel level, LivingEntity master, double radius) {
        for (Player other : level.getEntitiesOfClass(Player.class, master.getBoundingBox().inflate(radius), e -> e.isAlive() && e != master)) {
            List<MobEffectInstance> bad = bad(other);
            if (!bad.isEmpty()) {
                MobEffectInstance moved = bad.get(random.nextInt(bad.size()));
                other.removeEffect(moved.getEffect());
                master.addEffect(new MobEffectInstance(moved));
                return;
            }
        }
    }

    private void clientFx(Level level) {
        level.addParticle(ParticleTypes.PORTAL, getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble(), getZ() + (random.nextDouble() - 0.5), 0, 0, 0);
        if (age % 3 == 0) {
            Entity master = level.getEntity(entityData.get(OWNER_ID));
            if (master != null) {
                Vec3 from = position().add(0, 0.5, 0);
                Vec3 to = master.position().add(0, master.getBbHeight() * 0.5, 0).subtract(from);
                int steps = Math.min(24, (int) (to.length() * 3));
                for (int s = 0; s < steps; s++) {
                    Vec3 p = from.add(to.scale(s / (double) steps));
                    level.addParticle(ParticleTypes.WITCH, p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        charge = in.read(Charge.CODEC).orElse(Charge.DEFAULT);
        owner = in.read("owner", UUIDUtil.CODEC);
        age = in.getIntOr("age", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.store(Charge.CODEC, charge);
        owner.ifPresent(id -> out.store("owner", UUIDUtil.CODEC, id));
        out.putInt("age", age);
    }
}
