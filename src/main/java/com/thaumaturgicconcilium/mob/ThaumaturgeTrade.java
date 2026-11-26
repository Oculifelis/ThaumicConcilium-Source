package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A Thaumaturge trade table, a datapack registry entry at {@code data/<ns>/thaumaturgicconcilium/thaumaturge_trade/<name>.json}
 * (TC4 Concilium's Pech-style {@code ContainerThaumaturge}). The Thaumaturge reads {@code thaumaturgicconcilium:thaumaturge};
 * the Mad Thaumaturge reads only the hints of {@code thaumaturgicconcilium:mad_thaumaturge}.
 *
 * <p>Trading: one offered item matching a {@code values} entry is worth that value, which then swings by +rand(2)
 * with {@code raise_chance} or -rand(2) with {@code lower_chance}. The {@code outputs} are walked in a weighted random
 * order; each roll gives the output while {@code value > rand(10)} and stops as soon as {@code value < rand(10)}.
 *
 * <p>Hints: an offered item matching {@code hint_input} (a completed research note) is swapped for a hint: the player
 * gains {@code amount} observation knowledge in the hinted research's category and is told which research it points at.
 */
public record ThaumaturgeTrade(List<Value> values, List<Output> outputs, Optional<Ingredient> hintInput, List<Hint> hints, float hintChance,
        float raiseChance, float lowerChance) {
    public static final ResourceKey<Registry<ThaumaturgeTrade>> REGISTRY_KEY = ResourceKey.createRegistryKey(ThaumaturgicConcilium.id("thaumaturge_trade"));
    public static final ResourceKey<ThaumaturgeTrade> THAUMATURGE = ResourceKey.create(REGISTRY_KEY, ThaumaturgicConcilium.id("thaumaturge"));
    public static final ResourceKey<ThaumaturgeTrade> MAD_THAUMATURGE = ResourceKey.create(REGISTRY_KEY, ThaumaturgicConcilium.id("mad_thaumaturge"));

    public record Value(Ingredient ingredient, int value) {
        public static final Codec<Value> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(Value::ingredient),
                Codec.INT.fieldOf("value").forGetter(Value::value)).apply(i, Value::new));
    }

    public record Output(ItemStackTemplate result, int weight) {
        public static final Codec<Output> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(Output::result),
                Codec.intRange(1, 10000).optionalFieldOf("weight", 1).forGetter(Output::weight)).apply(i, Output::new));
    }

    /**
     * A research hint: {@code category} receives {@code observation} points of observation knowledge, and the player is
     * shown {@code text} (a translation key; by default a line naming {@code research}).
     */
    public record Hint(Identifier research, ResourceKey<IResearchCategory> category, int weight, int amount, Optional<String> text) {
        public static final Codec<Hint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("research").forGetter(Hint::research),
                ResourceKey.codec(IResearchCategory.REGISTRY_KEY).fieldOf("category").forGetter(Hint::category),
                Codec.intRange(1, 10000).optionalFieldOf("weight", 1).forGetter(Hint::weight),
                Codec.intRange(1, 1000).optionalFieldOf("observation", 1).forGetter(Hint::amount),
                Codec.STRING.optionalFieldOf("text").forGetter(Hint::text)).apply(i, Hint::new));
    }

    public static final Codec<ThaumaturgeTrade> CODEC = RecordCodecBuilder.create(i -> i.group(
            Value.CODEC.listOf().optionalFieldOf("values", List.of()).forGetter(ThaumaturgeTrade::values),
            Output.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(ThaumaturgeTrade::outputs),
            Ingredient.CODEC.optionalFieldOf("hint_input").forGetter(ThaumaturgeTrade::hintInput),
            Hint.CODEC.listOf().optionalFieldOf("hints", List.of()).forGetter(ThaumaturgeTrade::hints),
            Codec.floatRange(0, 1).optionalFieldOf("hint_chance", 1.0F).forGetter(ThaumaturgeTrade::hintChance),
            Codec.floatRange(0, 1).optionalFieldOf("raise_chance", 0.2F).forGetter(ThaumaturgeTrade::raiseChance),
            Codec.floatRange(0, 1).optionalFieldOf("lower_chance", 0.5F).forGetter(ThaumaturgeTrade::lowerChance))
            .apply(i, ThaumaturgeTrade::new));

    public static Optional<ThaumaturgeTrade> get(ServerLevel level, ResourceKey<ThaumaturgeTrade> key) {
        return level.registryAccess().lookup(REGISTRY_KEY).flatMap(r -> r.get(key)).map(h -> h.value());
    }

    /** The value of an offered item, or 0 when the Thaumaturge does not want it. */
    public int valueOf(ItemStack stack) {
        for (Value v : values) {
            if (v.ingredient().test(stack)) {
                return v.value();
            }
        }
        return 0;
    }

    /** The hint input, and when it is a research note, only a completed one (TC4 swapped only completed notes). */
    public boolean isHintInput(ItemStack stack) {
        if (hintInput.isEmpty() || !hintInput.get().test(stack)) {
            return false;
        }
        var note = stack.get(com.leclowndu93150.thaumaturge.registry.TTDataComponents.RESEARCH_NOTE.get());
        return note == null || note.complete();
    }
}
