package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.content.effect.WispEffects;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.MobUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Protolimb (TC4 Concilium {@code Protolimb}): one of the six pieces (head, body, two arms, two legs) the Void tears
 * a Protobody into. It drifts about on its own and shies from the nearest player within 16, keeping 4 blocks away.
 * Touching one absorbs it: the player's protolimb count goes up, and at six Solid Void is learnt.
 */
public class ProtolimbEntity extends Mob {
    public static final int KINDS = 6;
    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(ProtolimbEntity.class, EntityDataSerializers.BYTE);
    private static final Identifier SOLID_VOID = ThaumaturgicConcilium.id("solid_void");

    public int innerRotation;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean targetSet;
    private @Nullable Player shy;

    public ProtolimbEntity(EntityType<? extends ProtolimbEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        innerRotation = random.nextInt(100000);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, (byte) 0);
    }

    /** TC4 order: 0 head, 1 left arm, 2 right arm, 3 body, 4 left leg, 5 right leg. */
    public int kind() {
        return entityData.get(KIND);
    }

    public void setKind(int kind) {
        entityData.set(KIND, (byte) Math.floorMod(kind, KINDS));
    }

    @Override
    public void tick() {
        super.tick();
        innerRotation++;
    }

    /**
     * TC4 started every limb heading for world (0, 0, 0) until its first random retarget; the port starts from where
     * the limb is.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!targetSet) {
            targetX = getX();
            targetY = getY();
            targetZ = getZ();
            targetSet = true;
        }
        if (shy == null || !shy.isAlive()) {
            shy = level.getNearestPlayer(this, 16.0);
        }
        if (shy != null) {
            double dx = getX() - shy.getX();
            double dz = getZ() - shy.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 4.0 && dist > 0.0) {
                targetX += dx / dist * 4.0;
                targetZ += dz / dist * 4.0;
                targetY += (getY() - shy.getY()) * 0.5;
            }
        }
        double dx = targetX - getX();
        double dy = targetY - getY();
        double dz = targetZ - getZ();
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x + (Math.signum(dx) * 0.1 - v.x) * 0.1, v.y + (Math.signum(dy) * 0.1 - v.y) * 0.1, v.z + (Math.signum(dz) * 0.1 - v.z) * 0.1);
        if (random.nextInt(100) == 0 || (Math.abs(dx) < 1.0 && Math.abs(dy) < 1.0 && Math.abs(dz) < 1.0)) {
            targetX = getX() + (random.nextDouble() - 0.5) * 16.0;
            targetY = getY() + (random.nextDouble() - 0.5) * 8.0;
            targetZ = getZ() + (random.nextDouble() - 0.5) * 16.0;
        }
    }

    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91F));
    }

    @Override
    public void playerTouch(Player player) {
        if (!(level() instanceof ServerLevel level) || isRemoved() || !(player instanceof ServerPlayer server)) {
            return;
        }
        int limbs = Ethereal.protolimbs(server) + 1;
        Ethereal.setProtolimbs(server, limbs);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.CRAFTFAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (limbs == KINDS) {
            MobUtil.unlockResearch(server, SOLID_VOID);
        }
        level.sendParticles(WispEffects.burst(2.0F), getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putByte("Type", (byte) kind());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setKind(in.getByteOr("Type", (byte) 0));
    }
}
