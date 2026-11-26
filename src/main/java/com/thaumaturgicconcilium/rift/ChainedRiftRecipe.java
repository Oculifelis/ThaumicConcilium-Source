package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * A Chained Rift recipe (TC4 {@code ChainedRiftRecipe}, MineTweaker {@code mods.thaumaturgicconcilium.ChainedRift}): throw
 * the catalyst into a chained rift whose Hex of Predictability holds at least these essentia, and it comes out as the
 * result. Datapack registry {@code data/<ns>/thaumaturgicconcilium/chained_rift/}.
 *
 * <p>The result is an item id resolved when the recipe is used, so a recipe whose result belongs to an absent module
 * (the Pontifex robe) is skipped instead of failing to load.
 *
 * @param research the research the thrower must have finished
 * @param catalyst the thrown item
 * @param aspects  essentia the Hex must hold (TC4 amounts divided by 4)
 * @param result   the item that comes out
 * @param count    how many
 */
public record ChainedRiftRecipe(Identifier research, Ingredient catalyst, Map<ResourceKey<IAspect>, Integer> aspects, Identifier result,
                                int count) {
    public static final ResourceKey<Registry<ChainedRiftRecipe>> REGISTRY_KEY = ResourceKey.createRegistryKey(ThaumaturgicConcilium.id("chained_rift"));
    public static final Codec<Map<ResourceKey<IAspect>, Integer>> ASPECTS = Codec.unboundedMap(ResourceKey.codec(IAspect.REGISTRY_KEY),
            Codec.intRange(1, 100000));
    public static final Codec<ChainedRiftRecipe> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("research").forGetter(ChainedRiftRecipe::research),
            Ingredient.CODEC.fieldOf("catalyst").forGetter(ChainedRiftRecipe::catalyst),
            ASPECTS.fieldOf("aspects").forGetter(ChainedRiftRecipe::aspects),
            Identifier.CODEC.fieldOf("result").forGetter(ChainedRiftRecipe::result),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ChainedRiftRecipe::count)
    ).apply(i, ChainedRiftRecipe::new));

    public boolean matches(Map<ResourceKey<IAspect>, Integer> essentia, ItemStack thrown) {
        if (!catalyst.test(thrown)) {
            return false;
        }
        for (Map.Entry<ResourceKey<IAspect>, Integer> need : aspects.entrySet()) {
            if (essentia.getOrDefault(need.getKey(), 0) < need.getValue()) {
                return false;
            }
        }
        return true;
    }

    public Optional<ItemStack> output() {
        return BuiltInRegistries.ITEM.getOptional(result).map((Item item) -> new ItemStack(item, count));
    }

    /**
     * TC4 {@code findMatchingRiftRecipe}: among the recipes the thrower has researched that match, the one asking for
     * the most different aspects wins.
     */
    public static Optional<ChainedRiftRecipe> find(Level level, Player thrower, Map<ResourceKey<IAspect>, Integer> essentia, ItemStack thrown) {
        Optional<Registry<ChainedRiftRecipe>> registry = level.registryAccess().lookup(REGISTRY_KEY);
        if (registry.isEmpty()) {
            return Optional.empty();
        }
        ChainedRiftRecipe best = null;
        for (ChainedRiftRecipe recipe : registry.get()) {
            if (!recipe.matches(essentia, thrown) || recipe.output().isEmpty()
                    || !KnowledgeAccess.of(thrower).isResearchComplete(recipe.research())) {
                continue;
            }
            if (best == null || recipe.aspects().size() > best.aspects().size()) {
                best = recipe;
            }
        }
        return Optional.ofNullable(best);
    }
}
