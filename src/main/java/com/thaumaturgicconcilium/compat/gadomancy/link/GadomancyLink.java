package com.thaumaturgicconcilium.compat.gadomancy.link;

import com.gadomancyawakened.aura.AuraPylonBlock;
import com.gadomancyawakened.aura.AuraPylonBlockEntity;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.thaumaturgicconcilium.compat.gadomancy.SinAuras;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Runs the sin auras (see {@link SinAuras}) on Gadomancy Aura Pylons, as TC4's {@code AuraEffect}s did: a pylon whose
 * core is up and whose base holds essentia of a sin aspect works that aspect's aura around its lowest block. The pylon
 * spends its own essentia; this only reads it. Pylon cores within 6 chunks of a player are found every second.
 */
public final class GadomancyLink {
    private static final int SCAN_PERIOD = 20;
    private static final int SCAN_CHUNKS = 6;
    private static final Map<Level, Set<BlockPos>> CORES = new WeakHashMap<>();

    private GadomancyLink() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GadomancyLink::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var registry = level.registryAccess().lookup(SinAuras.REGISTRY_KEY).orElse(null);
        if (registry == null) {
            return;
        }
        long now = level.getGameTime();
        Set<BlockPos> cores = CORES.computeIfAbsent(level, l -> new HashSet<>());
        if (now % SCAN_PERIOD == 0) {
            rescan(level, cores);
        }
        if (cores.isEmpty()) {
            return;
        }
        for (BlockPos core : cores) {
            if (!level.isLoaded(core)) {
                continue;
            }
            BlockState state = level.getBlockState(core);
            if (!(state.getBlock() instanceof AuraPylonBlock) || state.getValue(AuraPylonBlock.PART) != AuraPylonBlock.Part.CORE) {
                continue;
            }
            BlockPos base = core.below(2);
            if (!(level.getBlockEntity(base) instanceof AuraPylonBlockEntity pylon) || pylon.getEssentiaAmount(Direction.UP) <= 0) {
                continue;
            }
            Holder<IAspect> aspect = pylon.aspect();
            if (aspect == null) {
                continue;
            }
            ResourceKey<IAspect> key = aspect.unwrapKey().orElse(null);
            registry.listElements().forEach(ref -> {
                SinAuras aura = ref.value();
                if (aura.aspect().equals(key) && now % aura.interval() == 0) {
                    aura.apply(level, core.below());
                }
            });
        }
    }

    private static void rescan(ServerLevel level, Set<BlockPos> cores) {
        cores.clear();
        Set<Long> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            int pcx = SectionPos.blockToSectionCoord(player.getBlockX());
            int pcz = SectionPos.blockToSectionCoord(player.getBlockZ());
            for (int cx = pcx - SCAN_CHUNKS; cx <= pcx + SCAN_CHUNKS; cx++) {
                for (int cz = pcz - SCAN_CHUNKS; cz <= pcz + SCAN_CHUNKS; cz++) {
                    if (!seen.add(((long) cx << 32) ^ (cz & 0xFFFFFFFFL))) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof AuraPylonBlockEntity && be.getBlockState().getBlock() instanceof AuraPylonBlock
                                && be.getBlockState().getValue(AuraPylonBlock.PART) == AuraPylonBlock.Part.CORE) {
                            cores.add(be.getBlockPos().immutable());
                        }
                    }
                }
            }
        }
    }
}
