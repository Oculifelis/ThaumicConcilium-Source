package com.thaumaturgicconcilium.spell.entity;

import com.thaumaturgicconcilium.spell.TCSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Wrath Attraction's flourish: the word "IRA" drawn in flame, mirrored about the caster's facing, two pixels a tick
 * until the picture is done (about 25 ticks). Burnout adds lava sparks once it is past pixel 20. Cosmetic only.
 */
public class WrathEffectEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BURNOUT = SynchedEntityData.defineId(WrathEffectEntity.class, EntityDataSerializers.BOOLEAN);
    /** TC4's pixel table: {row, column}, drawn mirrored. */
    static final int[][] IRA = {{0, 5}, {1, 4}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3}, {8, 3}, {9, 3}, {10, 3}, {11, 3}, {12, 4}, {13, 4}, {14, 1},
            {14, 5}, {14, 10}, {14, 12}, {15, 2}, {15, 12}, {16, 3}, {16, 13}, {17, 1}, {17, 4}, {17, 9}, {17, 14}, {18, 2}, {18, 5}, {18, 7}, {18, 9},
            {18, 13}, {18, 16}, {19, 3}, {19, 11}, {19, 14}, {20, 3}, {20, 12}, {20, 14}, {21, 3}, {21, 12}, {21, 14}, {22, 3}, {22, 12}, {22, 14}, {23, 3},
            {25, 2}, {26, 1}, {24, 3}, {23, 2}, {22, 1}};

    private int age;

    public WrathEffectEntity(EntityType<? extends WrathEffectEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static WrathEffectEntity spawn(ServerLevel level, double x, double y, double z, float yaw, boolean burnout) {
        WrathEffectEntity entity = new WrathEffectEntity(TCSpells.WRATH_EFFECT.get(), level);
        entity.setPos(x, y, z);
        entity.setYRot(yaw);
        entity.entityData.set(BURNOUT, burnout);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BURNOUT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int k = 0; k < 2; k++) {
                draw(age + k);
            }
        }
        age += 2;
        if (!level().isClientSide() && age >= IRA.length) {
            discard();
        }
    }

    private void draw(int pixel) {
        if (pixel >= IRA.length) {
            return;
        }
        int[] p = IRA[pixel];
        double rad = Math.toRadians(getYRot());
        double y = getY() + p[0] * 0.2;
        for (int side : new int[] {1, -1}) {
            double x = getX() - Math.cos(rad) * p[1] * 0.3 * side;
            double z = getZ() - Math.sin(rad) * p[1] * 0.3 * side;
            level().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
            if (pixel > 20 && entityData.get(BURNOUT)) {
                level().addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        age = in.getIntOr("age", 0);
        entityData.set(BURNOUT, in.getBooleanOr("burnout", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("age", age);
        out.putBoolean("burnout", entityData.get(BURNOUT));
    }
}
