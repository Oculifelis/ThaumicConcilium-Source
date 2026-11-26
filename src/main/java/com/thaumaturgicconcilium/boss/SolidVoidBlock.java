package com.thaumaturgicconcilium.boss;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Solid Void (TC4 Concilium {@code SolidVoidBlock}): void gathered at the end of a wand by a whole (six-protolimb)
 * ethereal player below the world. Breaks instantly, but nothing blows it up (resistance 6000). Drawn as a window onto
 * the starfield, like an End portal, on every face open to the air.
 */
public class SolidVoidBlock extends BaseEntityBlock {
    public static final MapCodec<SolidVoidBlock> CODEC = simpleCodec(SolidVoidBlock::new);

    public SolidVoidBlock(Properties properties) {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolidVoidBlockEntity(pos, state);
    }
}
