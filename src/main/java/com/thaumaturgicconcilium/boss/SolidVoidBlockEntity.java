package com.thaumaturgicconcilium.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Solid Void's render anchor: a full cube of End-portal sky on every face not hidden by a solid neighbour. */
public class SolidVoidBlockEntity extends TheEndPortalBlockEntity {
    public SolidVoidBlockEntity(BlockPos pos, BlockState state) {
        super(TCBosses.SOLID_VOID_BE.get(), pos, state);
    }

    @Override
    public boolean shouldRenderFace(Direction direction) {
        if (level == null) {
            return true;
        }
        BlockState next = level.getBlockState(worldPosition.relative(direction));
        return !next.isSolidRender() && !(next.getBlock() instanceof SolidVoidBlock);
    }
}
