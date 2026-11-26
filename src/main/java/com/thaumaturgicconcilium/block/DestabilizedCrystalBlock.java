package com.thaumaturgicconcilium.block;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Destabilized Crystal Cluster: sits on a block face like a crystal cluster and drops nothing. Use a vis crystal on it
 * to set its aspect (when empty) or add one charge of that aspect.
 */
public class DestabilizedCrystalBlock extends AmethystClusterBlock implements EntityBlock {
    public static final MapCodec<AmethystClusterBlock> CODEC = simpleCodec(p -> new DestabilizedCrystalBlock(p));

    public DestabilizedCrystalBlock(Properties props) {
        super(7.0F, 3.0F, props);
    }

    @Override
    public MapCodec<AmethystClusterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DestabilizedCrystalBlockEntity(pos, state);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        AspectInstance held = stack.get(TTDataComponents.CRYSTAL_ASPECT.get());
        if (held == null || !(level.getBlockEntity(pos) instanceof DestabilizedCrystalBlockEntity crystal)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide()) {
            if (!held.aspect().unwrapKey().map(crystal::fill).orElse(false)) {
                player.displayClientMessage(Component.translatable("message.thaumaturgicconcilium.crystal_mismatch").withStyle(ChatFormatting.RED), true);
                return net.minecraft.world.ItemInteractionResult.FAIL;
            }
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.6F + crystal.capacity() * 0.01F);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
