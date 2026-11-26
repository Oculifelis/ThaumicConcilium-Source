package com.thaumaturgicconcilium;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Data maps: {@code data/thaumaturgicconcilium/data_maps/item/polishment.json} and {@code mill_shard.json}, plus {@code data_maps/thaumaturge/spell_part/polishment.json}. */
public final class TCDataMaps {
    /**
     * Polishment: an infusion whose result is in this map also needs that much of one aspect, drawn from a Destabilized
     * Crystal through a Vis Conductor spell by the crafting player (within 16 blocks of the matrix) when the craft finishes.
     * Without it the craft is lost.
     */
    public record Polishment(ResourceKey<IAspect> aspect, int amount) {
        public static final Codec<Polishment> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(Polishment::aspect),
                Codec.intRange(1, 100000).fieldOf("amount").forGetter(Polishment::amount)).apply(i, Polishment::new));
    }

    /** Mill shard: an item the Shard Mill grinds as if it were a vis crystal of this aspect (Forbidden Magic's sin shards). */
    public record MillShard(ResourceKey<IAspect> aspect) {
        public static final Codec<MillShard> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(MillShard::aspect)).apply(i, MillShard::new));
    }

    public static final DataMapType<Item, MillShard> MILL_SHARD = DataMapType.builder(ThaumaturgicConcilium.id("mill_shard"), Registries.ITEM,
            MillShard.CODEC).synced(MillShard.CODEC, false).build();

    public static final DataMapType<Item, Polishment> POLISHMENT = DataMapType.builder(ThaumaturgicConcilium.id("polishment"), Registries.ITEM,
            Polishment.CODEC).synced(Polishment.CODEC, false).build();

    /** Polishment for infused foci: keyed by spell part, so a focus carrying this effect needs it. */
    public static final DataMapType<SpellPart, Polishment> SPELL_POLISHMENT = DataMapType.builder(ThaumaturgicConcilium.id("polishment"),
            SpellPart.REGISTRY_KEY, Polishment.CODEC).synced(Polishment.CODEC, false).build();

    /**
     * Spawn replacement: when an entity of this type first joins a world (not loaded from disk), it is replaced by
     * {@code entity} with this chance (TC4 Concilium: 20% of Brainy Zombies become Mad Thaumaturges, 20% of Cultist
     * Knights become Crimson Paladins).
     */
    public record Replacement(net.minecraft.resources.Identifier entity, float chance) {
        public static final Codec<Replacement> CODEC = RecordCodecBuilder.create(i -> i.group(
                net.minecraft.resources.Identifier.CODEC.fieldOf("entity").forGetter(Replacement::entity),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Replacement::chance)).apply(i, Replacement::new));
    }

    public static final DataMapType<net.minecraft.world.entity.EntityType<?>, Replacement> SPAWN_REPLACEMENT = DataMapType.builder(
            ThaumaturgicConcilium.id("spawn_replacement"), Registries.ENTITY_TYPE, Replacement.CODEC).build();

    /**
     * Vis a wand pays to build a structure, keyed by the block it builds: the Hex of Predictability (thin and thick) and
     * the Lithographer. A block with no entry builds for free.
     */
    public static final DataMapType<net.minecraft.world.level.block.Block, java.util.Map<ResourceKey<IAspect>, Integer>> WAND_BUILD_COST =
            DataMapType.builder(ThaumaturgicConcilium.id("wand_build_cost"), Registries.BLOCK,
                    Codec.unboundedMap(ResourceKey.codec(IAspect.REGISTRY_KEY), Codec.intRange(0, 1000000))).build();

    public static java.util.Map<ResourceKey<IAspect>, Integer> wandBuildCost(net.minecraft.world.level.block.Block block) {
        java.util.Map<ResourceKey<IAspect>, Integer> cost = block.builtInRegistryHolder().getData(WAND_BUILD_COST);
        return cost == null ? java.util.Map.of() : cost;
    }

    private TCDataMaps() {
    }

    static void register(IEventBus bus) {
        bus.addListener((RegisterDataMapTypesEvent e) -> {
            e.register(POLISHMENT);
            e.register(MILL_SHARD);
            e.register(SPELL_POLISHMENT);
            e.register(SPAWN_REPLACEMENT);
            e.register(WAND_BUILD_COST);
        });
    }
}
