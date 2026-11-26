package com.thaumaturgicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import com.thaumaturgicconcilium.spell.Channel;
import com.thaumaturgicconcilium.spell.ConductorLock;
import com.thaumaturgicconcilium.spell.ConductorTarget;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.TCSpells;
import com.thaumaturgicconcilium.spell.entity.ChargedWispEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Vis Conductor (and Wisp Launcher, Dematerialization).
 *
 * <p>Conductor: the first pulse aimed at a Destabilized Crystal within {@code reach} locks onto it (see
 * {@link ConductorLock}); while held within {@code reach} it drains {@code drain + drain_per_potency * potency} charge
 * every {@code step_ticks} ticks. Letting go with nothing living in the aim (within {@code aim_reach}) blows the crystal
 * up (strength {@code explosion}; block damage follows mobGriefing). Dematerialization aimed at a creature instead
 * deals {@code drained / (demat_divisor - potency)} magic damage ({@link ConductorTarget}s handle it themselves).
 *
 * <p>Wisp Launcher: holds the wisp looked at within {@code wisp_reach} in front of the caster; letting go with it
 * within {@code wisp_launch_dist_sq} turns it into a Charged Wisp thrown along the look (life {@code wisp_life +
 * rand(wisp_life_random) + wisp_life_per_extend * extend}, power = potency, zap settings in {@code zap}).
 */
