package com.thaumaturgicconcilium.rift;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The Causal Bouillon (TC4 dimension {@code causalBouillonID}): a datapack dimension, a flat sheet of Void Reality in an
 * End-like sky. A player touching an interdimensional rift with an active Gem of Rifts in hand goes in, or, from inside,
 * out to the gem's current cast dimension (the Overworld with no cast) at ten times (or a tenth of, in deflate mode)
 * their coordinates.
 */
public final class CausalBouillon {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ThaumaturgicConcilium.id("causal_bouillon"));
    /** Top of the Void Reality sheet is y = 3; arrive standing on it (TC4 dropped you from y = 5). */
    private static final double ARRIVAL_Y = 4.0;

    private CausalBouillon() {
    }

    public static void travel(ServerPlayer player, ItemStack gem) {
        ServerLevel here = player.level();
        if (!here.dimension().equals(DIMENSION)) {
            ServerLevel bouillon = here.getServer().getLevel(DIMENSION);
            if (bouillon == null) {
                return;
            }
            send(player, bouillon, new Vec3(player.getX(), ARRIVAL_Y, player.getZ()));
            player.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.bouillon.enter").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        RiftData.Gem data = gem.getOrDefault(TCRifts.RIFT_GEM_DATA.get(), RiftData.Gem.EMPTY);
        ResourceKey<Level> target = data.currentCast().map(RiftData.Cast::dimension).orElse(Level.OVERWORLD);
        ServerLevel dest = here.getServer().getLevel(target);
        if (dest == null || dest == here) {
            return;
        }
        double scale = data.reverse() ? 0.1 : 10.0;
        double x = dest.getWorldBorder().clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale).getX() + 0.5;
        double z = dest.getWorldBorder().clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale).getZ() + 0.5;
        send(player, dest, new Vec3(x, safeY(dest, Mth.floor(x), Mth.floor(z)), z));
        player.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.bouillon.leave").withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void send(ServerPlayer player, ServerLevel dest, Vec3 pos) {
        // TC4 passed pitch and yaw swapped; the look is kept as it was here
        player.teleport(new TeleportTransition(dest, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
        player.resetFallDistance();
    }

    /**
     * TC4 kept the Bouillon's y (about 5) on the way out, which buried the player. Here: the first standing spot from the
     * top (below the roof in ceilinged dimensions); in a void, a small obsidian platform at sea level.
     */
    private static double safeY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 0, z);
        int top = level.dimensionType().hasCeiling() ? level.getMinY() + level.getLogicalHeight() - 2
                : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = top; y > level.getMinY(); y--) {
            pos.setY(y);
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                    && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return y;
            }
        }
        int y = Math.max(level.getSeaLevel(), level.getMinY() + 1);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = new BlockPos(x + dx, y - 1, z + dz);
                if (level.getBlockState(floor).canBeReplaced()) {
                    level.setBlockAndUpdate(floor, Blocks.OBSIDIAN.defaultBlockState());
                }
            }
        }
        return y;
    }
}
