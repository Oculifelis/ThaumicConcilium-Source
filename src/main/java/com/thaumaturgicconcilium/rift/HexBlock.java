package com.thaumaturgicconcilium.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * Hex of Predictability (TC4 {@code HexOfPredictabilityBlock}): the 3x3 a wand makes out of a ring of eight Ancient
 * Stone around a centre block (bedrock by default, block tag {@code thaumaturgicconcilium:hex_center}). The middle one is the
 * master and has the block entity; it drops nothing, and breaking any of the nine turns the rest back into what they were.
 */
public class HexBlock extends BaseEntityBlock {
    public static final MapCodec<HexBlock> CODEC = simpleCodec(HexBlock::new);
    public static final BooleanProperty MASTER = BooleanProperty.create("master");

    public HexBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(MASTER, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MASTER);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(MASTER) ? new HexBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !state.getValue(MASTER) ? null : createTickerHelper(type, TCRifts.HEX_BE.get(), HexBlockEntity::serverTick);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            HexBlockEntity.findMaster(level, pos).ifPresent(HexBlockEntity::dissolve);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