public final class VisConductorEffect extends AbstractEffectBehavior {
    public static final MapCodec<VisConductorEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("wisp_launcher", false).forGetter(e -> e.wispLauncher),
            Codec.BOOL.optionalFieldOf("dematerialization", false).forGetter(e -> e.dematerialize),
            Codec.DOUBLE.optionalFieldOf("reach", 8.0).forGetter(e -> e.reach),
            Codec.INT.optionalFieldOf("drain", 1).forGetter(e -> e.drain),
            Codec.FLOAT.optionalFieldOf("drain_per_potency", 1.0F).forGetter(e -> e.drainPerPotency),
            Codec.INT.optionalFieldOf("step_ticks", 3).forGetter(e -> e.stepTicks),
            Codec.DOUBLE.optionalFieldOf("aim_reach", 32.0).forGetter(e -> e.aimReach),
            Codec.FLOAT.optionalFieldOf("explosion", 2.0F).forGetter(e -> e.explosion),
            Codec.FLOAT.optionalFieldOf("demat_divisor", 6.0F).forGetter(e -> e.dematDivisor),
            Wisp.CODEC.forGetter(e -> e.wisp),
            Identifier.CODEC.optionalFieldOf("sound").forGetter(e -> e.sound)).apply(i, VisConductorEffect::new));

    /** Wisp Launcher's settings. */
    public record Wisp(double reach, double holdAhead, double holdUp, double launchDistSq, double speed, int life, int lifeRandom, int lifePerExtend,
            ChargedWispEntity.Zap zap) {
        static final MapCodec<Wisp> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("wisp_reach", 32.0).forGetter(Wisp::reach),
                Codec.DOUBLE.optionalFieldOf("wisp_hold_ahead", 1.5).forGetter(Wisp::holdAhead),
                Codec.DOUBLE.optionalFieldOf("wisp_hold_up", 1.0).forGetter(Wisp::holdUp),
                Codec.DOUBLE.optionalFieldOf("wisp_launch_dist_sq", 6.0).forGetter(Wisp::launchDistSq),
                Codec.DOUBLE.optionalFieldOf("wisp_speed", 1.0).forGetter(Wisp::speed),
                Codec.INT.optionalFieldOf("wisp_life", 20).forGetter(Wisp::life),
                Codec.INT.optionalFieldOf("wisp_life_random", 20).forGetter(Wisp::lifeRandom),
                Codec.INT.optionalFieldOf("wisp_life_per_extend", 20).forGetter(Wisp::lifePerExtend),
                ChargedWispEntity.Zap.CODEC.codec().optionalFieldOf("zap", ChargedWispEntity.Zap.DEFAULT).forGetter(Wisp::zap)).apply(i, Wisp::new));
    }

    private final boolean wispLauncher;
    private final boolean dematerialize;
    private final double reach;
    private final int drain;
    private final float drainPerPotency;
    private final int stepTicks;
    private final double aimReach;
    private final float explosion;
    private final float dematDivisor;
    private final Wisp wisp;
    private final Optional<Identifier> sound;

    public VisConductorEffect(boolean wispLauncher, boolean dematerialize, double reach, int drain, float drainPerPotency, int stepTicks, double aimReach,
            float explosion, float dematDivisor, Wisp wisp, Optional<Identifier> sound) {
        this.wispLauncher = wispLauncher;
        this.dematerialize = dematerialize;
        this.reach = reach;
        this.drain = drain;
        this.drainPerPotency = drainPerPotency;
        this.stepTicks = stepTicks;
        this.aimReach = aimReach;
        this.explosion = explosion;
        this.dematDivisor = dematDivisor;
        this.wisp = wisp;
        this.sound = sound;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.VIS_CONDUCTOR.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        float potency = SpellUtil.potency(ctx, power);
        if (wispLauncher) {
            launcher(ctx, target, potency);
        } else {
            conductor(ctx, target, potency);
        }
    }

    // ---- conductor ----------------------------------------------------------------------------------------------

    private static Optional<DestabilizedCrystalBlockEntity> crystal(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof DestabilizedCrystalBlockEntity crystal ? Optional.of(crystal) : Optional.empty();
    }

    private void conductor(CastContext ctx, SpellTarget target, float potency) {
        ServerLevel level = ctx.level();
        LivingEntity caster = ctx.caster();
        if (!SpellUtil.onCaster(ctx, target) || !(caster instanceof Player player)) {
            // Behind a delivery: a crystal that is struck goes off at once.
            target.block().ifPresent(hit -> crystal(level, hit.getBlockPos()).ifPresent(c -> blow(level, hit.getBlockPos())));
            return;
        }
        Channel.Session session = Channel.of(ctx);
        Optional<ConductorLock.State> lock = ConductorLock.get(player);
        if (session.fresh()) {
            session.onRelease(s -> release(player, potency));
        }
        if (lock.isEmpty()) {
            Optional<BlockHitResult> hit = SpellUtil.block(ctx, target, reach);
            if (hit.isEmpty()) {
                return;
            }
            BlockPos pos = hit.get().getBlockPos();
            Optional<DestabilizedCrystalBlockEntity> found = crystal(level, pos);
            if (found.isEmpty() || found.get().aspect().isEmpty() || found.get().capacity() <= 0) {
                return;
            }
            ConductorLock.set(player, new ConductorLock.State(pos.immutable(), found.get().aspect().get(), 0));
            return;
        }
        ConductorLock.State state = lock.get();
        Optional<DestabilizedCrystalBlockEntity> found = crystal(level, state.crystal());
        if (found.isEmpty() || player.distanceToSqr(Vec3.atCenterOf(state.crystal())) > reach * reach
                || !found.get().aspect().map(a -> a.equals(state.aspect())).orElse(false)) {
            player.stopUsingItem();
            return;
        }
        int steps = session.due("drain", SpellUtil.pulseTicks(ctx), stepTicks);
        if (steps <= 0) {
            return;
        }
        int taken = found.get().drain(Math.round((drain + drainPerPotency * potency) * steps));
        ConductorLock.set(player, state.grown(taken));
        arc(level, player.getEyePosition().subtract(0, 0.3, 0), Vec3.atCenterOf(state.crystal()), color(level, state.aspect()));
        SpellUtil.play(level, sound, player.position(), 0.5F, 1.0F + (ctx.random().nextFloat() - ctx.random().nextFloat()) * 0.2F);
    }

    private void release(Player player, float potency) {
        Optional<ConductorLock.State> lock = ConductorLock.get(player);
        ConductorLock.clear(player);
        if (lock.isEmpty() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ConductorLock.State state = lock.get();
        if (crystal(level, state.crystal()).isEmpty()) {
            return;
        }
        Optional<Entity> aimed = SpellUtil.lookedAt(player, aimReach, Entity::isAlive);
        if (aimed.isEmpty()) {
            blow(level, state.crystal());
            return;
        }
        if (!dematerialize) {
            return;
        }
        Entity victim = aimed.get();
        if (victim instanceof ConductorTarget special && special.onDematerialize(player, state, potency)) {
            arc(level, player.position().add(0, 1, 0), victim.position().add(0, 0.5, 0), color(level, state.aspect()));
            return;
        }
        if (victim instanceof LivingEntity living) {
            float divisor = Math.max(1.0F, dematDivisor - potency);
            living.hurtServer(level, SpellUtil.magic(level, player, player), state.amount() / divisor);
            arc(level, player.position().add(0, 1, 0), living.position().add(0, living.getBbHeight() * 0.5, 0), color(level, state.aspect()));
            SpellUtil.play(level, Optional.of(Identifier.fromNamespaceAndPath("thaumaturge", "shock")), player.position(), 0.8F, 0.9F + level.getRandom().nextFloat() * 0.1F);
        }
    }

    private void blow(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, explosion, Level.ExplosionInteraction.MOB);
    }

    private static int color(ServerLevel level, ResourceKey<IAspect> aspect) {
        return level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(aspect).map(h -> h.value().color()).orElse(0xAAAAAA);
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to, int color) {
        Vec3 d = to.subtract(from);
        int steps = Math.min(40, (int) (d.length() * 3) + 1);
        DustParticleOptions dust = new DustParticleOptions(color, 0.8F);
        for (int s = 0; s <= steps; s++) {
            Vec3 p = from.add(d.scale(s / (double) steps)).add(level.getRandom().nextGaussian() * 0.08, level.getRandom().nextGaussian() * 0.08, level.getRandom().nextGaussian() * 0.08);
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 3, 0.2, 0.2, 0.2, 0.02);
    }

    // ---- wisp launcher ------------------------------------------------------------------------------------------

    private void launcher(CastContext ctx, SpellTarget target, float potency) {
        ServerLevel level = ctx.level();
        LivingEntity caster = ctx.caster();
        if (!SpellUtil.onCaster(ctx, target)) {
            // Behind a delivery: a wisp that is struck is overcharged and flies on along the spell.
            target.entity().filter(e -> e instanceof WispEntity).ifPresent(e -> charge(ctx, (WispEntity) e, target.direction(), potency));
            return;
        }
        if (caster == null) {
            return;
        }
        Channel.Session session = Channel.of(ctx);
        Entity held = session.get("wisp");
        if (held == null || !held.isAlive()) {
            held = SpellUtil.lookedAt(caster, wisp.reach(), e -> e instanceof WispEntity && e.isAlive()).orElse(null);
            session.put("wisp", held);
        }
        if (held == null) {
            return;
        }
        session.everyTick(s -> {
            Entity e = s.get("wisp");
            if (e != null && e.isAlive()) {
                hold(caster, e);
            }
        });
        session.onRelease(s -> {
            Entity e = s.get("wisp");
            if (e instanceof WispEntity w && w.isAlive() && w.distanceToSqr(caster) < wisp.launchDistSq()) {
                charge(ctx, w, caster.getLookAngle(), potency);
            }
        });
        hold(caster, held);
        arc(level, caster.getEyePosition().subtract(0, 0.3, 0), held.position().add(0, 0.4, 0), 0xAAAAAA);
        if (session.due("hum", SpellUtil.pulseTicks(ctx), 10) > 0) {
            SpellUtil.play(level, sound, caster.position(), 0.5F, 1.0F);
        }
    }

    private void hold(LivingEntity caster, Entity e) {
        Vec3 look = caster.getLookAngle();
        Vec3 spot = caster.position().add(look.x * wisp.holdAhead(), look.y * wisp.holdAhead() + wisp.holdUp(), look.z * wisp.holdAhead());
        Vec3 d = spot.subtract(e.position());
        e.setDeltaMovement(d.length() > 1.0 ? d.normalize() : d);
        e.hurtMarked = true;
    }

    private void charge(CastContext ctx, WispEntity from, Vec3 dir, float potency) {
        Holder<IAspect> aspect = from.aspect();
        int life = wisp.life() + (wisp.lifeRandom() > 0 ? ctx.random().nextInt(wisp.lifeRandom()) : 0) + Math.round(wisp.lifePerExtend() * SpellUtil.extend(ctx));
        ChargedWispEntity.spawn(ctx.level(), from.position().add(0, from.getBbHeight() * 0.5, 0), dir.normalize().scale(wisp.speed()),
                aspect == null ? null : aspect.unwrapKey().orElse(null), ctx.caster(), potency, life, wisp.zap());
        from.discard();
    }
}
