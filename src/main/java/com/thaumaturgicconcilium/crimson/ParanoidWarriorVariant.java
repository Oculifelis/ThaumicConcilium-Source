package com.thaumaturgicconcilium.crimson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * One kind of Paranoid Warrior (the original's Thaumium / Void / Shadow samurai), a datapack registry entry in
 * {@code data/<ns>/thaumaturgicconcilium/paranoid_warrior_variant/}. Server side only: the client sees the worn gear.
 *
 * @param weight        chance to be picked when a warrior spawns (original config 50 / 40 / 10)
 * @param damageDivisor incoming damage is divided by this instead of going through armour (3.5 / 5 / 10)
 * @param bonusDamage   added to the melee attack damage (7 / 10 / 15)
 * @param boltDamage    damage of the bolt it throws when it is down to a third of its health (2 / 4 / 6)
 * @param boltColor     colour of that bolt, {@code 0xRRGGBB}
 * @param equipment     a {@code minecraft:equipment} loot table that dresses it (armour look and blade)
 * @param loot          an extra entity loot table rolled on death on top of the warrior's own (its nuggets)
 */
public record ParanoidWarriorVariant(int weight, float damageDivisor, float bonusDamage, float boltDamage, int boltColor,
                                     ResourceKey<LootTable> equipment, ResourceKey<LootTable> loot) {
    public static final ResourceKey<Registry<ParanoidWarriorVariant>> REGISTRY_KEY = ResourceKey.createRegistryKey(
            ThaumaturgicConcilium.id("paranoid_warrior_variant"));

    /** The Thaumium warrior's numbers, used if a datapack removed every variant. */
    public static final ParanoidWarriorVariant FALLBACK = new ParanoidWarriorVariant(1, 3.5F, 7.0F, 2.0F, 0x6666DD,
            ResourceKey.create(Registries.LOOT_TABLE, ThaumaturgicConcilium.id("equipment/paranoid_warrior/thaumium")),
            ResourceKey.create(Registries.LOOT_TABLE, ThaumaturgicConcilium.id("entities/paranoid_warrior/thaumium")));

    public static final Codec<ParanoidWarriorVariant> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100000).fieldOf("weight").forGetter(ParanoidWarriorVariant::weight),
            Codec.floatRange(0.01F, 1000.0F).fieldOf("damage_divisor").forGetter(ParanoidWarriorVariant::damageDivisor),
            Codec.FLOAT.optionalFieldOf("bonus_damage", 0.0F).forGetter(ParanoidWarriorVariant::bonusDamage),
            Codec.FLOAT.optionalFieldOf("bolt_damage", 2.0F).forGetter(ParanoidWarriorVariant::boltDamage),
            Codec.INT.optionalFieldOf("bolt_color", 0x6666DD).forGetter(ParanoidWarriorVariant::boltColor),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("equipment").forGetter(ParanoidWarriorVariant::equipment),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot").forGetter(ParanoidWarriorVariant::loot)
    ).apply(i, ParanoidWarriorVariant::new));
}
