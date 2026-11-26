package com.thaumaturgicconcilium.spell.entity;

import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Alchemist's Spree's shimmer: a cloud of distortion that hangs in front of its owner while the spell keeps pulsing
 * and fades a second after the last pulse. Cosmetic only (TC4 drew it with a shader; this is a particle cloud).
 */
public class DistortionEffectEntity extends Entity {
    private static final int FADE = 20;
    private Optional<UUID> owner = Optional.empty();
    private long lastRefresh;

    public DistortionEffectEntity(EntityType<? extends DistortionEffectEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static DistortionEffectEntity spawn(ServerLevel level, Entity owner) {
        DistortionEffectEntity entity = new DistortionEffectEntity(TCSpells.DISTORTION_EFFECT.get(), level);
        entity.owner = Optional.of(owner.getUUID());
        entity.place(owner);
        entity.lastRefresh = level.getGameTime();
        level.addFreshEntity(entity);
        return entity;
    }

    /** Keeps the cloud alive and in front of its owner for another second. */
    public void refresh(Entity owner) {
        lastRefresh = level().getGameTime();
        place(owner);
    }

    public boolean ownedBy(Entity entity) {
        return owner.map(id -> id.equals(entity.getUUID())).orElse(false);
    }

    private void place(Entity owner) {
        Vec3 at = owner.getEyePosition().add(owner.getLookAngle().scale(1.5)).subtract(0, 0.5, 0);
        setPos(at.x, at.y, at.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int k = 0; k < 3; k++) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 1.5,
                        getZ() + (random.nextDouble() - 0.5) * 2, 0, 0, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.BUBBLE_POP, getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble(), getZ() + (random.nextDouble() - 0.5),
                        0, 0.02, 0);
            }
            return;
        }
        if (level().getGameTime() - lastRefresh > FADE || owner.isEmpty()) {
            discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        owner = in.read("owner", UUIDUtil.CODEC);
        lastRefresh = 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        owner.ifPresent(id -> out.store("owner", UUIDUtil.CODEC, id));
    }
}
