package com.thaumaturgicconcilium.compat.horizons;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Red-Powered Mind, the creature (TC4 Concilium {@code RedPoweredMind}): what a Mad Thaumaturge becomes in a Thaumic
 * Horizons vat with Automagy's redstone parts. Freed into open air it settles at once into the Red-Powered Mind block;
 * anywhere else (inside a block) it withers, 1 health a tick. While held without behavior (still in the vat, or posed with
 * {@code NoAI}) it simply stays a creature: TC4 had no such state.
 */
public class RedPoweredMindEntity extends Mob {
    public RedPoweredMindEntity(EntityType<? extends RedPoweredMindEntity> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || isNoAi() || !isAlive()) {
            return;
        }
        BlockPos pos = blockPosition();
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, HorizonsContent.RED_POWERED_MIND_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            discard();
        } else {
            hurtServer(level, damageSources().inWall(), 1.0F);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
