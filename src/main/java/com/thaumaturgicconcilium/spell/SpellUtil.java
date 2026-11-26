package com.thaumaturgicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.CastStyle;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellStats;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellContinuation;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellState;
import com.leclowndu93150.thaumaturge.content.spell.carrier.CarrierPayload;
import com.leclowndu93150.thaumaturge.content.spell.carrier.SpellProjectile;
import com.leclowndu93150.thaumaturge.content.spell.delivery.SpellLook;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import com.thaumaturgicconcilium.TCRegistry;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

/** Helpers shared by the Concilium spell effects. */
public final class SpellUtil {
    /**
     * One TC4 focus upgrade rank of Potency, in Thaumaturge power: the Amplify modifier adds 0.25 power per level, so
     * TC4's {@code potency} is read back as {@code (power - 1) / 0.25}.
     */
    static final float POWER_PER_POTENCY = 0.25F;
    /** One Extend level multiplies the duration stat by 0.5 more (Thaumaturge's Extend modifier). */
    static final float DURATION_PER_EXTEND = 0.5F;
    /**
     * Thaumaturge scales a channelled pulse's power by {@code interval / 20 * 1.2}; the Concilium foci counted ticks
     * of use, so the pulse length is read back from the cast power the wand reported.
     */
    private static final float CHANNEL_POWER_BONUS = 1.2F;

    /** The latest cast each caster started: style and the power the wand handed the engine. */
    private static final Map<UUID, Cast> CASTS = new ConcurrentHashMap<>();

    private SpellUtil() {
    }

    record Cast(CastStyle style, float power) {
    }

    static void recordCast(LivingEntity caster, CastStyle style, float power) {
        CASTS.put(caster.getUUID(), new Cast(style, power));
    }

    static void forget(UUID id) {
        CASTS.remove(id);
    }

    private static Cast castOf(CastContext ctx) {
        LivingEntity caster = ctx.caster();
        Cast cast = caster == null ? null : CASTS.get(caster.getUUID());
        return cast == null ? new Cast(CastStyle.INSTANT, 1.0F) : cast;
    }

    /** Whether this node runs inside a channelled pulse. */
    public static boolean channelled(CastContext ctx) {
        return castOf(ctx).style() == CastStyle.CHANNELED;
    }

    /** The power with a channelled pulse's own scaling taken out, so foci that counted ticks get full strength. */
    public static float strength(CastContext ctx, float power) {
        Cast cast = castOf(ctx);
        if (cast.style() == CastStyle.CHANNELED && cast.power() > 0.0F) {
            return power / cast.power();
        }
        return power;
    }

    /** TC4's Potency rank, read back from the power (fractional: half an Amplify level counts half). */
    public static float potency(CastContext ctx, float power) {
        return Math.max(0.0F, (strength(ctx, power) - 1.0F) / POWER_PER_POTENCY);
    }

    /** TC4's Enlarge rank: Thaumaturge's radius stat (one block per Widen level). */
    public static float enlarge(CastContext ctx) {
        return Math.max(0.0F, ctx.state().get(SpellStats.RADIUS));
    }

    /** TC4's Extend rank, read back from Thaumaturge's duration stat. */
    public static float extend(CastContext ctx) {
        return Math.max(0.0F, (ctx.state().get(SpellStats.DURATION) - 1.0F) / DURATION_PER_EXTEND);
    }

    /** Ticks a channelled pulse stands for (at least 1); 1 for any other cast. */
    public static int pulseTicks(CastContext ctx) {
        Cast cast = castOf(ctx);
        if (cast.style() != CastStyle.CHANNELED) {
            return 1;
        }
        return Math.max(1, Math.min(20, Math.round(cast.power() / CHANNEL_POWER_BONUS * 20.0F)));
    }

    // ---- sounds -------------------------------------------------------------------------------------------------

    /** A sound by id; ids no mod registered still play when a resource pack defines them. */
    public static Holder<SoundEvent> sound(Identifier id) {
        Optional<Holder.Reference<SoundEvent>> found = BuiltInRegistries.SOUND_EVENT.get(id);
        return found.isPresent() ? found.get() : Holder.direct(SoundEvent.createVariableRangeEvent(id));
    }

    public static void play(Level level, Optional<Identifier> id, Vec3 at, float volume, float pitch) {
        id.ifPresent(s -> level.playSound(null, at.x, at.y, at.z, sound(s), SoundSource.PLAYERS, volume, pitch));
    }

    // ---- aiming -------------------------------------------------------------------------------------------------

    /** Whether the target is the caster itself: the effect sits right under the origin (or behind Self). */
    public static boolean onCaster(CastContext ctx, SpellTarget target) {
        return target.entity().filter(e -> e == ctx.caster()).isPresent();
    }

    /** The block the target names, or (cast on the caster) the block the caster looks at within {@code reach}. */
    public static Optional<BlockHitResult> block(CastContext ctx, SpellTarget target, double reach) {
        if (target.block().isPresent()) {
            return target.block();
        }
        if (!onCaster(ctx, target)) {
            return Optional.empty();
        }
        Vec3 from = target.position();
        BlockHitResult hit = ctx.level().clip(new ClipContext(from, from.add(target.direction().normalize().scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, ctx.caster()));
        return hit.getType() == HitResult.Type.BLOCK ? Optional.of(hit) : Optional.empty();
    }

    /** The entity the caster looks at within {@code reach} that passes the filter, not hidden behind blocks. */
    public static Optional<Entity> lookedAt(LivingEntity looker, double reach, Predicate<Entity> filter) {
        Vec3 eye = looker.getEyePosition();
        Vec3 look = looker.getLookAngle().scale(reach);
        Vec3 end = eye.add(look);
        BlockHitResult wall = looker.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, looker));
        if (wall.getType() == HitResult.Type.BLOCK) {
            end = wall.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(looker.level(), looker, eye, end,
                looker.getBoundingBox().expandTowards(look).inflate(1), e -> e != looker && filter.test(e), 0.3F);
        return hit == null ? Optional.empty() : Optional.of(hit.getEntity());
    }

