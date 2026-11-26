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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Ethereal Manipulator: while held, the dropped item, projectile or fireball looked at within the part's {@code range}
 * setting (TC4 changed it with sneak + mouse wheel, 1 to 16) is carried to the point {@code range - 1} blocks along the
 * look at {@code speed} blocks a tick. Behind a delivery it draws loose items and projectiles near the point reached
 * toward the caster's aim.
 */
public final class EtherealManipulatorEffect extends AbstractEffectBehavior {
    public static final MapCodec<EtherealManipulatorEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("speed", 0.3333).forGetter(e -> e.speed),
            Codec.INT.optionalFieldOf("default_range", 5).forGetter(e -> e.defaultRange),
            Codec.DOUBLE.optionalFieldOf("grab_radius", 1.5).forGetter(e -> e.grabRadius)).apply(i, EtherealManipulatorEffect::new));

    private final double speed;
    private final int defaultRange;
    private final double grabRadius;

    public EtherealManipulatorEffect(double speed, int defaultRange, double grabRadius) {
        this.speed = speed;
        this.defaultRange = defaultRange;
        this.grabRadius = grabRadius;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.ETHEREAL_MANIPULATOR.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    private static boolean movable(Entity e) {
        return e.isAlive() && (e instanceof ItemEntity || e instanceof Projectile);
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        if (caster == null) {
            return;
        }
        int setting = ctx.setting("range");
        int range = setting > 0 ? setting : defaultRange;
        if (!SpellUtil.onCaster(ctx, target)) {
            Vec3 to = caster.getEyePosition().add(caster.getLookAngle().scale(Math.max(1, range - 1)));
            for (Entity e : ctx.level().getEntities(caster, new net.minecraft.world.phys.AABB(target.position(), target.position()).inflate(grabRadius),
                    EtherealManipulatorEffect::movable)) {
                pull(e, to);
            }
            return;
        }
        Channel.Session session = Channel.of(ctx);
        session.put("range", range);
        Entity held = session.get("held");
        if (held == null || !held.isAlive() || held.distanceTo(caster) > range + 2) {
            held = SpellUtil.lookedAt(caster, range, EtherealManipulatorEffect::movable).orElse(null);
            session.put("held", held);
        }
        if (held != null) {
            session.everyTick(s -> {
                Entity e = s.get("held");
                Integer r = s.get("range");
                if (e != null && e.isAlive() && caster.isAlive()) {
                    pull(e, caster.getEyePosition().add(caster.getLookAngle().scale(Math.max(1, (r == null ? defaultRange : r) - 1))));
                }
            });
            pull(held, caster.getEyePosition().add(caster.getLookAngle().scale(Math.max(1, range - 1))));
        }
    }

    private void pull(Entity e, Vec3 to) {
        Vec3 d = to.subtract(e.position());
        double len = d.length();
        e.setDeltaMovement(len < speed ? d : d.scale(speed / len));
        e.hurtMarked = true;
        e.resetFallDistance();
        if (e.level() instanceof ServerLevel server && e.tickCount % 4 == 0) {
            server.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 0.2, e.getZ(), 1, 0.1, 0.1, 0.1, 0);
        }
    }
}
