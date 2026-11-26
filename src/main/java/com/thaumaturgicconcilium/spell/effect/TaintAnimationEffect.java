package com.thaumaturgicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.Channel;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Taint Animation (and Thought Manifestation, Flux Vaporization), every {@code interval} ticks while held.
 *
 * <p>Sneaking: every tainted creature within {@code command_radius} turns on the living, untainted creature the caster
 * looks at, tainted crawlers become the caster's Enslaved Taint Spiders, and with Potency they get Strength for
 * {@code strength_ticks} at amplifier {@code clamp(permanent warp * strength_per_warp * potency, 1, strength_max)}.
 *
 * <p>Otherwise: one random taint fibre, flux gas or (Flux Vaporization) flux goo block within {@code block_radius +
 * block_radius_per_enlarge * enlarge} wakes up: a fibre becomes a taintacle aimed at the creature looked at, gas
 * becomes a taint swarm, goo boils into gas. Thought Manifestation also turns a mind spider within
 * {@code spider_radius} into an Enslaved Taint Spider.
 */
public final class TaintAnimationEffect extends AbstractEffectBehavior {
    public static final MapCodec<TaintAnimationEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("interval", 10).forGetter(e -> e.interval),
            Codec.DOUBLE.optionalFieldOf("aim_reach", 32.0).forGetter(e -> e.aimReach),
            Codec.DOUBLE.optionalFieldOf("command_radius", 16.0).forGetter(e -> e.commandRadius),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().optionalFieldOf("strength_effect").forGetter(e -> e.strengthEffect),
            Codec.INT.optionalFieldOf("strength_ticks", 100).forGetter(e -> e.strengthTicks),
            Codec.FLOAT.optionalFieldOf("strength_per_warp", 0.1F).forGetter(e -> e.strengthPerWarp),
            Codec.INT.optionalFieldOf("strength_max", 200).forGetter(e -> e.strengthMax),
            Codec.DOUBLE.optionalFieldOf("block_radius", 4.0).forGetter(e -> e.blockRadius),
            Codec.DOUBLE.optionalFieldOf("block_radius_per_enlarge", 2.0).forGetter(e -> e.blockRadiusPerEnlarge),
            Codec.BOOL.optionalFieldOf("flux_vaporization", false).forGetter(e -> e.vaporize),
            Codec.BOOL.optionalFieldOf("thought_manifestation", false).forGetter(e -> e.manifest),
            Codec.DOUBLE.optionalFieldOf("spider_radius", 16.0).forGetter(e -> e.spiderRadius),
            Ids.CODEC.forGetter(e -> e.ids)).apply(i, TaintAnimationEffect::new));

    /** The blocks and creatures involved, by id (looked up when used). */
    public record Ids(ResourceLocation fibre, ResourceLocation gas, ResourceLocation goo, ResourceLocation taintacle, ResourceLocation swarm, List<ResourceLocation> spiders, ResourceLocation mindSpider,
            ResourceLocation enslaved) {
        static final MapCodec<Ids> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("taint_fibre", tt("taint_fibre")).forGetter(Ids::fibre),
                ResourceLocation.CODEC.optionalFieldOf("flux_gas", tt("flux_gas")).forGetter(Ids::gas),
                ResourceLocation.CODEC.optionalFieldOf("flux_goo", tt("flux_goo")).forGetter(Ids::goo),
                ResourceLocation.CODEC.optionalFieldOf("taintacle", tt("taintacle")).forGetter(Ids::taintacle),
                ResourceLocation.CODEC.optionalFieldOf("taint_swarm", tt("taint_swarm")).forGetter(Ids::swarm),
                ResourceLocation.CODEC.listOf().optionalFieldOf("taint_spiders", List.of(tt("taint_crawler"))).forGetter(Ids::spiders),
                ResourceLocation.CODEC.optionalFieldOf("mind_spider", tt("mind_spider")).forGetter(Ids::mindSpider),
                ResourceLocation.CODEC.optionalFieldOf("enslaved_spider", ResourceLocation.fromNamespaceAndPath("thaumaturgicconcilium", "taint_spider_smart"))
                        .forGetter(Ids::enslaved)).apply(i, Ids::new));

        private static ResourceLocation tt(String path) {
            return ResourceLocation.fromNamespaceAndPath("thaumaturge", path);
        }
    }

    private final int interval;
    private final double aimReach;
    private final double commandRadius;
    private final Optional<Holder<MobEffect>> strengthEffect;
    private final int strengthTicks;
    private final float strengthPerWarp;
    private final int strengthMax;
    private final double blockRadius;
    private final double blockRadiusPerEnlarge;
    private final boolean vaporize;
    private final boolean manifest;
    private final double spiderRadius;
    private final Ids ids;

    public TaintAnimationEffect(int interval, double aimReach, double commandRadius, Optional<Holder<MobEffect>> strengthEffect, int strengthTicks,
            float strengthPerWarp, int strengthMax, double blockRadius, double blockRadiusPerEnlarge, boolean vaporize, boolean manifest, double spiderRadius,
            Ids ids) {
        this.interval = interval;
        this.aimReach = aimReach;
        this.commandRadius = commandRadius;
        this.strengthEffect = strengthEffect;
        this.strengthTicks = strengthTicks;
        this.strengthPerWarp = strengthPerWarp;
        this.strengthMax = strengthMax;
        this.blockRadius = blockRadius;
        this.blockRadiusPerEnlarge = blockRadiusPerEnlarge;
        this.vaporize = vaporize;
        this.manifest = manifest;
        this.spiderRadius = spiderRadius;
        this.ids = ids;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.TAINT_ANIMATION.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        int due = Channel.of(ctx).due("animate", SpellUtil.pulseTicks(ctx), interval);
        if (due <= 0) {
            return;
        }
        LivingEntity caster = ctx.caster();
        LivingEntity prey = SpellUtil.living(ctx, target, aimReach).filter(e -> !tainted(e) && e != caster).orElse(null);
        Vec3 centre = SpellUtil.onCaster(ctx, target) && caster != null ? caster.position() : target.position();
        if (caster != null && caster.isShiftKeyDown() && SpellUtil.onCaster(ctx, target)) {
            command(ctx, caster, centre, prey, SpellUtil.potency(ctx, power));
        } else {
            animate(ctx, centre, prey);
        }
    }

    private static boolean tainted(Entity e) {
        return e.getType().is(ThaumaturgeEntityTypeTags.TAINTED);
    }

    private void command(CastContext ctx, LivingEntity caster, Vec3 centre, @Nullable LivingEntity prey, float potency) {
        ServerLevel level = ctx.level();
        int warp = caster instanceof Player p ? WarpHelper.getWarp(p).get(WarpType.PERMANENT) : 0;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(centre, centre).inflate(commandRadius), m -> m.isAlive() && tainted(m));
        for (Mob mob : mobs) {
            Mob commanded = ids.spiders().contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())) ? enslave(level, mob) : mob;
            if (prey != null) {
                commanded.setTarget(prey);
            }
            if (potency > 0 && strengthEffect.isPresent()) {
                int amp = Mth.clamp(Math.round(warp * strengthPerWarp * potency), 1, strengthMax);
                commanded.addEffect(new MobEffectInstance(strengthEffect.get(), strengthTicks, amp));
            }
        }
    }

    /** Swaps a creature for an Enslaved Taint Spider in place (when that creature is registered). */
    private Mob enslave(ServerLevel level, Mob from) {
        Optional<Entity> made = SpellUtil.create(level, ids.enslaved(), from.position());
        if (made.isEmpty() || !(made.get() instanceof Mob spider)) {
            return from;
        }
        spider.moveTo(from.getX(), from.getY(), from.getZ(), from.getYRot(), from.getXRot());
        spider.setHealth(Math.min(spider.getMaxHealth(), from.getHealth()));
        level.addFreshEntity(spider);
        level.sendParticles(ParticleTypes.WITCH, from.getX(), from.getY() + 0.5, from.getZ(), 12, 0.3, 0.3, 0.3, 0);
        from.discard();
        return spider;
    }

    private void animate(CastContext ctx, Vec3 centre, @Nullable LivingEntity prey) {
        ServerLevel level = ctx.level();
        Block fibre = SpellUtil.block(ids.fibre()).orElse(null);
        Block gas = SpellUtil.block(ids.gas()).orElse(null);
        Block goo = SpellUtil.block(ids.goo()).orElse(null);
        int r = (int) Math.ceil(blockRadius + blockRadiusPerEnlarge * SpellUtil.enlarge(ctx));
        BlockPos origin = BlockPos.containing(centre);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            Block block = level.getBlockState(pos).getBlock();
            if ((fibre != null && block == fibre) || (gas != null && block == gas) || (vaporize && goo != null && block == goo)) {
                found.add(pos.immutable());
            }
        }
        if (!found.isEmpty()) {
            BlockPos pos = found.get(ctx.random().nextInt(found.size()));
            Block block = level.getBlockState(pos).getBlock();
            Vec3 at = Vec3.atBottomCenterOf(pos);
            if (block == fibre) {
                level.removeBlock(pos, false);
                SpellUtil.create(level, ids.taintacle(), at).ifPresent(e -> {
                    if (e instanceof Mob mob && prey != null) {
                        mob.setTarget(prey);
                    }
                    level.addFreshEntity(e);
                });
            } else if (block == gas) {
                level.removeBlock(pos, false);
                SpellUtil.create(level, ids.swarm(), at).ifPresent(e -> {
                    if (e instanceof Mob mob && prey != null) {
                        mob.setTarget(prey);
                    }
                    level.addFreshEntity(e);
                });
            } else if (gas != null) {
                level.setBlockAndUpdate(pos, gas.defaultBlockState());
            }
            level.sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.5, at.z, 10, 0.3, 0.3, 0.3, 0);
        }
        if (manifest) {
            List<Mob> spiders = level.getEntitiesOfClass(Mob.class, new AABB(centre, centre).inflate(spiderRadius),
                    m -> m.isAlive() && BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).equals(ids.mindSpider()));
            if (!spiders.isEmpty()) {
                Mob made = enslave(level, spiders.get(ctx.random().nextInt(spiders.size())));
                if (prey != null) {
                    made.setTarget(prey);
                }
            }
        }
    }
}
