package com.thaumaturgicconcilium.rift;

import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Optional phase 4 hook for the Astral Monitor's and the Lithographer's "blood" and "mana" glyphs (TC4: Blood Magic's
 * soul network and Botania's mana). Each is a reader from a watched player to 0..9, or -1 for a dark glyph. Nothing is
 * set by default, so both stay dark; compat code for a loaded partner (Neo Vitae for blood, Ars Magica Legacy for mana)
 * sets one at mod construction. A reader that throws is treated as -1.
 */
public final class ExtraReadings {
    private static volatile ToIntFunction<ServerPlayer> blood = p -> -1;
    private static volatile ToIntFunction<ServerPlayer> mana = p -> -1;

    private ExtraReadings() {
    }

    public static void setBlood(ToIntFunction<ServerPlayer> reader) {
        blood = reader;
    }

    public static void setMana(ToIntFunction<ServerPlayer> reader) {
        mana = reader;
    }

    public static int blood(@Nullable ServerPlayer player) {
        return read(blood, player);
    }

    public static int mana(@Nullable ServerPlayer player) {
        return read(mana, player);
    }

    private static int read(ToIntFunction<ServerPlayer> reader, @Nullable ServerPlayer player) {
        if (player == null) {
            return -1;
        }
        try {
            return reader.applyAsInt(player);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** TC4's glyph scale: {@code round(value / max * 10) - 1}, clamped to 0..9; -1 when there is no maximum. */
    public static int scale(double value, double max) {
        if (max <= 0.0) {
            return -1;
        }
        int v = (int) Math.round(value / max * 10.0) - 1;
        return Math.max(0, Math.min(9, v));
    }
}
