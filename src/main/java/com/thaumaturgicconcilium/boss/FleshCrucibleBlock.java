package com.thaumaturgicconcilium.boss;

import com.mojang.serialization.MapCodec;
import com.thaumaturgicconcilium.mob.MadThaumaturgeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Crucible of Flesh (TC4 Concilium {@code FleshCrucibleBlock}): made from a Quicksilver Reactor by infusion. While it
 * has no redstone signal a Mad Thaumaturge that walks in is rendered down into 2 flesh (up to 64), a player who owes
 * flesh (from the ethereal state) takes one back per tick of contact, and anything else alive is scalded for 1. A
 * comparator reads how full it is.
 */
public class FleshCrucibleBlock extends BaseEntityBlock {
    public static final MapCodec<FleshCrucibleBlock> CODEC = simpleCodec(FleshCrucibleBlock::new);
    private static final VoxelShape INSIDE = Block.box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), INSIDE, BooleanOp.ONLY_FIRST);

    public FleshCrucibleBlock(Properties properties) {
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
        return new FleshCrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, TCBosses.FLESH_CRUCIBLE_BE.get(), FleshCrucibleBlockEntity::clientTick) : null;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server) || server.hasNeighborSignal(pos) || !(level.getBlockEntity(pos) instanceof FleshCrucibleBlockEntity be)) {
            return;
        }
        if (entity instanceof MadThaumaturgeEntity mad) {
            be.smelt(mad);
        } else if (entity instanceof Player player) {
            if (Ethereal.fleshLost(player) > 0) {
                be.sip(player);
            }
        } else if (entity instanceof LivingEntity living) {
            living.hurt(server.damageSources().inFire(), 1.0F);
            server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 2.0F + server.getRandom().nextFloat() * 0.4F);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FleshCrucibleBlockEntity be ? be.comparator() : 0;
    }
}
