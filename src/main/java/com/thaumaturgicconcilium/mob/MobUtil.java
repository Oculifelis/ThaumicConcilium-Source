package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/** Shared bits of the Concilium mobs: sounds by id, research unlocks and research hints. */
public final class MobUtil {
    private MobUtil() {
    }

    /** One of the original's sounds (registered by TCRegistry). */
    public static DeferredHolder<SoundEvent, SoundEvent> tcSound(String name) {
        return DeferredHolder.create(Registries.SOUND_EVENT, ThaumaturgicConcilium.id(name));
    }

    /** A sound by id, or null when it is not registered. */
    public static @Nullable SoundEvent sound(String id) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    public static void play(Level level, Entity at, @Nullable SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (sound != null) {
            level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, source, volume, pitch);
        }
    }

    /**
     * Unlocks a research for a player (TC4 {@code ThaumcraftApiHelper} research + heartbeat): just that entry, never its
     * parents. Returns false when it was already complete.
     */
    public static boolean unlockResearch(ServerPlayer player, ResourceLocation research) {
        if (KnowledgeAccess.of(player).isResearchComplete(research)) {
            return false;
        }
        boolean done = ResearchManager.complete(player, research);
        if (done) {
            play(player.level(), player, sound("thaumaturge:heartbeat"), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return done;
    }

    /** Picks a weighted hint and gives it to the player; returns false when there is none. */
    public static boolean giveHint(ServerPlayer player, List<ThaumaturgeTrade.Hint> hints, RandomSource random) {
        int total = hints.stream().mapToInt(ThaumaturgeTrade.Hint::weight).sum();
        if (total <= 0) {
            return false;
        }
        int roll = random.nextInt(total);
        for (ThaumaturgeTrade.Hint hint : hints) {
            roll -= hint.weight();
            if (roll < 0) {
                give(player, hint);
                return true;
            }
        }
        return false;
    }

    private static void give(ServerPlayer player, ThaumaturgeTrade.Hint hint) {
        Holder<IResearchCategory> category = player.level().registryAccess().lookupOrThrow(IResearchCategory.REGISTRY_KEY).get(hint.category())
                .map(h -> (Holder<IResearchCategory>) h).orElse(null);
        if (category != null) {
            ResearchManager.gainKnowledge(player, KnowledgeType.OBSERVATION, category, KnowledgeType.OBSERVATION.progression() * hint.amount());
        }
        Component title = Component.translatable("research." + hint.research().getNamespace() + "." + hint.research().getPath() + ".title");
        Component line = hint.text().<Component>map(Component::translatable).orElseGet(() -> Component.translatable("message.thaumaturgicconcilium.hint", title));
        player.sendSystemMessage(line.copy().withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.hint.points", title).withStyle(ChatFormatting.GRAY));
        play(player.level(), player, sound("thaumaturge:learn"), SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    /** A random line {@code key.0 .. key.(count-1)} in dark purple, as the original's taunts and trade chatter. */
    public static void chat(Player player, String key, int count, RandomSource random) {
        player.sendSystemMessage(Component.translatable(key + "." + random.nextInt(count)).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
