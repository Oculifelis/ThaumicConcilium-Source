package com.thaumaturgicconcilium.compat.horizons;

import com.mojang.serialization.MapCodec;
import com.thaumaturgicconcilium.rift.AstralHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Red-Powered Mind block (TC4 Concilium {@code RedPoweredMindBlock}): a brain wired to redstone. The power it is given is
 * kept in its tile, and a Lithographer watching this place reads it as the brain's thought strength (see
 * {@link AstralHooks.BrainSource}). TC4 only refreshed the power when a neighbour changed, so a mind placed next to a
 * lit lever read 0 until something moved; it is now read on placement too.
 */
public class RedPoweredMindBlock extends BaseEntityBlock implements AstralHooks.BrainSource {
    public static final MapCodec<RedPoweredMindBlock> CODEC = simpleCodec(RedPoweredMindBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.4, 3.52, 2.4, 13.6, 12.8, 13.6);

    public RedPoweredMindBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedPoweredMindBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, HorizonsContent.RED_POWERED_MIND_BE.get(), RedPoweredMindBlockEntity::clientTick) : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, block, orientation, moved);
        refresh(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        refresh(level, pos);
    }

    private static void refresh(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RedPoweredMindBlockEntity be) {
            be.setPower(level.getBestNeighborSignal(pos));
        }
    }

    @Override
    public int brainPower(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RedPoweredMindBlockEntity be ? be.power() : 0;
    }
}
