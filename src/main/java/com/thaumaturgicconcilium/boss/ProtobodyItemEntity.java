package com.thaumaturgicconcilium.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A dropped Protobody (TC4 Concilium {@code EntityItemProtobody}): an item entity that, once it has fallen 16 blocks
 * below the bottom of the world, is torn by the Void into six Protolimbs, one of each kind, flung out around it.
 */
public class ProtobodyItemEntity extends ItemEntity {
    private static final int DEPTH = 16;

    public ProtobodyItemEntity(EntityType<? extends ProtobodyItemEntity> type, Level level) {
        super(type, level);
    }

    public ProtobodyItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(TCBosses.PROTOBODY_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || isRemoved() || getY() >= level.getMinY() - DEPTH) {
            return;
        }
        for (int i = 0; i < ProtolimbEntity.KINDS; i++) {
            ProtolimbEntity limb = TCBosses.PROTOLIMB.get().create(level, EntitySpawnReason.TRIGGERED);
            if (limb == null) {
                continue;
            }
            limb.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
            limb.setKind(i);
            Vec3 toward = new Vec3(getX() + (random.nextGaussian() - 0.5) * 2.0, getY() + 1.0, getZ() + (random.nextGaussian() - 0.5) * 2.0);
            limb.setDeltaMovement(toward.subtract(position()).normalize().scale(0.5));
            level.addFreshEntity(limb);
        }
        discard();
    }
}
