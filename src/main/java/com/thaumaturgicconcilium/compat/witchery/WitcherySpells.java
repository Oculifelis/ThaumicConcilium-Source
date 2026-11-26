package com.thaumaturgicconcilium.compat.witchery;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.research.ResearchEvent;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.crimson.EtherealShacklesEntity;
import com.thaumaturgicconcilium.entity.ShardPowderEntity;
import com.thaumaturgicconcilium.mob.MobUtil;
import com.thaumaturgicconcilium.rift.RiftUtil;
import dev.sterner.witchery.core.api.SymbologySpell;
import dev.sterner.witchery.core.registry.WitcheryInfusions;
import dev.sterner.witchery.core.registry.WitcherySymbologies;
import dev.sterner.witchery.feature.infusion.InfusionPlayerAttachment;
import dev.sterner.witchery.feature.infusion.InfusionType;
import dev.sterner.witchery.feature.symbology.SymbologyPlayerAttachment;
import dev.sterner.witchery.feature.symbology.SymbologyTargetUtil;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Crimson Cult's five symbology spells (TC4 {@code WitcheryIntegration}), cast with a Witchery wand once learned.
 * They are learned together when the player completes the Crimson Spells research (and on login, for a player who had
 * it before Witchery was added). The Witchery infusion the caster holds changes some of them, scaled by the caster's
 * Thaumaturge aspect pool:
 * <ul>
 * <li>Incarcerous: Infernal infusion, throws Ethereal Shackles; otherwise 2-4 sloth motes (TC4 shard powder type 12:
 * slowness and vis exhaustion on the first thing they touch).</li>
 * <li>Intrare Polosae: a critical arrow, +5 damage; Overworld infusion, damage 1 + 1.5 x sqrt(Aversio pool).</li>
 * <li>Ariania Exumai / Trup Ehsolvi: magic damage to the arthropod / undead monster looked at within 32 blocks, a white bolt to
 * it; 0-29 + 2 per spell level, or with the Light infusion 1 + 2.5 x sqrt(Lux pool).</li>
 * <li>Oculus Madesco: Blurred Vision on the mob looked at for 400 ticks; Infernal infusion, plus the Tenebrae pool to the
 * duration and blindness as well.</li>
 * </ul>
 * Witchery 0.5 spell costs are infusion charge out of 6000 (its own spells cost 60 to 400); the TC4 cost of 2 charges is
 * taken as 200.
 */
public final class WitcherySpells {
    public static final ResourceLocation RESEARCH = ThaumaturgicConcilium.id("crimson_spells");
    private static final int COST = 200;
    private static final double RANGE = 32.0;
    private static final ResourceKey<IAspect> DESIDIA = ResourceKey.create(IAspect.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("forsakenmagic", "desidia"));

    public static final DeferredRegister<SymbologySpell> SPELLS = DeferredRegister.create(WitcherySymbologies.INSTANCE.getSYMBOLOGY_REGISTRY_KEY(),
            ThaumaturgicConcilium.MODID);

    static {
        SPELLS.register("incarcerous", () -> new CrimsonSpell("incarcerous", new byte[]{2, 2, 2, 0, 3, 3, 1}, COST, 1, WitcherySpells::incarcerous));
        SPELLS.register("intrare_polosae", () -> new CrimsonSpell("intrare_polosae", new byte[]{1, 1, 1, 3}, COST, 1, WitcherySpells::arrow));
        SPELLS.register("ariania_exumai", () -> new CrimsonSpell("ariania_exumai", new byte[]{0, 0, 0, 2, 1, 1, 0, 0}, COST, 1,
                (level, player, look, lvl) -> smite(level, player, look, lvl, EntityTypeTags.ARTHROPOD)));
        SPELLS.register("trup_ehsolvi", () -> new CrimsonSpell("trup_ehsolvi", new byte[]{1, 1, 1, 0, 3, 2, 2}, COST, 2,
                (level, player, look, lvl) -> smite(level, player, look, lvl, EntityTypeTags.UNDEAD)));
        SPELLS.register("oculus_madesco", () -> new CrimsonSpell("oculus_madesco", new byte[]{0, 0, 3, 3, 1, 1, 2, 2}, COST, 1,
                WitcherySpells::blind));
    }

    private WitcherySpells() {
    }

    static void register(IEventBus modBus) {
        SPELLS.register(modBus);
        NeoForge.EVENT_BUS.addListener(WitcherySpells::researched);
        NeoForge.EVENT_BUS.addListener(WitcherySpells::login);
    }

    // ------------------------------------------------------------------ learning

