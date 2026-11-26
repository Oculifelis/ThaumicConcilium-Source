package com.thaumaturgicconcilium;

import com.thaumaturgicconcilium.boss.LesserPortalEntity;
import com.thaumaturgicconcilium.boss.TCBosses;
import com.thaumaturgicconcilium.rift.LithographerBlockEntity;
import com.thaumaturgicconcilium.rift.TCRifts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Save/load round trips of the Lesser Portal waves and the Lithographer projection, run once the dev server has started
 * when the JVM has {@code -Dthaumaturgicconcilium.selftest=true} (only the Gradle server run sets it). Logs one
 * {@code SELFTEST PASS} or {@code SELFTEST FAIL} line per check. (The Sloppy Alchemist ram goal is transient behaviour state and
 * is not saved, so it has no round trip.)
 */
final class SelfTest {
    private SelfTest() {
    }

    static void register() {
        if (Boolean.getBoolean("thaumaturgicconcilium.selftest")) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> run(e.getServer().overworld()));
        }
    }

    private static void check(String name, boolean ok, String detail) {
        ThaumaturgicConcilium.LOG.info("SELFTEST {} {}: {}", ok ? "PASS" : "FAIL", name, detail);
    }

    private static void portal(ServerLevel level, String name, int stage, int counter, boolean boss, LesserPortalEntity.Wave expect) {
        LesserPortalEntity portal = TCBosses.LESSER_PORTAL.get().create(level);
        LesserPortalEntity loaded = TCBosses.LESSER_PORTAL.get().create(level);
        if (portal == null || loaded == null) {
            check(name, false, "could not create the portal");
            return;
        }
        portal.selfTestSet(stage, counter, boss);
        CompoundTag tag = portal.saveWithoutId(new CompoundTag());
        loaded.load(tag);
        check(name, loaded.wave() == expect && portal.snapshot().equals(loaded.snapshot()), loaded.snapshot());
    }

    private static void lithographer(ServerLevel level, String name, LithographerBlockEntity.Projection p, int ticks) {
        BlockPos pos = new BlockPos(0, 64, 0);
        LithographerBlockEntity be = new LithographerBlockEntity(pos, TCRifts.LITHOGRAPHER.get().defaultBlockState());
        be.selfTestSet(p, ticks);
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        LithographerBlockEntity loaded = new LithographerBlockEntity(pos, TCRifts.LITHOGRAPHER.get().defaultBlockState());
        loaded.loadWithComponents(tag, level.registryAccess());
        check(name, loaded.projection() == p && be.snapshot().equals(loaded.snapshot()), loaded.snapshot());
    }

    /** The shipped Lithographer cost, or the value the test datapack in tools/selftest_datapack sets. */
    private static void buildCost() {
        java.util.Map<?, Integer> cost = TCDataMaps.wandBuildCost(TCRifts.LITHOGRAPHER.get());
        Integer aqua = cost.get(com.leclowndu93150.thaumaturge.api.aspect.TTAspects.AQUA);
        int hex = TCDataMaps.wandBuildCost(TCRifts.HEX.get()).size();
        if (aqua != null && aqua == 77) {
            check("build_cost_override", cost.size() == 1 && hex == 2, "lithographer " + cost + " hex aspects=" + hex);
        } else {
            check("build_cost_default", aqua != null && aqua == 50 && cost.size() == 4 && hex == 2,
                    "lithographer " + cost + " hex aspects=" + hex + " (test pack not loaded)");
        }
    }

    private static void run(ServerLevel level) {
        try {
            portal(level, "portal_roundtrip_fresh", 0, 200, false, LesserPortalEntity.Wave.RUSH);
            portal(level, "portal_roundtrip_steady", 7, 37, false, LesserPortalEntity.Wave.STEADY);
            portal(level, "portal_roundtrip_boss_pause", 12, 113, true, LesserPortalEntity.Wave.PAUSE);
            portal(level, "portal_roundtrip_drain", 16, 9, false, LesserPortalEntity.Wave.DRAIN);
            lithographer(level, "lithographer_roundtrip_idle", LithographerBlockEntity.Projection.IDLE, 0);
            lithographer(level, "lithographer_roundtrip_raising", LithographerBlockEntity.Projection.RAISING, 7);
            lithographer(level, "lithographer_roundtrip_imprinting", LithographerBlockEntity.Projection.IMPRINTING, 15);
            // project() mid-raise used to restart with the plates half down; it must be ignored now.
            LithographerBlockEntity busy = new LithographerBlockEntity(new BlockPos(0, 64, 0), TCRifts.LITHOGRAPHER.get().defaultBlockState());
            busy.selfTestSet(LithographerBlockEntity.Projection.RAISING, 7);
            busy.project();
            check("lithographer_project_ignored_mid_raise", busy.projection() == LithographerBlockEntity.Projection.RAISING
                    && busy.deploy() == -7, busy.snapshot());
            buildCost();
        } catch (Exception ex) {
            ThaumaturgicConcilium.LOG.error("SELFTEST FAIL crashed", ex);
        }
    }
}
