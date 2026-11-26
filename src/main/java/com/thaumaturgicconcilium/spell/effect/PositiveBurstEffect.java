package com.thaumaturgicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.TCSpells;
import com.thaumaturgicconcilium.spell.entity.PositiveBurstOrbEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Positive Burst (and Fulfillment, Vitaminize): sprays {@code count + rand(count_random + count_random_per_potency *
 * potency)} orbs at {@code speed} along the aim, scattered by {@code spread}. The orbs seek players other than the
 * caster and heal, shield or buff whoever picks them up (see the {@code orb} settings).
 */
public final class PositiveBurstEffect extends AbstractEffectBehavior {
    public static final MapCodec<PositiveBurstEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("count", 4).forGetter(e -> e.count),
            Codec.INT.optionalFieldOf("count_random", 5).forGetter(e -> e.countRandom),
            Codec.FLOAT.optionalFieldOf("count_random_per_potency", 1.0F).forGetter(e -> e.countRandomPerPotency),
            Codec.DOUBLE.optionalFieldOf("speed", 0.4).forGetter(e -> e.speed),
            Codec.DOUBLE.optionalFieldOf("spread", 0.25).forGetter(e -> e.spread),
            PositiveBurstOrbEntity.Boon.CODEC.codec().optionalFieldOf("orb", PositiveBurstOrbEntity.Boon.DEFAULT).forGetter(e -> e.orb))
            .apply(i, PositiveBurstEffect::new));

    private final int count;
    private final int countRandom;
    private final float countRandomPerPotency;
    private final double speed;
    private final double spread;
    private final PositiveBurstOrbEntity.Boon orb;

    public PositiveBurstEffect(int count, int countRandom, float countRandomPerPotency, double speed, double spread, PositiveBurstOrbEntity.Boon orb) {
        this.count = count;
        this.countRandom = countRandom;
        this.countRandomPerPotency = countRandomPerPotency;
        this.speed = speed;
        this.spread = spread;
        this.orb = orb;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.POSITIVE_BURST.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        RandomSource random = ctx.random();
        float potency = SpellUtil.potency(ctx, power);
        int bound = Math.round(countRandom + countRandomPerPotency * potency);
        int n = count + (bound > 0 ? random.nextInt(bound) : 0);
        Vec3 dir = target.direction().normalize();
        Vec3 from = SpellUtil.onCaster(ctx, target) ? target.position().add(dir.scale(0.5)) : target.position();
        int orbPower = Math.round(potency);
        for (int k = 0; k < n; k++) {
            Vec3 v = dir.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread).normalize().scale(speed);
            PositiveBurstOrbEntity.spawn(ctx.level(), from, v, orb, orbPower, ctx.caster());
        }
    }
}
