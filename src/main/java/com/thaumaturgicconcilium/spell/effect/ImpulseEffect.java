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
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Impulse (and Vacuum, Pneumatic Strike): fires a compressed blast that flies {@code life + rand(life_random) +
 * life_per_extend * extend} ticks and explodes with strength {@code explosion + explosion_per_potency * potency} (block
 * damage follows mobGriefing). With {@code knock_radius} it throws creatures outward instead of exploding; with
 * {@code vacuum_radius} it drags non-player creatures along while it flies.
 */
public final class ImpulseEffect extends AbstractEffectBehavior {
    public static final MapCodec<ImpulseEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(e -> e.speed),
            Codec.INT.optionalFieldOf("life", 5).forGetter(e -> e.life),
            Codec.INT.optionalFieldOf("life_random", 20).forGetter(e -> e.lifeRandom),
            Codec.INT.optionalFieldOf("life_per_extend", 10).forGetter(e -> e.lifePerExtend),
            Codec.FLOAT.optionalFieldOf("explosion", 1.0F).forGetter(e -> e.explosion),
            Codec.FLOAT.optionalFieldOf("explosion_per_potency", 1.0F).forGetter(e -> e.explosionPerPotency),
            Codec.DOUBLE.optionalFieldOf("knock_radius", 0.0).forGetter(e -> e.knockRadius),
            Codec.DOUBLE.optionalFieldOf("knock_base", 0.0).forGetter(e -> e.knockBase),
            Codec.DOUBLE.optionalFieldOf("knock_per_potency", 0.5).forGetter(e -> e.knockPerPotency),
            Codec.DOUBLE.optionalFieldOf("vacuum_radius", 0.0).forGetter(e -> e.vacuumRadius),
            Codec.DOUBLE.optionalFieldOf("vacuum_strength", 2.0).forGetter(e -> e.vacuumStrength),
            Identifier.CODEC.optionalFieldOf("thump").forGetter(e -> e.thump)).apply(i, ImpulseEffect::new));

    private final double speed;
    private final int life;
    private final int lifeRandom;
    private final int lifePerExtend;
    private final float explosion;
    private final float explosionPerPotency;
    private final double knockRadius;
    private final double knockBase;
    private final double knockPerPotency;
    private final double vacuumRadius;
    private final double vacuumStrength;
    private final Optional<Identifier> thump;

    public ImpulseEffect(double speed, int life, int lifeRandom, int lifePerExtend, float explosion, float explosionPerPotency, double knockRadius,
            double knockBase, double knockPerPotency, double vacuumRadius, double vacuumStrength, Optional<Identifier> thump) {
        this.speed = speed;
        this.life = life;
        this.lifeRandom = lifeRandom;
        this.lifePerExtend = lifePerExtend;
        this.explosion = explosion;
        this.explosionPerPotency = explosionPerPotency;
        this.knockRadius = knockRadius;
        this.knockBase = knockBase;
        this.knockPerPotency = knockPerPotency;
        this.vacuumRadius = vacuumRadius;
        this.vacuumStrength = vacuumStrength;
        this.thump = thump;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.IMPULSE.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        float potency = SpellUtil.potency(ctx, power);
        int ticks = life + (lifeRandom > 0 ? ctx.random().nextInt(lifeRandom) : 0) + Math.round(lifePerExtend * SpellUtil.extend(ctx));
        CompressedBlastEntity.Blast blast = new CompressedBlastEntity.Blast(ticks, explosion + explosionPerPotency * potency, knockRadius,
                knockBase + knockPerPotency * potency, vacuumRadius, vacuumStrength, 0, 0, 0, thump);
        CompressedBlastEntity.launch(ctx.level(), target.position(), target.direction().normalize().scale(speed), blast, ItemStack.EMPTY, ctx.caster());
    }
}
