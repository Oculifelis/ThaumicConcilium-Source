package com.thaumaturgicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.Channel;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spotlight (and Warming Up, Taint Purification); needs the Ring of Blustering Light. While held, a beam reaches the
 * creature or block looked at within {@code reach}.
 *
 * <ul>
 * <li>Damage mode: whenever the creature is out of its hurt cooldown it takes {@code damage + seconds held *
 * damage_per_second_potency * potency} magic damage, so the beam ramps with hold time.</li>
 * <li>Warm mode: the creature gets {@code effect} for {@code effect_ticks} at amplifier {@code effect_amplifier +
 * potency} instead.</li>
 * <li>Purify mode: a block in a {@code tainted_biomes} biome turns its column to {@code biome}; tainted animals become
 * their {@code cures} counterpart; other tainted or eldritch creatures take the damage divided by
 * {@code tainted_damage_divisor}.</li>
 * </ul>
 * Letting go puts the caster on a cooldown of {@code cooldown + cooldown_per_tick * ticks held} ticks.
 */
public final class SpotlightEffect extends AbstractEffectBehavior {
    /** What the beam does. */
    public enum Mode implements StringRepresentable {
        DAMAGE("damage"), WARM("warm"), PURIFY("purify");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<SpotlightEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.DAMAGE).forGetter(e -> e.mode),
            Codec.DOUBLE.optionalFieldOf("reach", 50.0).forGetter(e -> e.reach),
            Codec.FLOAT.optionalFieldOf("damage", 1.0F).forGetter(e -> e.damage),
            Codec.FLOAT.optionalFieldOf("damage_per_second_potency", 1.0F).forGetter(e -> e.damagePerSecondPotency),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().optionalFieldOf("effect").forGetter(e -> e.effect),
            Codec.INT.optionalFieldOf("effect_ticks", 20).forGetter(e -> e.effectTicks),
            Codec.INT.optionalFieldOf("effect_amplifier", 5).forGetter(e -> e.effectAmplifier),
            Purify.CODEC.forGetter(e -> e.purify),
            Codec.INT.optionalFieldOf("cooldown", 20).forGetter(e -> e.cooldown),
            Codec.FLOAT.optionalFieldOf("cooldown_per_tick", 2.0F).forGetter(e -> e.cooldownPerTick),
            Codec.INT.optionalFieldOf("hum_interval", 10).forGetter(e -> e.humInterval),
            ResourceLocation.CODEC.optionalFieldOf("hum").forGetter(e -> e.hum),
            Codec.INT.optionalFieldOf("beam_color", 0xFFFF00).forGetter(e -> e.beamColor)).apply(i, SpotlightEffect::new));

    /** Taint Purification's settings. */
    public record Purify(ResourceLocation biome, TagKey<Biome> taintedBiomes, Map<ResourceLocation, ResourceLocation> cures, float taintedDamageDivisor) {
        static final MapCodec<Purify> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("biome", ResourceLocation.fromNamespaceAndPath("thaumaturge", "magical_forest")).forGetter(Purify::biome),
                TagKey.codec(Registries.BIOME).optionalFieldOf("tainted_biomes", TagKey.create(Registries.BIOME,
                        ResourceLocation.fromNamespaceAndPath("thaumaturge", "is_tainted"))).forGetter(Purify::taintedBiomes),
                Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).optionalFieldOf("cures", Map.of()).forGetter(Purify::cures),
                Codec.FLOAT.optionalFieldOf("tainted_damage_divisor", 10.0F).forGetter(Purify::taintedDamageDivisor)).apply(i, Purify::new));
    }

    private final Mode mode;
    private final double reach;
    private final float damage;
    private final float damagePerSecondPotency;
    private final Optional<Holder<MobEffect>> effect;
    private final int effectTicks;
    private final int effectAmplifier;
    private final Purify purify;
    private final int cooldown;
    private final float cooldownPerTick;
    private final int humInterval;
    private final Optional<ResourceLocation> hum;
    private final int beamColor;

    public SpotlightEffect(Mode mode, double reach, float damage, float damagePerSecondPotency, Optional<Holder<MobEffect>> effect, int effectTicks,
            int effectAmplifier, Purify purify, int cooldown, float cooldownPerTick, int humInterval, Optional<ResourceLocation> hum, int beamColor) {
        this.mode = mode;
        this.reach = reach;
        this.damage = damage;
        this.damagePerSecondPotency = damagePerSecondPotency;
        this.effect = effect;
        this.effectTicks = effectTicks;
        this.effectAmplifier = effectAmplifier;
        this.purify = purify;
        this.cooldown = cooldown;
        this.cooldownPerTick = cooldownPerTick;
        this.humInterval = humInterval;
        this.hum = hum;
        this.beamColor = beamColor;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.SPOTLIGHT.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        if (!SpellUtil.ringGate(caster)) {
            if (caster instanceof Player p) {
                p.stopUsingItem();
            }
            return;
        }
        ServerLevel level = ctx.level();
        float potency = SpellUtil.potency(ctx, power);
        Channel.Session session = Channel.of(ctx);
        long now = level.getGameTime();
        int held = session.held(now);
        if (caster instanceof Player player && session.fresh()) {
            session.onRelease(s -> {
                int ticks = Math.round(cooldown + cooldownPerTick * s.held(player.level().getGameTime()));
                CasterManager.setCooldown(player, ticks);
                player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), ticks);
            });
        }
        if (session.due("hum", SpellUtil.pulseTicks(ctx), humInterval) > 0) {
            SpellUtil.play(level, hum, target.position(), 0.4F, 1.0F);
        }
        Vec3 from = target.position();
        LivingEntity victim;
        Optional<BlockHitResult> block = Optional.empty();
        Vec3 end;
        if (SpellUtil.onCaster(ctx, target)) {
            victim = SpellUtil.living(ctx, target, reach).orElse(null);
            block = victim == null ? SpellUtil.block(ctx, target, reach) : Optional.empty();
            end = victim != null ? victim.position().add(0, victim.getBbHeight() * 0.5, 0)
                    : block.map(BlockHitResult::getLocation).orElse(from.add(target.direction().normalize().scale(reach)));
            if (caster != null) {
                from = caster.getEyePosition().subtract(0, 0.2, 0);
            }
        } else {
            victim = target.entity().filter(e -> e instanceof LivingEntity l && l.isAlive()).map(LivingEntity.class::cast).orElse(null);
            block = target.block();
            end = target.position();
            from = caster != null ? caster.getEyePosition() : end;
        }
        beam(level, from, end);
        float hurt = damage + (held / 20) * damagePerSecondPotency * potency;
        if (victim != null && victim != caster) {
            hit(ctx, level, caster, victim, hurt, potency);
        }
        if (mode == Mode.PURIFY && block.isPresent()) {
            cleanse(level, block.get().getBlockPos());
        }
    }

    private void hit(CastContext ctx, ServerLevel level, LivingEntity caster, LivingEntity victim, float hurt, float potency) {
        switch (mode) {
            case WARM -> effect.ifPresent(e -> victim.addEffect(new MobEffectInstance(e, effectTicks, effectAmplifier + Math.round(potency))));
            case PURIFY -> {
                ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
                ResourceLocation cure = purify.cures().get(type);
                if (cure != null) {
                    SpellUtil.create(level, cure, victim.position()).ifPresent(e -> {
                        e.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
                        level.addFreshEntity(e);
                        victim.discard();
                    });
                } else if (victim.getType().is(ThaumaturgeEntityTypeTags.TAINTED) || victim.getType().is(ThaumaturgeEntityTypeTags.ELDRITCH)) {
                    strike(level, caster, victim, hurt / purify.taintedDamageDivisor());
                }
            }
            default -> strike(level, caster, victim, hurt);
        }
    }

    private static void strike(ServerLevel level, LivingEntity caster, LivingEntity victim, float amount) {
        if (victim.invulnerableTime == 0 && amount > 0) {
            victim.hurt(SpellUtil.magic(level, caster, caster), amount);
        }
    }

    private void beam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int steps = Math.min(100, (int) (d.length() * 2));
        DustParticleOptions dust = new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(beamColor).toVector3f(), 1.0F);
        for (int s = 1; s <= steps; s++) {
            Vec3 p = from.add(d.scale(s / (double) steps));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Turns the column of a tainted-biome block back into the purify biome. */
    private void cleanse(ServerLevel level, BlockPos pos) {
        Holder<Biome> current = level.getBiome(pos);
        if (!current.is(purify.taintedBiomes())) {
            return;
        }
        Optional<Holder.Reference<Biome>> clean = level.registryAccess().lookupOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, purify.biome()));
        if (clean.isEmpty()) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(pos);
        int qx = QuartPos.fromBlock(pos.getX());
        int qz = QuartPos.fromBlock(pos.getZ());
        Holder<Biome> target = clean.get();
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> x == qx && z == qz ? target : chunk.getNoiseBiome(x, y, z),
                level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.<ChunkAccess>of(chunk));
    }
}
