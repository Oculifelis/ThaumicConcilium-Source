package com.thaumaturgicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.equipment.runic.RunicShieldState;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.Channel;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Scribing: every {@code interval} ticks while held, the player looked at within {@code reach + reach_per_enlarge *
 * enlarge} (or reached by a delivery) gains {@code charge + charge_per_potency * potency} runic shielding, up to their
 * maximum absorption (their runic shield size). Players with no runic shield get nothing, as in TC4.
 */
public final class ScribingEffect extends AbstractEffectBehavior {
    public static final MapCodec<ScribingEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("interval", 10).forGetter(e -> e.interval),
            Codec.DOUBLE.optionalFieldOf("reach", 5.0).forGetter(e -> e.reach),
            Codec.DOUBLE.optionalFieldOf("reach_per_enlarge", 5.0).forGetter(e -> e.reachPerEnlarge),
            Codec.FLOAT.optionalFieldOf("charge", 2.0F).forGetter(e -> e.charge),
            Codec.FLOAT.optionalFieldOf("charge_per_potency", 1.0F).forGetter(e -> e.chargePerPotency),
            Identifier.CODEC.optionalFieldOf("sound").forGetter(e -> e.sound)).apply(i, ScribingEffect::new));

    private final int interval;
    private final double reach;
    private final double reachPerEnlarge;
    private final float charge;
    private final float chargePerPotency;
    private final Optional<Identifier> sound;

    public ScribingEffect(int interval, double reach, double reachPerEnlarge, float charge, float chargePerPotency, Optional<Identifier> sound) {
        this.interval = interval;
        this.reach = reach;
        this.reachPerEnlarge = reachPerEnlarge;
        this.charge = charge;
        this.chargePerPotency = chargePerPotency;
        this.sound = sound;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.SCRIBING.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        int due = Channel.of(ctx).due("scribe", SpellUtil.pulseTicks(ctx), interval);
        if (due <= 0) {
            return;
        }
        double r = reach + reachPerEnlarge * SpellUtil.enlarge(ctx);
        Optional<LivingEntity> found = SpellUtil.onCaster(ctx, target)
                ? SpellUtil.lookedAt(ctx.caster(), r, e -> e instanceof Player p && p.isAlive()).map(LivingEntity.class::cast)
                : target.entity().filter(e -> e instanceof Player).map(LivingEntity.class::cast);
        if (found.isEmpty() || !(found.get() instanceof Player player)) {
            return;
        }
        float max = player.getMaxAbsorption();
        if (max <= 0) {
            return;
        }
        float add = (charge + chargePerPotency * SpellUtil.potency(ctx, power)) * due;
        float shield = Math.min(max, player.getAbsorptionAmount() + add);
        player.setAbsorptionAmount(shield);
        RunicShieldState state = player.getData(TTAttachments.RUNIC_SHIELD);
        state.lastCharge = (int) shield;
        LivingEntity caster = ctx.caster();
        Vec3 from = caster == null ? target.position() : caster.getEyePosition();
        Vec3 to = player.position().add(0, player.getBbHeight() * 0.5, 0).subtract(from);
        for (int s = 0; s < 12; s++) {
            Vec3 p = from.add(to.scale(s / 12.0));
            ctx.level().sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.02);
        }
        SpellUtil.play(ctx.level(), sound, from, 0.5F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
    }
}
