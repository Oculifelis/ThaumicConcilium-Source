package com.thaumaturgicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.Channel;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Cooling (and Refreeze): while held, throws a snowball every {@code max(1, interval - interval_per_potency *
 * potency)} ticks. With a {@code shard} (Refreeze) it fires that spell as a Thaumaturge projectile instead, at
 * {@code shard_speed} (a frost strike: TC4's frost shard).
 */
public final class CoolingEffect extends AbstractEffectBehavior {
    public static final MapCodec<CoolingEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("interval", 7).forGetter(e -> e.interval),
            Codec.FLOAT.optionalFieldOf("interval_per_potency", 1.0F).forGetter(e -> e.intervalPerPotency),
            Codec.FLOAT.optionalFieldOf("snowball_speed", 1.5F).forGetter(e -> e.snowballSpeed),
            Codec.FLOAT.optionalFieldOf("inaccuracy", 1.0F).forGetter(e -> e.inaccuracy),
            SpellNode.CODEC.optionalFieldOf("shard").forGetter(e -> e.shard),
            Codec.FLOAT.optionalFieldOf("shard_speed", 1.5F).forGetter(e -> e.shardSpeed),
            Identifier.CODEC.optionalFieldOf("sound").forGetter(e -> e.sound)).apply(i, CoolingEffect::new));

    private final int interval;
    private final float intervalPerPotency;
    private final float snowballSpeed;
    private final float inaccuracy;
    private final Optional<SpellNode> shard;
    private final float shardSpeed;
    private final Optional<Identifier> sound;

    public CoolingEffect(int interval, float intervalPerPotency, float snowballSpeed, float inaccuracy, Optional<SpellNode> shard, float shardSpeed,
            Optional<Identifier> sound) {
        this.interval = interval;
        this.intervalPerPotency = intervalPerPotency;
        this.snowballSpeed = snowballSpeed;
        this.inaccuracy = inaccuracy;
        this.shard = shard;
        this.shardSpeed = shardSpeed;
        this.sound = sound;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.COOLING.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        float potency = SpellUtil.potency(ctx, power);
        int every = Math.max(1, Math.round(interval - intervalPerPotency * potency));
        Channel.Session session = Channel.of(ctx);
        int shots = Math.min(20, session.due("shot", SpellUtil.pulseTicks(ctx), every));
        Vec3 dir = target.direction().normalize();
        Vec3 from = target.position().add(dir.scale(0.3));
        LivingEntity caster = ctx.caster();
        for (int k = 0; k < shots; k++) {
            if (shard.isPresent()) {
                SpellUtil.launchSpell(ctx, shard.get(), from, dir, shardSpeed, 0.0, SpellUtil.strength(ctx, power));
            } else {
                Snowball ball = caster != null ? new Snowball(ctx.level(), caster, new ItemStack(Items.SNOWBALL))
                        : new Snowball(ctx.level(), from.x, from.y, from.z, new ItemStack(Items.SNOWBALL));
                ball.setPos(from.x, from.y, from.z);
                ball.shoot(dir.x, dir.y, dir.z, snowballSpeed, inaccuracy);
                ctx.level().addFreshEntity(ball);
            }
            SpellUtil.play(ctx.level(), sound, from, 0.5F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
        }
    }
}
