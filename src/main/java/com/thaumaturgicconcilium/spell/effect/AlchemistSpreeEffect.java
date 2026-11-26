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
import com.thaumaturgicconcilium.spell.entity.DistortionEffectEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Alchemist's Spree (and Primal Essence); needs the Ring of Blustering Light. While held, a distortion hangs before the
 * caster and every projectile within {@code half_width} x {@code half_height} of it (of the point reached, behind a
 * delivery) that a {@code conversions} entry names is transmuted, keeping its flight: into a spell projectile
 * ({@code spell}, or {@code primal_spell} with Primal Essence; snowballs become frost shards), another entity
 * ({@code spawn}; eggs hatch taint crawlers) or an explosion of {@code explode * (potency + 1)} (fiery with Primal
 * Essence; small fireballs, TC4's embers).
 */
public final class AlchemistSpreeEffect extends AbstractEffectBehavior {
    /** One transmutation. */
    public record Conversion(Identifier from, Optional<SpellNode> spell, Optional<SpellNode> primalSpell, Optional<Identifier> spawn, float explode) {
        static final Codec<Conversion> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("from").forGetter(Conversion::from),
                SpellNode.CODEC.optionalFieldOf("spell").forGetter(Conversion::spell),
                SpellNode.CODEC.optionalFieldOf("primal_spell").forGetter(Conversion::primalSpell),
                Identifier.CODEC.optionalFieldOf("spawn").forGetter(Conversion::spawn),
                Codec.FLOAT.optionalFieldOf("explode", 0.0F).forGetter(Conversion::explode)).apply(i, Conversion::new));
    }

    public static final MapCodec<AlchemistSpreeEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("half_width", 3.0).forGetter(e -> e.halfWidth),
            Codec.DOUBLE.optionalFieldOf("half_height", 1.5).forGetter(e -> e.halfHeight),
            Codec.BOOL.optionalFieldOf("primal_essence", false).forGetter(e -> e.primal),
            Conversion.CODEC.listOf().optionalFieldOf("conversions", List.of()).forGetter(e -> e.conversions),
            Codec.INT.optionalFieldOf("sound_interval", 20).forGetter(e -> e.soundInterval),
            Identifier.CODEC.optionalFieldOf("sound").forGetter(e -> e.sound)).apply(i, AlchemistSpreeEffect::new));

    private final double halfWidth;
    private final double halfHeight;
    private final boolean primal;
    private final List<Conversion> conversions;
    private final int soundInterval;
    private final Optional<Identifier> sound;

    public AlchemistSpreeEffect(double halfWidth, double halfHeight, boolean primal, List<Conversion> conversions, int soundInterval,
            Optional<Identifier> sound) {
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.primal = primal;
        this.conversions = conversions;
        this.soundInterval = soundInterval;
        this.sound = sound;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.ALCHEMIST_SPREE.get();
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
        Vec3 centre = target.position();
        if (SpellUtil.onCaster(ctx, target) && caster != null) {
            Channel.Session session = Channel.of(ctx);
            DistortionEffectEntity cloud = session.get("cloud");
            if (cloud == null || cloud.isRemoved()) {
                cloud = DistortionEffectEntity.spawn(level, caster);
                session.put("cloud", cloud);
            } else {
                cloud.refresh(caster);
            }
            DistortionEffectEntity shown = cloud;
            session.everyTick(s -> {
                if (!shown.isRemoved()) {
                    shown.refresh(caster);
                }
            });
            centre = cloud.position();
            if (session.due("bubbles", SpellUtil.pulseTicks(ctx), soundInterval) > 0) {
                SpellUtil.play(level, sound, centre, 0.6F, 1.0F);
            }
        }
        AABB box = new AABB(centre, centre).inflate(halfWidth, halfHeight, halfWidth);
        for (Entity e : level.getEntities((Entity) null, box, e -> e.isAlive() && e instanceof Projectile)) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            for (Conversion c : conversions) {
                if (c.from().equals(id)) {
                    convert(ctx, level, (Projectile) e, c, potency, power);
                    break;
                }
            }
        }
    }

    private void convert(CastContext ctx, ServerLevel level, Projectile from, Conversion c, float potency, float power) {
        Vec3 pos = from.position();
        Vec3 motion = from.getDeltaMovement();
        Optional<SpellNode> spell = primal && c.primalSpell().isPresent() ? c.primalSpell() : c.spell();
        boolean done = false;
        if (spell.isPresent()) {
            Vec3 dir = motion.lengthSqr() < 1.0E-4 ? new Vec3(0, -1, 0) : motion;
            done = SpellUtil.launchSpell(ctx, spell.get(), pos, dir, (float) Math.max(0.5, motion.length()), 0.0, SpellUtil.strength(ctx, power) * (1.0F + potency));
        } else if (c.spawn().isPresent()) {
            Optional<Entity> made = SpellUtil.create(level, c.spawn().get(), pos);
            if (made.isPresent()) {
                Entity e = made.get();
                e.setDeltaMovement(motion);
                if (e instanceof Projectile p) {
                    p.setOwner(from.getOwner());
                }
                level.addFreshEntity(e);
                done = true;
            }
        } else if (c.explode() > 0) {
            level.explode(ctx.caster(), pos.x, pos.y, pos.z, c.explode() * (potency + 1.0F), primal, Level.ExplosionInteraction.MOB);
            done = true;
        }
        if (done) {
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 6, 0.2, 0.2, 0.2, 0);
            from.discard();
        }
    }
}
