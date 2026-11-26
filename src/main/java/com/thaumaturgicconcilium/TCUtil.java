package com.thaumaturgicconcilium;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Lookups of Thaumaturge content by id (so a missing entry never crashes a world) and shared world helpers. */
public final class TCUtil {
    private TCUtil() {
    }

    public static Optional<Block> block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id));
    }

    public static Optional<Holder.Reference<MobEffect>> effect(String id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id));
    }

    /** Gives a mob effect by id, if that effect exists. */
    public static void giveEffect(LivingEntity target, String id, int duration, int amplifier) {
        effect(id).ifPresent(e -> target.addEffect(new MobEffectInstance(e, duration, amplifier)));
    }

    /**
     * Taints ten random columns within {@code radius}: taint fibres on open ground, flux gas or flux goo above (the
     * original's taint splash, used by the Bottle of Thick Taint, collapsing rifts and the Peeler of Matter).
     */
    public static void taintArea(ServerLevel level, BlockPos center, int radius) {
        RandomSource random = level.getRandom();
        Optional<Block> fibre = block("thaumaturge:taint_fibre");
        Optional<Block> gas = block("thaumaturge:flux_gas");
        Optional<Block> goo = block("thaumaturge:flux_goo");
        for (int i = 0; i < 10; i++) {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, center.getY(), z));
            if (Math.abs(top.getY() - center.getY()) > radius + 2) {
                continue;
            }
            BlockState here = level.getBlockState(top);
            if (!here.canBeReplaced()) {
                continue;
            }
            Optional<Block> pick = switch (random.nextInt(3)) {
                case 0 -> fibre;
                case 1 -> gas;
                default -> goo;
            };
            pick.ifPresent(b -> level.setBlockAndUpdate(top, b.defaultBlockState()));
        }
    }
}
