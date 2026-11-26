package com.thaumaturgicconcilium.rift;

import com.mojang.serialization.MapCodec;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Vis Condenser (TC4 {@code VisCondenserBlock}): a stand under a Destabilized Crystal. Redstone from a side fires the
 * crystal's charge as a bolt the other way, as far as the signal is strong; redstone from below pours the charge into the
 * nearest Hex of Predictability that holds a rift.
 */
public class VisCondenserBlock extends BaseEntityBlock {
    public static final MapCodec<VisCondenserBlock> CODEC = simpleCodec(VisCondenserBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 11.2, 16);

    public VisCondenserBlock(Properties props) {
        super(props);
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VisCondenserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCRifts.VIS_CONDENSER_BE.get(), VisCondenserBlockEntity::tick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        super.neighborChanged(state, level, pos, block, fromPos, moved);
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof VisCondenserBlockEntity condenser) {
            condenser.redstone(server);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof VisCondenserBlockEntity condenser && condenser.cooldown() > 0) {
            for (int i = 0; i < 5; i++) {
                level.addParticle(new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0x960000).toVector3f(), 3.0F), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0, 0);
            }
        }
        if (level.getBlockEntity(pos.above()) instanceof DestabilizedCrystalBlockEntity crystal) {
            int c = crystal.aspect().map(a -> RiftUtil.aspectColor(level, a)).orElse(0xFFFFFF);
            double y = pos.getY() + 1.2;
            arc(level, random, pos.getX(), pos.getZ(), pos.getX() + 0.9, pos.getZ(), y, c);
            arc(level, random, pos.getX(), pos.getZ() + 0.9, pos.getX(), pos.getZ(), y, c);
            arc(level, random, pos.getX() + 0.9, pos.getZ(), pos.getX() + 0.9, pos.getZ() + 0.9, y, c);
            arc(level, random, pos.getX() + 0.9, pos.getZ() + 0.9, pos.getX(), pos.getZ() + 0.9, y, c);
        }
    }

    /** TC4 drew four little arcs around the crystal, each with a one-in-two chance. */
    private static void arc(Level level, RandomSource random, double x0, double z0, double x1, double z1, double y, int rgb) {
        if (random.nextInt(2) != 0) {
            return;
        }
        level.addParticle(new com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions(x1, y + (random.nextDouble() - 0.5), z1,
                ((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, RiftUtil.MIN_WIDTH), x0, y + (random.nextDouble() - 0.5), z0,
                0, 0, 0);
    }

    static int sidePower(Level level, BlockPos pos, Direction side) {
        return level.getSignal(pos.relative(side), side);
    }
}
