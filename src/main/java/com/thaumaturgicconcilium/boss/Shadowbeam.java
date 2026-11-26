package com.thaumaturgicconcilium.boss;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Brightest One's shadow beam (TC4 Concilium {@code Shadowbeam}, taken from Thaumic Tinkerer's Focus of Shadows):
 * a beam that is simulated to its end in one go, 300 steps of 0.15 blocks from the caster's eyes along its look,
 * bouncing off blocks like light off a mirror. Every living thing it passes through (not the caster) takes magic
 * damage of 8 + potency. Dark sparkles mark the path.
 */
public final class Shadowbeam {
    private static final int OFFSET = 2;
    private static final int LENGTH = 298;
    private static final double STEP = 0.15;
    private static final DustParticleOptions SPARKLE = new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0x2A0A3A).toVector3f(), 1.5F);

    private Shadowbeam() {
    }

    public static void fire(ServerLevel level, LivingEntity caster, int potency) {
        Vec3 pos = caster.getEyePosition();
        Vec3 motion = caster.getLookAngle().scale(STEP);
        Set<Entity> hit = new HashSet<>();
        for (int tick = 0; tick < OFFSET + LENGTH; tick++) {
            Vec3 next = pos.add(motion);
            BlockHitResult block = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            AABB box = new AABB(pos, next).inflate(0.15);
            for (Entity e : level.getEntities(caster, box, e -> e instanceof LivingEntity && e.isAlive())) {
                if (hit.add(e)) {
                    e.hurt(level.damageSources().indirectMagic(caster, caster), 8.0F + potency);
                }
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                Direction side = block.getDirection();
                Vec3 n = new Vec3(side.getStepX(), side.getStepY(), side.getStepZ());
                motion = motion.subtract(n.scale(2.0 * motion.dot(n)));
                pos = block.getLocation().add(motion.scale(0.01));
            } else {
                pos = next;
            }
            if (tick > OFFSET && tick % 3 == 0) {
                level.sendParticles(SPARKLE, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
}
