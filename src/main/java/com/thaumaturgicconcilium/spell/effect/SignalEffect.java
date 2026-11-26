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
import com.thaumaturgicconcilium.spell.entity.CompressedBlastEntity;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.phys.Vec3;

/**
 * Signal (and Flashbang): fires a compressed blast that bursts into a firework. The colour and shape are the part's
 * {@code color} setting (the 16 dyes, 16 = a random colour each shot; TC4 cycled it by sneak-using) and {@code shape}
 * setting (0 small ball, 1 large ball, 2 star, 3 creeper, 4 burst, 5 a random one; TC4's rank-1 upgrades). Flashbang
 * blinds everything within {@code blind_radius} when it bursts.
 */
public final class SignalEffect extends AbstractEffectBehavior {
    private static final FireworkExplosion.Shape[] SHAPES = {FireworkExplosion.Shape.SMALL_BALL, FireworkExplosion.Shape.LARGE_BALL,
            FireworkExplosion.Shape.STAR, FireworkExplosion.Shape.CREEPER, FireworkExplosion.Shape.BURST};
    private static final int RAINBOW = 16;
    private static final int RANDOM_SHAPE = 5;

    public static final MapCodec<SignalEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(e -> e.speed),
            Codec.INT.optionalFieldOf("life", 15).forGetter(e -> e.life),
            Codec.INT.optionalFieldOf("life_random", 20).forGetter(e -> e.lifeRandom),
            Codec.INT.optionalFieldOf("life_per_extend", 10).forGetter(e -> e.lifePerExtend),
            Codec.FLOAT.optionalFieldOf("flicker_chance", 0.05F).forGetter(e -> e.flicker),
            Codec.FLOAT.optionalFieldOf("trail_chance", 0.05F).forGetter(e -> e.trail),
            Codec.DOUBLE.optionalFieldOf("blind_radius", 0.0).forGetter(e -> e.blindRadius),
            Codec.INT.optionalFieldOf("blind_ticks", 100).forGetter(e -> e.blindTicks),
            Codec.INT.optionalFieldOf("blind_amplifier", 3).forGetter(e -> e.blindAmplifier),
            ResourceLocation.CODEC.optionalFieldOf("thump").forGetter(e -> e.thump)).apply(i, SignalEffect::new));

    private final double speed;
    private final int life;
    private final int lifeRandom;
    private final int lifePerExtend;
    private final float flicker;
    private final float trail;
    private final double blindRadius;
    private final int blindTicks;
    private final int blindAmplifier;
    private final Optional<ResourceLocation> thump;

    public SignalEffect(double speed, int life, int lifeRandom, int lifePerExtend, float flicker, float trail, double blindRadius, int blindTicks,
            int blindAmplifier, Optional<ResourceLocation> thump) {
        this.speed = speed;
        this.life = life;
        this.lifeRandom = lifeRandom;
        this.lifePerExtend = lifePerExtend;
        this.flicker = flicker;
        this.trail = trail;
        this.blindRadius = blindRadius;
        this.blindTicks = blindTicks;
        this.blindAmplifier = blindAmplifier;
        this.thump = thump;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.SIGNAL.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        RandomSource random = ctx.random();
        int colorIndex = ctx.setting("color");
        int color = colorIndex >= RAINBOW || colorIndex < 0 ? Mth.hsvToRgb(random.nextFloat(), 1.0F, 1.0F) : DyeColor.byId(colorIndex).getFireworkColor();
        int shapeIndex = ctx.setting("shape");
        FireworkExplosion.Shape shape = SHAPES[shapeIndex >= RANDOM_SHAPE ? random.nextInt(SHAPES.length) : Mth.clamp(shapeIndex, 0, SHAPES.length - 1)];
        FireworkExplosion explosion = new FireworkExplosion(shape, IntList.of(color), IntList.of(), random.nextFloat() < trail, random.nextFloat() < flicker);
        int ticks = life + (lifeRandom > 0 ? random.nextInt(lifeRandom) : 0) + Math.round(lifePerExtend * SpellUtil.extend(ctx));
        CompressedBlastEntity.Blast blast = new CompressedBlastEntity.Blast(ticks, 0.0F, 0, 0, 0, 0, blindRadius, blindTicks, blindAmplifier, thump);
        Vec3 dir = target.direction().normalize();
        CompressedBlastEntity.launch(ctx.level(), target.position(), dir.scale(speed), blast, CompressedBlastEntity.firework(explosion), ctx.caster());
    }
}
