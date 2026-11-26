package com.thaumaturgicconcilium.boss.client;

import com.thaumaturgicconcilium.boss.SolidVoidBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;

/** TC4 SolidVoidTileRenderer: a full block of starfield on each face open to the air. */
public final class SolidVoidRenderer extends TheEndPortalRenderer<SolidVoidBlockEntity> {
    public SolidVoidRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float getOffsetUp() {
        return 1.0F;
    }

    @Override
    protected float getOffsetDown() {
        return 0.0F;
    }
}