    /** The living creature the target names, or (cast on the caster) the one it looks at within {@code reach}. */
    public static Optional<LivingEntity> living(CastContext ctx, SpellTarget target, double reach) {
        if (onCaster(ctx, target)) {
            return lookedAt(ctx.caster(), reach, e -> e instanceof LivingEntity l && l.isAlive()).map(LivingEntity.class::cast);
        }
        return target.entity().filter(e -> e instanceof LivingEntity l && l.isAlive()).map(LivingEntity.class::cast);
    }

    /** Where the target sits, or (cast on the caster) the point {@code reach} blocks along the look. */
    public static Vec3 point(CastContext ctx, SpellTarget target, double reach) {
        return onCaster(ctx, target) ? target.position().add(target.direction().normalize().scale(reach)) : target.position();
    }

    /** Where a carrier launched by the effect starts: the caster's eyes, or the point reached. */
    public static Vec3 launchPoint(CastContext ctx, SpellTarget target) {
        return target.position();
    }

    // ---- damage, gates ------------------------------------------------------------------------------------------

    /** Magic damage from the caster (indirect when a carrier dealt it). */
    public static DamageSource magic(Level level, @Nullable Entity direct, @Nullable Entity owner) {
        return owner == null ? level.damageSources().magic() : level.damageSources().indirectMagic(direct == null ? owner : direct, owner);
    }

    /**
     * The Ring of Blustering Light gate of Spotlight, Warp Extraction and Alchemist's Spree: a player must wear the
     * ring in a Curios slot. Other casters (golems, mobs) pass.
     */
    public static boolean ringGate(@Nullable LivingEntity caster) {
        if (!(caster instanceof Player player) || player.hasInfiniteMaterials()) {
            return true;
        }
        boolean worn = CuriosApi.getCuriosInventory(player).map(c -> c.isEquipped(TCRegistry.RING_OF_BLUSTERING_LIGHT.get())).orElse(false);
        if (!worn) {
            player.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.no_ring").withStyle(ChatFormatting.DARK_PURPLE));
        }
        return worn;
    }

    /** Whether a held stack (a wand with a socketed focus, or a focus) carries a spell containing the part. */
    public static boolean holdsPart(ItemStack held, ResourceKey<SpellPart> part) {
        if (held.isEmpty()) {
            return false;
        }
        ItemStack focus = held;
        ItemStackTemplate socketed = held.get(TTDataComponents.SOCKETED_FOCUS.get());
        if (socketed != null) {
            focus = socketed.create();
        }
        Spell spell = Spells.spellOf(focus);
        if (spell == null) {
            return false;
        }
        boolean[] found = {false};
        spell.root().forEach((SpellNode node) -> found[0] |= node.part().equals(part));
        return found[0];
    }

    public static boolean holdsPart(LivingEntity holder, ResourceKey<SpellPart> part) {
        return holdsPart(holder.getMainHandItem(), part) || holdsPart(holder.getOffhandItem(), part);
    }

    // ---- carriers and registries --------------------------------------------------------------------------------

    /**
     * Launches a Thaumaturge spell projectile carrying {@code node} (a frost strike for Refreeze's shards, say) from
     * {@code from} along {@code dir}, with power {@code power}. Unknown parts (an addon that is not installed) are
     * skipped.
     */
    public static boolean launchSpell(CastContext ctx, SpellNode node, Vec3 from, Vec3 dir, float speed, double gravity, float power) {
        LivingEntity caster = ctx.caster();
        if (caster == null || Spells.part(ctx.registries(), node.part()).isEmpty()) {
            return false;
        }
        SpellState state = ctx.state().with(SpellStats.POWER, power);
        SpellContinuation continuation = new SpellContinuation(java.util.List.of(node), state, ctx.casterId(), ctx.castId());
        SpellLook look = SpellLook.downstream(ctx.registries(), java.util.List.of(node), SpellLook.DEFAULT);
        SpellProjectile.launch(ctx.level(), caster, new CarrierPayload(continuation, look), new SpellTarget(new EntityHitResult(caster, from), from, dir.normalize()),
                speed, gravity, 0.0F);
        return true;
    }

    /** An entity type by id, empty when no mod registered it. */
    public static Optional<EntityType<?>> entityType(Identifier id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    /** A block by id, empty when no mod registered it. */
    public static Optional<Block> block(Identifier id) {
        return BuiltInRegistries.BLOCK.getOptional(id);
    }

    /** Creates an entity of a type by id at a point, not yet added to the level. */
    public static Optional<Entity> create(ServerLevel level, Identifier id, Vec3 at) {
        return entityType(id).map(type -> type.create(level, EntitySpawnReason.MOB_SUMMONED)).map(e -> {
            e.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
            return e;
        });
    }

    public static ResourceKey<SpellPart> part(String path) {
        return ResourceKey.create(SpellPart.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturgicconcilium", path));
    }
}