    private static void researched(ResearchEvent.Completed event) {
        if (event.research().equals(RESEARCH) && event.player() instanceof ServerPlayer player) {
            learnAll(player);
        }
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KnowledgeAccess.of(player).isResearchComplete(RESEARCH)) {
            learnAll(player);
        }
    }

    private static void learnAll(ServerPlayer player) {
        for (var holder : SPELLS.getEntries()) {
            ResourceLocation id = holder.getId();
            if (!SymbologyPlayerAttachment.INSTANCE.hasLearnedSpell(player, id)) {
                SymbologyPlayerAttachment.INSTANCE.learnSpell(player, id);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean infusion(ServerPlayer player, InfusionType type) {
        return InfusionPlayerAttachment.getData(player).getType() == type;
    }

    private static int pool(ServerPlayer player, ResourceKey<IAspect> aspect) {
        Holder<IAspect> holder = player.level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(aspect).orElse(null);
        return holder == null ? 0 : AspectPoolAccess.amount(player, holder);
    }

    private static void craftFail(ServerLevel level, ServerPlayer player) {
        MobUtil.play(level, player, MobUtil.sound("thaumaturge:craftfail"), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ spells

    private static void incarcerous(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
        if (infusion(player, WitcheryInfusions.INSTANCE.getINFERNAL().get())) {
            EtherealShacklesEntity shackles = new EtherealShacklesEntity(level, player);
            Vec3 dir = player.getLookAngle();
            shackles.setPos(player.getX() + dir.x * 3.0, player.getEyeY() + dir.y * 3.0, player.getZ() + dir.z * 3.0);
            shackles.shoot(dir.x, dir.y, dir.z, 2.0F, 2.0F);
            level.addFreshEntity(shackles);
            MobUtil.play(level, shackles, MobUtil.sound(ThaumaturgicConcilium.id("shackles_throw").toString()), SoundSource.PLAYERS, 0.5F, 1.0F);
            return;
        }
        level.levelEvent(null, 1008, player.blockPosition(), 0);
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float pitch = player.getXRot() * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch)).normalize();
        int motes = 2 + level.getRandom().nextInt(3);
        for (int i = 0; i < motes; i++) {
            double x = player.getX() + dir.x - Mth.cos(yaw) * 0.16;
            double y = player.getEyeY() + dir.y - 0.5;
            double z = player.getZ() + dir.z - Mth.sin(yaw) * 0.16;
            ShardPowderEntity mote = new ShardPowderEntity(level, x, y, z, DESIDIA, player, 0);
            mote.setDeltaMovement(dir.add(level.getRandom().nextGaussian() * 0.09, level.getRandom().nextGaussian() * 0.09,
                    level.getRandom().nextGaussian() * 0.09));
            level.addFreshEntity(mote);
        }
    }

    private static void arrow(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
        arrow.setCritArrow(true);
        if (infusion(player, WitcheryInfusions.INSTANCE.getOVERWORLD().get())) {
            arrow.setBaseDamage(1.0 + Math.sqrt(pool(player, TTAspects.AVERSIO)) * 1.5);
        } else {
            arrow.setBaseDamage(2.0 + 5.0); // TC4 arrow base 2, +5
        }
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
        level.addFreshEntity(arrow);
    }

    private static void smite(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel, TagKey<EntityType<?>> kind) {
        LivingEntity target = SymbologyTargetUtil.INSTANCE.getTargetedLivingEntity(player, look, RANGE);
        if (!(target instanceof Enemy) || !target.isAlive() || !target.getType().builtInRegistryHolder().is(kind)) {
            return;
        }
        craftFail(level, player);
        RiftUtil.bolt(level, new Vec3(player.getX(), player.getY() + player.getBbHeight() - 0.5, player.getZ()),
                new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ()), 0xFFFFFF, RiftUtil.MIN_WIDTH);
        float damage = infusion(player, WitcheryInfusions.INSTANCE.getLIGHT().get())
                ? (float) (1.0 + Math.sqrt(pool(player, TTAspects.LUX)) * 2.5)
                : level.getRandom().nextInt(30) + spellLevel * 2;
        target.hurt(level.damageSources().indirectMagic(player, player), damage);
    }

    private static void blind(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
        boolean infernal = infusion(player, WitcheryInfusions.INSTANCE.getINFERNAL().get());
        int duration = 200 + (infernal ? pool(player, TTAspects.TENEBRAE) : 0);
        LivingEntity target = SymbologyTargetUtil.INSTANCE.getTargetedLivingEntity(player, look, RANGE);
        if (!(target instanceof Mob)) {
            return;
        }
        craftFail(level, player);
        MobEffectInstance blurred = new MobEffectInstance(TTMobEffects.BLURRED_VISION, duration + 200, 0);
        target.addEffect(blurred, player);
        if (infernal) {
            MobEffectInstance blindness = new MobEffectInstance(MobEffects.BLINDNESS, duration, 0);
            target.addEffect(blindness, player);
        }
    }
}
