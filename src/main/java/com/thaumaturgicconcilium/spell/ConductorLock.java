package com.thaumaturgicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;

/**
 * The Destabilized Crystal a player's Vis Conductor spell is draining (TC4 kept it in the focus NBT as
 * {@code blockX/Y/Z}, {@code aspect} and {@code amount}). Set on the first pulse aimed at a crystal, grown every pulse,
 * cleared when the player lets go. Stored in a player attachment, so it survives a relog mid-hold.
 */
public final class ConductorLock {
    private ConductorLock() {
    }

    /**
     * A lock.
     *
     * @param crystal the crystal block
     * @param aspect  the crystal's aspect when locked
     * @param amount  charge drained from it so far during this hold
     */
    public record State(BlockPos crystal, ResourceKey<IAspect> aspect, int amount) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("crystal").forGetter(State::crystal),
                ResourceKey.codec(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(State::aspect),
                Codec.INT.fieldOf("amount").forGetter(State::amount)).apply(i, State::new));

        public State grown(int more) {
            return new State(crystal, aspect, amount + more);
        }
    }

    /** The player's current lock, if any. */
    public static Optional<State> get(Player player) {
        return player.getData(TCSpells.CONDUCTOR_LOCK.get());
    }

    /** Drops the player's lock. */
    public static void clear(Player player) {
        player.setData(TCSpells.CONDUCTOR_LOCK.get(), Optional.empty());
    }

    /** Sets the player's lock (the Vis Conductor spell does this). */
    public static void set(Player player, State state) {
        player.setData(TCSpells.CONDUCTOR_LOCK.get(), Optional.of(state));
    }
}
