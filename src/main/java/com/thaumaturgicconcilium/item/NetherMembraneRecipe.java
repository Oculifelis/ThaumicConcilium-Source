package com.thaumaturgicconcilium.item;

import com.mojang.serialization.MapCodec;
import com.thaumaturgicconcilium.TCRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Nether Membrane: craft it with any one item (that has no crafting remainder and is not fire-proof yet) and the item
 * comes out wrapped, so as a dropped item it no longer burns in fire or lava (vanilla's damage-resistant component).
 */
public class NetherMembraneRecipe extends CustomRecipe {
    public static final NetherMembraneRecipe INSTANCE = new NetherMembraneRecipe();
    public static final MapCodec<NetherMembraneRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, NetherMembraneRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<NetherMembraneRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static ItemStack target(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return ItemStack.EMPTY;
        }
        ItemStack target = ItemStack.EMPTY;
        boolean membrane = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.is(TCRegistry.NETHER_MEMBRANE.get()) && !membrane) {
                membrane = true;
            } else if (target.isEmpty()) {
                target = s;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (!membrane || target.isEmpty() || target.has(DataComponents.DAMAGE_RESISTANT) || target.getItem().getCraftingRemainder() != null) {
            return ItemStack.EMPTY;
        }
        return target;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !target(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack target = target(input);
        if (target.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = target.copyWithCount(1);
        // The membrane item is itself fire-proof: hand its damage-resistant component on.
        for (int i = 0; i < input.size(); i++) {
            DamageResistant fireproof = input.getItem(i).is(TCRegistry.NETHER_MEMBRANE.get())
                    ? input.getItem(i).get(DataComponents.DAMAGE_RESISTANT) : null;
            if (fireproof != null) {
                out.set(DataComponents.DAMAGE_RESISTANT, fireproof);
            }
        }
        return out;
    }

    @Override
    public RecipeSerializer<NetherMembraneRecipe> getSerializer() {
        return TCRegistry.NETHER_MEMBRANE_RECIPE.get();
    }
}
