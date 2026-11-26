package com.thaumaturgicconcilium.crimson;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The original's Samurai / Crimson Archer / Crimson Ranger attack behavior in one goal. The mob always faces its target. In
 * melee mode it drives straight at the target and strikes from two blocks; otherwise it keeps its distance (backing off
 * and strafing inside 85% of its range) and every {@code cooldown} ticks, with sight and in range, makes its ranged
 * attack. It gives up when the target is more than sqrt(2.5) ranges away.
 *
 * <p>Port fix: past 85% of its range the original kept pathing six blocks further away even when the target was out
 * of range, so a kiting mob ran off until it lost its target. Here it holds position inside its range and walks back
 * towards the target when it is out of range.
 */
public class KitingAttackGoal extends Goal {
    /** A mob that uses this goal. */
    public interface KitingMob {
        /** True to fight in melee (the Samurai above a third of its health, an Archer without a bow). */
        boolean meleeMode();

        /** The ranged attack, made every cooldown with sight and in range. */
        void kiteAttack(ServerLevel level, LivingEntity target);

        /** Called each tick of melee mode before moving (the Samurai's speed bursts). */
        default void meleeTick(LivingEntity target, double horizontalDistSq) {
        }
    }

    private static final double DRIVE_SPEED = 0.25;
    private final PathfinderMob mob;
    private final KitingMob kiter;
    private final int cooldown;
    private final float range;
    private @Nullable LivingEntity target;
    private int attackTime;
    private int seeTime;
    private int meleeCooldown;
    private int strafeTime;
    private int strafeDirection = 1;

    public <M extends PathfinderMob & KitingMob> KitingAttackGoal(M mob, int cooldown, float range) {
        this.mob = mob;
        this.kiter = mob;
        this.cooldown = cooldown;
        this.range = range;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive()) {
            return false;
        }
        target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive() && mob.distanceToSqr(t) <= range * range * 2.5;
    }

    @Override
    public void start() {
        seeTime = 0;
        attackTime = 0;
        meleeCooldown = 0;
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        target = null;
        attackTime = 0;
        mob.getNavigation().stop();
        mob.setAggressive(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = target;
        if (t == null || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        float yaw = (float) (Mth.atan2(t.getZ() - mob.getZ(), t.getX() - mob.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        yaw = Mth.wrapDegrees(yaw);
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.getLookControl().setLookAt(t, 30.0F, 30.0F);

        double dx = mob.getX() - t.getX();
        double dy = mob.getY() - t.getY();
        double dz = mob.getZ() - t.getZ();
        double distSq = dx * dx + dz * dz;
        double len = Math.max(Math.sqrt(distSq), 1.0E-4);
        double nx = dx / len;
        double nz = dz / len;
        seeTime = mob.getSensing().hasLineOfSight(t) ? seeTime + 1 : 0;

        if (kiter.meleeMode()) {
            kiter.meleeTick(t, distSq);
            drive(-nx * DRIVE_SPEED, -nz * DRIVE_SPEED);
            mob.getNavigation().moveTo(t, 1.0);
            if (meleeCooldown > 0) {
                meleeCooldown--;
            }
            if (distSq <= 4 && dy * dy <= 4 && meleeCooldown <= 0) {
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(level, t);
                meleeCooldown = 20;
            }
            return;
        }

        double maxSq = range * range;
        if (distSq < maxSq * 0.85) {
            if (strafeTime <= 0) {
                strafeTime = 40;
                strafeDirection = mob.getRandom().nextBoolean() ? 1 : -1;
            } else {
                strafeTime--;
            }
            double sx = -nz;
            double sz = nx;
            drive(nx * DRIVE_SPEED + sx * 0.1 * strafeDirection, nz * DRIVE_SPEED + sz * 0.1 * strafeDirection);
            mob.getNavigation().stop();
        } else if (distSq > maxSq) {
            mob.getNavigation().moveTo(t, 1.0);
        } else {
            mob.getNavigation().stop();
        }

        if (attackTime < cooldown) {
            attackTime++;
        }
        if (seeTime > 0 && distSq <= maxSq && attackTime >= cooldown) {
            kiter.kiteAttack(level, t);
            attackTime = 0;
        }
    }

    /** The original set the mob's horizontal motion straight to this each tick (and its vertical motion to 0 on the ground). */
    private void drive(double x, double z) {
        Vec3 v = mob.getDeltaMovement();
        mob.setDeltaMovement(x, mob.onGround() ? Math.min(v.y, 0.0) : v.y, z);
    }
}
