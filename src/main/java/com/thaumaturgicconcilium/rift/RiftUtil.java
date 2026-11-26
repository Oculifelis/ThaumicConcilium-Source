package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared helpers of the rift set: aspect colours and TC4's lightning packets as Thaumaturge bolt particles. */
public final class RiftUtil {
    /** TC4 lightning widths of 0.01 vanish as Thaumaturge bolts; this is the thinnest drawn. */
    public static final float MIN_WIDTH = 0.15F;

    private RiftUtil() {
    }

    public static int aspectColor(Level level, ResourceKey<IAspect> aspect) {
        return level.registryAccess().lookup(IAspect.REGISTRY_KEY).flatMap(r -> r.get(aspect)).map(h -> h.value().color()).orElse(0xFFFFFF);
    }

    public static List<ResourceKey<IAspect>> allAspects(Level level) {
        return level.registryAccess().lookup(IAspect.REGISTRY_KEY).map(r -> r.registryKeySet().stream().toList()).orElse(List.of());
    }

    public static Holder<IAspect> holder(HolderLookup.Provider registries, ResourceKey<IAspect> aspect) {
        return registries.lookupOrThrow(IAspect.REGISTRY_KEY).getOrThrow(aspect);
    }

    /** TC4 {@code PacketFXLightning}: a bolt of colour {@code rgb} and width {@code width} seen by players near {@code from}. */
    public static void bolt(ServerLevel level, Vec3 from, Vec3 to, int rgb, float width) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        level.sendParticles(new BoltParticleOptions(to.x, to.y, to.z, r, g, b, Math.max(MIN_WIDTH, width)), true, true, from.x, from.y, from.z, 1,
                0, 0, 0, 0);
    }

    /** TC4 {@code PacketFXBlockZap}: the pale blue-white zap. */
    public static void zapBolt(ServerLevel level, Vec3 from, Vec3 to) {
        bolt(level, from, to, 0xC8DCFF, 0.1F);
    }

    /** Adds {@code amount} of {@code aspect} into a mutable essentia map. */
    public static void add(Map<ResourceKey<IAspect>, Integer> essentia, ResourceKey<IAspect> aspect, int amount) {
        essentia.merge(aspect, amount, Integer::sum);
    }
}
