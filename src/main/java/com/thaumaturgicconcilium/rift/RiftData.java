package com.thaumaturgicconcilium.rift;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The small data records of the rift set: item components and the lithograph player attachment. */
public final class RiftData {
    private RiftData() {
    }

    /**
     * A block a Terra-Cast Gem tore out of a dimension (TC4 NBT {@code name}, {@code meta}, {@code dim}). The rift gem
     * keeps up to {@link Gem#MAX_CASTS} of them and takes the wearer to the cast's dimension.
     */
    public record Cast(ResourceLocation block, ResourceKey<Level> dimension) {
        public static final Codec<Cast> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("block").forGetter(Cast::block),
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Cast::dimension)
        ).apply(i, Cast::new));
        public static final StreamCodec<ByteBuf, Cast> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Cast::block,
                ResourceKey.streamCodec(Registries.DIMENSION), Cast::dimension,
                Cast::new);
    }

    /**
     * An active Gem of Rifts (TC4 NBT {@code MAX_GEMS}, {@code CURR}, {@code NAME/META/DIM<n>}, {@code REVERSE}).
     *
     * @param casts   the dimensions it knows, oldest first
     * @param current the cast used when leaving the Causal Bouillon
     * @param reverse deflate (divide coordinates by 10) instead of inflate (multiply by 10) on the way out
     */
    public record Gem(List<Cast> casts, int current, boolean reverse) {
        public static final int MAX_CASTS = 16;
        public static final Gem EMPTY = new Gem(List.of(), 0, false);
        public static final Codec<Gem> CODEC = RecordCodecBuilder.create(i -> i.group(
                Cast.CODEC.listOf().optionalFieldOf("casts", List.of()).forGetter(Gem::casts),
                Codec.INT.optionalFieldOf("current", 0).forGetter(Gem::current),
                Codec.BOOL.optionalFieldOf("reverse", false).forGetter(Gem::reverse)
        ).apply(i, Gem::new));
        public static final StreamCodec<ByteBuf, Gem> STREAM_CODEC = StreamCodec.composite(
                Cast.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CASTS)), Gem::casts,
                ByteBufCodecs.VAR_INT, Gem::current,
                ByteBufCodecs.BOOL, Gem::reverse,
                Gem::new);

        public Optional<Cast> currentCast() {
            return current >= 0 && current < casts.size() ? Optional.of(casts.get(current)) : Optional.empty();
        }

        public Gem cycle() {
            return casts.isEmpty() ? this : new Gem(casts, (current + 1) % casts.size(), reverse);
        }

        public Gem toggleReverse() {
            return new Gem(casts, current, !reverse);
        }

        /** TC4's TerraGemRecipe: append and select the new cast; refused when full. */
        public Optional<Gem> withCast(Cast cast) {
            if (casts.size() >= MAX_CASTS) {
                return Optional.empty();
            }
            List<Cast> more = new ArrayList<>(casts);
            more.add(cast);
            return Optional.of(new Gem(List.copyOf(more), more.size() - 1, reverse));
        }
    }

    /**
     * What a held Astral Monitor last read about the player its wand names (TC4 NBT {@code vals}): health 0..10, then
     * blood, mana and runic shielding 0..9, -1 for none. Blood (Blood Magic) and mana (Botania) stay -1 in this pack.
     */
    public record Readings(int health, int blood, int mana, int runes) {
        public static final Codec<Readings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("health").forGetter(Readings::health),
                Codec.INT.fieldOf("blood").forGetter(Readings::blood),
                Codec.INT.fieldOf("mana").forGetter(Readings::mana),
                Codec.INT.fieldOf("runes").forGetter(Readings::runes)
        ).apply(i, Readings::new));
        public static final StreamCodec<ByteBuf, Readings> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Readings::health,
                ByteBufCodecs.VAR_INT, Readings::blood,
                ByteBufCodecs.VAR_INT, Readings::mana,
                ByteBufCodecs.VAR_INT, Readings::runes,
                Readings::new);
    }

    /**
     * One of the four things a lithographed player watches: a player (from a xylographed wand) or a place (from an item
     * with an {@code astral_target}, the phase 4 Crystal Eye / Red-Powered Mind).
     */
    public record Target(Optional<String> player, Optional<GlobalPos> pos) {
        public static final Target NONE = new Target(Optional.empty(), Optional.empty());
        public static final Codec<Target> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("player").forGetter(Target::player),
                GlobalPos.CODEC.optionalFieldOf("pos").forGetter(Target::pos)
        ).apply(i, Target::new));
        public static final StreamCodec<ByteBuf, Target> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Target::player,
                ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), Target::pos,
                Target::new);
    }

    /**
     * TC4's {@code TCPlayerCapabilities} lithograph fields: the Lithographer imprinted four targets into the sleeper's
     * sight ({@code lithographed}); Coagulating Eye Drops toggle the overlay off and on ({@code relieved}).
     */
    public record Lithograph(boolean lithographed, boolean relieved, List<Target> monitored) {
        public static final Lithograph NONE = new Lithograph(false, false, List.of());
        public static final Codec<Lithograph> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("lithographed", false).forGetter(Lithograph::lithographed),
                Codec.BOOL.optionalFieldOf("relieved", false).forGetter(Lithograph::relieved),
                Target.CODEC.listOf().optionalFieldOf("monitored", List.of()).forGetter(Lithograph::monitored)
        ).apply(i, Lithograph::new));
        public static final StreamCodec<ByteBuf, Lithograph> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Lithograph::lithographed,
                ByteBufCodecs.BOOL, Lithograph::relieved,
                Target.STREAM_CODEC.apply(ByteBufCodecs.list(4)), Lithograph::monitored,
                Lithograph::new);

        public boolean showing() {
            return lithographed && !relieved;
        }
    }
}
