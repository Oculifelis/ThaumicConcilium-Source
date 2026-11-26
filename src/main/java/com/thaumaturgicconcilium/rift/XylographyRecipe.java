package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Wand Xylography (TC4 {@code WandXylography}): a wand alone in the crafting grid comes out carved, data component
 * {@code xylography} set to a blank that the crafting event fills with the crafter's name ({@link RiftEvents}). The
 * Astral Monitor and the Lithographer read that name. TC4 matched any number of wands; here exactly one.
 */
public class XylographyRecipe extends CustomRecipe {
    public static final XylographyRecipe INSTANCE = new XylographyRecipe();
    public static final MapCodec<XylographyRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, XylographyRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<XylographyRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<XylographyRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, XylographyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    public XylographyRecipe() {
        super(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }
    /** TC4 wrote a single space until the crafting event knew the player. */
    public static final String BLANK = " ";

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !wand(input).isEmpty();
    }

    private static ItemStack wand(CraftingInput input) {
        if (input.ingredientCount() != 1) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (!s.isEmpty()) {
                return s.getItem() instanceof ItemWand ? s : ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack wand = wand(input);
        if (wand.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = wand.copyWithCount(1);
        out.set(TCRifts.XYLOGRAPHY.get(), BLANK);
        return out;
    }

    @Override
    public RecipeSerializer<XylographyRecipe> getSerializer() {
        return TCRifts.XYLOGRAPHY_RECIPE.get();
    }
}
