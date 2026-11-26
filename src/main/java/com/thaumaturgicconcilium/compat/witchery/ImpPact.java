package com.thaumaturgicconcilium.compat.witchery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import dev.sterner.witchery.core.api.event.ImpEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The imp deal (TC4 {@code ImpDealTransformer}, {@code WitcheryImpDealEvent} and {@code ImpDealTargetAI}). A player who
 * right-clicks a Witchery imp holding an item from {@link #OFFERINGS} gives it up, the imp answers in chat, and the player
 * holds a pact for {@code duration_ticks} (data file {@code thaumaturgicconcilium/imp_pact/pact.json}, one hour by
 * default). While it lasts, mobs in {@link #HONOURS} do not take that player as a target; a Chort says so in chat at most
 * once a minute. A player who hurts one of those mobs breaks the pact with that mob only, and it may fight back.
 *
 * <p>Witchery's imp posts {@code ImpEvents.Interact} on the game bus from its server-side {@code mobInteract}, so no
 * mixin is needed. This class is only loaded behind the Witchery check in {@code CompatA}.
 */
public final class ImpPact {
    public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, ThaumaturgicConcilium.id("imp_offerings"));
    public static final TagKey<EntityType<?>> HONOURS = TagKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id("honours_imp_pact"));
    /** TC4 {@code TCConfig.impPactTime}: 72000 ticks. */
    public static final int DEFAULT_DURATION = 72000;
    /** TC4 {@code Chort.chatCooldown}. */
    private static final int CHORT_CHAT_COOLDOWN = 1200;

    static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
            ThaumaturgicConcilium.MODID);
    /** On a player: the game time the pact ends (0 = none). Kept through death, like the TC4 player data. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> PACT_END = ATTACHMENTS.register("imp_pact_end",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("v")).copyOnDeath().build());
    /** On a pact-honouring mob: the players who hurt it, so the pact no longer covers them for this mob. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<UUID>>> BROKEN = ATTACHMENTS.register("imp_pact_broken",
            () -> AttachmentType.<List<UUID>>builder(() -> List.of()).serialize(UUIDUtil.CODEC.listOf().fieldOf("v")).build());
    /** On a Chort: the game time it may speak about a pact again (not saved). */
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> CHAT_AT = ATTACHMENTS.register("imp_pact_chat",
            () -> AttachmentType.builder(() -> 0L).build());

    private static int duration = DEFAULT_DURATION;

    private ImpPact() {
    }

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(ImpEvents.Interact.class, ImpPact::offer);
        NeoForge.EVENT_BUS.addListener(LivingChangeTargetEvent.class, ImpPact::spare);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, ImpPact::hurt);
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(ThaumaturgicConcilium.id("imp_pact"), new Listener()));
    }

    public static int duration() {
        return duration;
    }

    public static boolean hasPact(Player player) {
        return player.getData(PACT_END) > player.level().getGameTime();
    }

    /** True when {@code mob} honours a pact {@code player} holds and the player has not hurt it. */
    public static boolean spares(Entity mob, Player player) {
        return mob.getType().builtInRegistryHolder().is(HONOURS) && hasPact(player) && !mob.getData(BROKEN).contains(player.getUUID());
    }

    private static void offer(ImpEvents.Interact event) {
        Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(player.level() instanceof ServerLevel level) || !stack.is(OFFERINGS)) {
            return;
        }
        Mob imp = event.getImp();
        stack.consume(1, player);
        player.setData(PACT_END, level.getGameTime() + duration);
        player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.imp.pact", imp.getDisplayName())
                .withStyle(ChatFormatting.GOLD));
        level.sendParticles(ParticleTypes.FLAME, imp.getX(), imp.getY() + imp.getBbHeight() * 0.6, imp.getZ(), 12, 0.25, 0.3, 0.25, 0.02);
        // Mobs already after the player let go at once, as long as the player has not hurt them.
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0), m -> m.getTarget() == player)) {
            if (spares(mob, player)) {
                mob.setTarget(null);
            }
        }
        event.setCanceled(true);
    }

    private static void spare(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || mob.level().isClientSide() || !spares(mob, player)) {
            return;
        }
        event.setCanceled(true);
        if (mob instanceof ChortEntity) {
            long now = mob.level().getGameTime();
            if (mob.getData(CHAT_AT) <= now) {
                mob.setData(CHAT_AT, now + CHORT_CHAT_COOLDOWN);
                player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.chort.has_pact").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    private static void hurt(LivingDamageEvent.Post event) {
        LivingEntity mob = event.getEntity();
        if (event.getSource().getEntity() instanceof Player player && !mob.level().isClientSide() && spares(mob, player)) {
            List<UUID> broken = new ArrayList<>(mob.getData(BROKEN));
            broken.add(player.getUUID());
            mob.setData(BROKEN, List.copyOf(broken));
        }
    }

    /** {@code data/<ns>/thaumaturgicconcilium/imp_pact/pact.json}: {@code {"duration_ticks": 72000}}. */
    record Settings(int durationTicks) {
        static final Codec<Settings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration_ticks").forGetter(Settings::durationTicks)).apply(i, Settings::new));
    }

    static final class Listener extends SimpleJsonResourceReloadListener<Settings> {
        private static final Identifier FILE = ThaumaturgicConcilium.id("pact");

        Listener() {
            super(Settings.CODEC, FileToIdConverter.json("thaumaturgicconcilium/imp_pact"));
        }

        @Override
        protected void apply(Map<Identifier, Settings> files, ResourceManager rm, ProfilerFiller profiler) {
            Settings s = files.get(FILE);
            duration = s != null ? s.durationTicks() : DEFAULT_DURATION;
        }
    }
}
