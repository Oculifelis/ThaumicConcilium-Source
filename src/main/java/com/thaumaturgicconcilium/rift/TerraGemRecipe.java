package com.thaumaturgicconcilium.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * TC4 {@code TerraGemRecipe}: an active Terra-Cast Gem that holds a cast plus an active Gem of Rifts give the rift gem
 * with the cast added (up to 16) and selected. The terra gem is used up. TC4 also matched an empty terra gem and
 * consumed it for nothing; here it needs a cast.
 */
public class TerraGemRecipe extends CustomRecipe {
    public static final TerraGemRecipe INSTANCE = new TerraGemRecipe();
    public static final MapCodec<TerraGemRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, TerraGemRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<TerraGemRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return ItemStack.EMPTY;
        }
        ItemStack rift = ItemStack.EMPTY;
        ItemStack terra = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof RiftGemItem) {
                rift = s;
            } else if (s.getItem() instanceof TerraCastGemItem) {
                terra = s;
            }
        }
        if (rift.isEmpty() || terra.isEmpty()) {
            return ItemStack.EMPTY;
        }
        RiftData.Cast cast = terra.get(TCRifts.TERRA_CAST.get());
        if (cast == null) {
            return ItemStack.EMPTY;
        }
        ItemStack fRift = rift;
        return rift.getOrDefault(TCRifts.RIFT_GEM_DATA.get(), RiftData.Gem.EMPTY).withCast(cast).map(gem -> {
            ItemStack out = fRift.copyWithCount(1);
            out.set(TCRifts.RIFT_GEM_DATA.get(), gem);
            return out;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<TerraGemRecipe> getSerializer() {
        return TCRifts.TERRA_GEM_RECIPE.get();
    }
}
