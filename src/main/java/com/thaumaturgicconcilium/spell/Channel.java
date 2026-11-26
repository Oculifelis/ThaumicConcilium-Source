package com.thaumaturgicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Hold-to-use bookkeeping for the foci that did something every tick of use and something else on release (Vis
 * Conductor, Warp Extraction, Spotlight, Alchemist's Spree, ...). Every pulse of a channelled spell touches its
 * session; a session whose holder let go (no longer using an item, or no pulse for {@link #TIMEOUT} ticks) runs its
 * release action once and ends. Sessions live in memory only, like TC4's focus NBT between two ticks of use.
 */
public final class Channel {
    /** Ticks without a pulse after which a session counts as released (longest Thaumaturge pulse is 12). */
    static final int TIMEOUT = 20;
    private static final Map<UUID, Map<ResourceKey<SpellPart>, Session>> SESSIONS = new ConcurrentHashMap<>();

    private Channel() {
    }

    /** One hold of one part. */
    public static final class Session {
        final long started;
        long last;
        /** Ticks banked toward the next "every N ticks" actions, by name. */
        private final Map<String, Integer> banks = new HashMap<>();
        /** Free slots the effect keeps between pulses. */
        private final Map<String, Object> data = new HashMap<>();
        private @Nullable Consumer<Session> release;
        private @Nullable Consumer<Session> everyTick;

        Session(long now) {
            this.started = now;
            this.last = now;
        }

        /** Ticks since the hold started. */
        public int held(long now) {
            return (int) Math.max(0, now - started);
        }

        /** Whether this is the hold's first pulse. */
        public boolean fresh() {
            return started == last;
        }

        /**
         * Adds {@code ticks} to the named bank and returns how many whole {@code every}-tick actions are due. The first
         * pulse of a hold always has one due, like TC4's tick 0.
         */
        public int due(String bank, int ticks, int every) {
            int step = Math.max(1, every);
            Integer had = banks.get(bank);
            int total = had == null ? step : had + ticks;
            int n = total / step;
            banks.put(bank, total - n * step);
            return n;
        }

        @SuppressWarnings("unchecked")
        public <T> @Nullable T get(String key) {
            return (T) data.get(key);
        }

        public void put(String key, @Nullable Object value) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        /** What to do once when the holder lets go. */
        public void onRelease(Consumer<Session> action) {
            this.release = action;
        }

        /** What to do every server tick while the hold lasts (smooth pulls between pulses). */
        public void everyTick(Consumer<Session> action) {
            this.everyTick = action;
        }
    }

    /** The session of this caster and part, started when absent; marks a pulse now. */
    public static Session touch(LivingEntity caster, ResourceKey<SpellPart> part) {
        long now = caster.level().getGameTime();
        Map<ResourceKey<SpellPart>, Session> mine = SESSIONS.computeIfAbsent(caster.getUUID(), k -> new ConcurrentHashMap<>());
        Session session = mine.get(part);
        if (session == null || now - session.last > TIMEOUT) {
            session = new Session(now);
            mine.put(part, session);
        }
        session.last = now;
        return session;
    }

    /**
     * The session of the casting player for this node's part; for other casters (golems, mobs) a one-pulse session
     * that is never kept, so every cast acts as a fresh hold with nothing to release.
     */
    public static Session of(com.leclowndu93150.thaumaturge.api.spell.cast.CastContext ctx) {
        LivingEntity caster = ctx.caster();
        if (caster instanceof Player) {
            return touch(caster, ctx.partKey());
        }
        return new Session(ctx.level().getGameTime());
    }

    /** Whether the caster is still holding this part (pulsed within the timeout). */
    public static boolean active(UUID caster, ResourceKey<SpellPart> part, long now) {
        Map<ResourceKey<SpellPart>, Session> mine = SESSIONS.get(caster);
        Session session = mine == null ? null : mine.get(part);
        return session != null && now - session.last <= TIMEOUT;
    }

    /** Runs the release of every session this player let go of; called every server tick per player. */
    static void tick(ServerPlayer player) {
        Map<ResourceKey<SpellPart>, Session> mine = SESSIONS.get(player.getUUID());
        if (mine == null || mine.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        boolean using = player.isUsingItem();
        for (Iterator<Session> it = mine.values().iterator(); it.hasNext();) {
            Session session = it.next();
            if (now - session.last > TIMEOUT || (!using && now > session.last)) {
                it.remove();
                if (session.release != null) {
                    session.release.accept(session);
                }
            } else if (session.everyTick != null) {
                session.everyTick.accept(session);
            }
        }
    }

    /** Drops every session of a player without releasing (logout, death, dimension change). */
    static void clear(Player player) {
        SESSIONS.remove(player.getUUID());
    }
}
