package com.thaumaturgicconcilium.spell.effect;

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
import com.thaumaturgicconcilium.spell.entity.ConcentratedWarpChargeEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Warp Extraction (and Mass Hysteria, Self-Flagellation); needs the Ring of Blustering Light.
 *
 * <ul>
 * <li>Sneak-use: dismisses the caster's charge within {@code dismiss_radius}, or summons a Concentrated Warp Charge
 * (power {@code power + power_per_potency * potency}, range = enlarge; the rest is the {@code charge} settings).</li>
 * <li>Held: the caster's charge looked at within {@code pull_reach} is held {@code hold_ahead} blocks ahead and
 * {@code hold_up} up.</li>
 * <li>Let go: the caster's charges within {@code dismiss_radius} are thrown along the look at {@code throw_speed}.</li>
 * </ul>
 * Behind a delivery it summons a charge at the point reached.
 */
public final class WarpExtractionEffect extends AbstractEffectBehavior {
    public static final MapCodec<WarpExtractionEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ConcentratedWarpChargeEntity.Charge.CODEC.codec().optionalFieldOf("charge", ConcentratedWarpChargeEntity.Charge.DEFAULT).forGetter(e -> e.charge),
            Codec.FLOAT.optionalFieldOf("power", 1.0F).forGetter(e -> e.power),
            Codec.FLOAT.optionalFieldOf("power_per_potency", 1.0F).forGetter(e -> e.powerPerPotency),
            Codec.DOUBLE.optionalFieldOf("dismiss_radius", 10.0).forGetter(e -> e.dismissRadius),
            Codec.DOUBLE.optionalFieldOf("pull_reach", 32.0).forGetter(e -> e.pullReach),
            Codec.DOUBLE.optionalFieldOf("hold_ahead", 1.5).forGetter(e -> e.holdAhead),
            Codec.DOUBLE.optionalFieldOf("hold_up", 2.0).forGetter(e -> e.holdUp),
            Codec.DOUBLE.optionalFieldOf("throw_speed", 2.5).forGetter(e -> e.throwSpeed),
            Identifier.CODEC.optionalFieldOf("summon_sound").forGetter(e -> e.summonSound)).apply(i, WarpExtractionEffect::new));

    private final ConcentratedWarpChargeEntity.Charge charge;
    private final float power;
    private final float powerPerPotency;
    private final double dismissRadius;
    private final double pullReach;
    private final double holdAhead;
    private final double holdUp;
    private final double throwSpeed;
    private final Optional<Identifier> summonSound;

    public WarpExtractionEffect(ConcentratedWarpChargeEntity.Charge charge, float power, float powerPerPotency, double dismissRadius, double pullReach,
            double holdAhead, double holdUp, double throwSpeed, Optional<Identifier> summonSound) {
        this.charge = charge;
        this.power = power;
        this.powerPerPotency = powerPerPotency;
        this.dismissRadius = dismissRadius;
        this.pullReach = pullReach;
        this.holdAhead = holdAhead;
        this.holdUp = holdUp;
        this.throwSpeed = throwSpeed;
        this.summonSound = summonSound;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.WARP_EXTRACTION.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    private List<ConcentratedWarpChargeEntity> mine(ServerLevel level, LivingEntity caster, double radius) {
        return level.getEntitiesOfClass(ConcentratedWarpChargeEntity.class, caster.getBoundingBox().inflate(radius), c -> c.isAlive() && c.isOwnedBy(caster));
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        if (caster == null || !SpellUtil.ringGate(caster)) {
            if (caster instanceof Player p) {
                p.stopUsingItem();
            }
            return;
        }
        ServerLevel level = ctx.level();
        ConcentratedWarpChargeEntity.Charge made = charge.withPower(this.power + powerPerPotency * SpellUtil.potency(ctx, power), SpellUtil.enlarge(ctx));
        if (!SpellUtil.onCaster(ctx, target)) {
            ConcentratedWarpChargeEntity.spawn(level, caster, target.position(), made);
            return;
        }
        Channel.Session session = Channel.of(ctx);
        if (session.fresh()) {
            session.onRelease(s -> {
                if (caster.isAlive() && caster.level() == level) {
                    for (ConcentratedWarpChargeEntity c : mine(level, caster, dismissRadius)) {
                        c.setDeltaMovement(caster.getLookAngle().scale(throwSpeed));
                    }
                }
            });
        }
        if (caster.isShiftKeyDown()) {
            if (session.get("sneaked") == null) {
                session.put("sneaked", Boolean.TRUE);
                List<ConcentratedWarpChargeEntity> own = mine(level, caster, dismissRadius);
                if (!own.isEmpty()) {
                    own.forEach(Entity::discard);
                } else {
                    Vec3 at = caster.getEyePosition().add(caster.getLookAngle().scale(holdAhead));
                    ConcentratedWarpChargeEntity.spawn(level, caster, at, made);
                    SpellUtil.play(level, summonSound, caster.position(), 1.0F, 1.0F);
                }
            }
            return;
        }
        Entity held = session.get("held");
        if (held == null || !held.isAlive()) {
            held = SpellUtil.lookedAt(caster, pullReach, e -> e instanceof ConcentratedWarpChargeEntity c && c.isOwnedBy(caster)).orElse(null);
            session.put("held", held);
        }
        if (held != null) {
            session.everyTick(s -> {
                Entity e = s.get("held");
                if (e != null && e.isAlive()) {
                    hold(caster, e);
                }
            });
            hold(caster, held);
        }
    }

    private void hold(LivingEntity caster, Entity charge) {
        Vec3 look = caster.getLookAngle();
        Vec3 spot = caster.position().add(look.x * holdAhead, look.y * holdAhead + holdUp, look.z * holdAhead);
        Vec3 d = spot.subtract(charge.position());
        charge.setDeltaMovement(d.length() > 1.0 ? d.normalize() : d);
    }
}
