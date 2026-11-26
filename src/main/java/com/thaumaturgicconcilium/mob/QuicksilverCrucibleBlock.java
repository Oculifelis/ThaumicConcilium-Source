package com.thaumaturgicconcilium.mob;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Quicksilver Crucible (TC4 Concilium): made only by a Quicksilver Elemental melting into a Thaumaturge crucible.
 * Quicksilver thrown in adds 2 Permutatio each (up to 64). Every 20 ticks while unpowered it spends 1 Permutatio to
 * fuse the last two aspects of a crucible within 8 into their compound. Living things inside are scalded; a
 * comparator reads its Permutatio.
 */
public class QuicksilverCrucibleBlock extends BaseEntityBlock {
    public static final MapCodec<QuicksilverCrucibleBlock> CODEC = simpleCodec(QuicksilverCrucibleBlock::new);
    private static final VoxelShape INSIDE = Block.box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), INSIDE, net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST);

    public QuicksilverCrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuicksilverCrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TCMobs.QUICKSILVER_CRUCIBLE_BE.get(), QuicksilverCrucibleBlockEntity::serverTick);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof QuicksilverCrucibleBlockEntity be)) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            be.absorb(item);
        } else if (entity instanceof LivingEntity living && server.getGameTime() % 10 == 0) {
            living.hurt(server.damageSources().hotFloor(), 1.0F);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof QuicksilverCrucibleBlockEntity be ? be.comparator() : 0;
    }
}
