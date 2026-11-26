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
import com.thaumaturgicconcilium.spell.entity.WrathEffectEntity;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Wrath Attraction (and Burnout): every hostile creature within {@code radius + radius_per_enlarge * enlarge} turns on
 * the caster, and the word "IRA" burns in the air. Burnout also weakens them ({@code effect} for {@code effect_ticks}).
 */
public final class WrathAttractionEffect extends AbstractEffectBehavior {
    public static final MapCodec<WrathAttractionEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("radius", 5.0).forGetter(e -> e.radius),
            Codec.DOUBLE.optionalFieldOf("radius_per_enlarge", 5.0).forGetter(e -> e.radiusPerEnlarge),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().optionalFieldOf("effect").forGetter(e -> e.effect),
            Codec.INT.optionalFieldOf("effect_ticks", 100).forGetter(e -> e.effectTicks),
            Codec.INT.optionalFieldOf("effect_amplifier", 3).forGetter(e -> e.effectAmplifier),
            ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(e -> e.sound)).apply(i, WrathAttractionEffect::new));

    private final double radius;
    private final double radiusPerEnlarge;
    private final Optional<Holder<MobEffect>> effect;
    private final int effectTicks;
    private final int effectAmplifier;
    private final Optional<ResourceLocation> sound;

    public WrathAttractionEffect(double radius, double radiusPerEnlarge, Optional<Holder<MobEffect>> effect, int effectTicks, int effectAmplifier,
            Optional<ResourceLocation> sound) {
        this.radius = radius;
        this.radiusPerEnlarge = radiusPerEnlarge;
        this.effect = effect;
        this.effectTicks = effectTicks;
        this.effectAmplifier = effectAmplifier;
        this.sound = sound;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.WRATH_ATTRACTION.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        Vec3 centre = SpellUtil.onCaster(ctx, target) && caster != null ? caster.position() : target.position();
        double r = radius + radiusPerEnlarge * SpellUtil.enlarge(ctx);
        SpellUtil.play(ctx.level(), sound, centre, 1.0F, 1.0F);
        WrathEffectEntity.spawn(ctx.level(), centre.x, centre.y, centre.z, caster != null ? caster.getYRot() : 0.0F, effect.isPresent());
        for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, new AABB(centre, centre).inflate(r), m -> m instanceof Enemy && m.isAlive())) {
            if (caster != null && mob != caster && !ctx.isAlly(mob)) {
                mob.setTarget(caster);
            }
            effect.ifPresent(e -> mob.addEffect(new MobEffectInstance(e, effectTicks, effectAmplifier)));
        }
    }
}
