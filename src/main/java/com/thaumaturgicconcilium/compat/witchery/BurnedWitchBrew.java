package com.thaumaturgicconcilium.compat.witchery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import dev.sterner.witchery.content.item.WitcheryPotionIngredient;
import dev.sterner.witchery.core.registry.WitcheryDataComponents;
import dev.sterner.witchery.core.registry.WitcheryItems;
import dev.sterner.witchery.core.reload_listener.PotionDataReloadListener;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A brew the Burned Witch throws (datapack registry {@code thaumaturgicconcilium:burned_witch_brew}): Witchery cauldron
 * ingredients in the order they go in, as item ids. The TC4 witch carried four fixed 1.7.10 brews (attack, attack for
 * vampires, attack for werewolves, ill-fitting); their lists are data here (tools/gen_compat_a.py), translated to
 * Witchery 0.5 items. Each item is looked up in Witchery's potion ingredient data when the brew is made; an item that
 * is not an ingredient (or does not exist) is skipped.
 */
public record BurnedWitchBrew(List<ResourceLocation> ingredients) {
    public static final ResourceKey<Registry<BurnedWitchBrew>> REGISTRY_KEY = ResourceKey.createRegistryKey(ThaumaturgicConcilium.id("burned_witch_brew"));
    public static final Codec<BurnedWitchBrew> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.listOf().fieldOf("ingredients").forGetter(BurnedWitchBrew::ingredients)
    ).apply(i, BurnedWitchBrew::new));

    public static final ResourceKey<BurnedWitchBrew> ATTACK = key("attack");
    public static final ResourceKey<BurnedWitchBrew> ATTACK_VAMPIRE = key("attack_vampire");
    public static final ResourceKey<BurnedWitchBrew> ATTACK_WEREWOLF = key("attack_werewolf");
    public static final ResourceKey<BurnedWitchBrew> ILL_FITTING = key("ill_fitting");

    private static ResourceKey<BurnedWitchBrew> key(String name) {
        return ResourceKey.create(REGISTRY_KEY, ThaumaturgicConcilium.id(name));
    }

    /** The brew as a Witchery potion stack, or empty when the brew is missing or none of its items are ingredients. */
    public static ItemStack make(Level level, ResourceKey<BurnedWitchBrew> key) {
        BurnedWitchBrew brew = level.registryAccess().registry(REGISTRY_KEY).flatMap(r -> r.getOptional(key)).orElse(null);
        if (brew == null) {
            return ItemStack.EMPTY;
        }
        List<WitcheryPotionIngredient> contents = new ArrayList<>();
        for (ResourceLocation id : brew.ingredients()) {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null) {
                continue;
            }
            WitcheryPotionIngredient ingredient = PotionDataReloadListener.INSTANCE.getIngredientFromItem(new ItemStack(item));
            if (ingredient != null) {
                contents.add(ingredient);
            }
        }
        if (contents.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(WitcheryItems.INSTANCE.getWITCHERY_POTION().get());
        stack.set(WitcheryDataComponents.INSTANCE.getWITCHERY_POTION_CONTENT().get(), contents);
        return stack;
    }
}
