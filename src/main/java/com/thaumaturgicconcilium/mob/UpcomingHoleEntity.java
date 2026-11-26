package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import com.leclowndu93150.thaumaturge.content.spell.block.PortableHoles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;

/**
 * The Dissolved's trap (TC4 {@code UpcomingHoleEntity}): 30 ticks of sparkles, then a portable hole one block around
 * and 33 deep under the spot, closing after 120 ticks. Uses Thaumaturge's own portable hole (the Rift spell's), so
 * block entities and unbreakable blocks are left alone and every block comes back.
 */
public class UpcomingHoleEntity extends Entity {
    private static final int WARNING = 30;
    private static final int DEPTH = 33;
    private static final int DURATION = 120;

    public UpcomingHoleEntity(EntityType<? extends UpcomingHoleEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(new SparkleParticleOptions(0xFF4040FF, 0.8F, 0, 1.0F, 0.0F, 6, false), getX(), getY() + 0.1, getZ(), 3, 0.6, 0.05,
                0.6, 0.0);
        if (tickCount >= WARNING) {
            BlockPos floor = BlockPos.containing(getX(), getY() - 0.5, getZ());
            PortableHoles.open(level, floor, Direction.UP, DEPTH, DURATION);
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, damage);
        }
        return false;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
    }
}
