package com.thaumaturgicconcilium.crimson;

import com.leclowndu93150.thaumaturge.content.equipment.runic.RunicShieldState;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.thaumaturgicconcilium.spell.entity.GlowingOrb;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Crimson Orb, cast by the Crimson Paladin: flies straight along the caster's aim for 50 ticks, turns back like a
 * boomerang and is gone at 100. Everything living it passes through (except the caster and crimson cultists) takes 12
 * indirect magic damage and loses its absorption; a player also loses the Thaumaturge runic shield charge, which then
 * waits a full recharge cycle before it starts to refill.
 *
 * <p>Port fixes: the original stopped checking the whole hit list at the first cultist (so a cultist next to you
 * shielded you), and did nothing at all after a reload (its caster was not saved). Drawn as a crimson glow.
 */
public class CrimsonOrbEntity extends Entity implements GlowingOrb {
    private static final int TURN_AGE = 50;
    private static final int RUNIC_LOCKOUT = 100;

    private int age;
    private int lifetime = 100;
    private int power = 2;
    private @Nullable UUID casterId;

    public CrimsonOrbEntity(EntityType<? extends CrimsonOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public CrimsonOrbEntity(Level level, double x, double y, double z, Vec3 velocity, LivingEntity caster) {
        this(TCCrimson.CRIMSON_ORB.get(), level);
        setPos(x, y, z);
        setDeltaMovement(velocity);
        casterId = caster.getUUID();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public int glowColor() {
        return 0xC4122F;
    }

    @Override
    public float glowScale() {
        return 0.5F;
    }

    @Override
    public void tick() {
        super.tick();
        move(MoverType.SELF, getDeltaMovement());
        age++;
        if (!(level() instanceof ServerLevel level)) {
            if (random.nextInt(2) == 0) {
                level().addParticle(new DustParticleOptions(0xC4122F, 0.8F), getX(), getY() + 0.1, getZ(), 0, 0, 0);
            }
            return;
        }
        Entity caster = casterId == null ? null : level.getEntity(casterId);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox())) {
            if (e == caster || e.getUUID().equals(casterId) || TCCrimson.isCultist(e)) {
                continue;
            }
            e.hurtServer(level, damageSources().indirectMagic(this, caster), 10 + power);
            e.setAbsorptionAmount(0.0F);
            if (e instanceof ServerPlayer player) {
                RunicShieldState state = player.getData(TTAttachments.RUNIC_SHIELD);
                state.lastCharge = 0;
                state.nextCycle = level.getGameTime() + RUNIC_LOCKOUT;
            }
        }
        if (age == TURN_AGE) {
            setDeltaMovement(getDeltaMovement().scale(-1.0));
            hurtMarked = true;
        }
        if (age > lifetime) {
            discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        age = in.getIntOr("Life", 0);
        lifetime = in.getIntOr("LifeTime", 100);
        power = in.getIntOr("power", 2);
        casterId = in.read("caster", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("Life", age);
        out.putInt("LifeTime", lifetime);
        out.putInt("power", power);
        if (casterId != null) {
            out.store("caster", UUIDUtil.CODEC, casterId);
        }
    }
}
