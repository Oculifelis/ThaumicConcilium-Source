package com.thaumaturgicconcilium.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * Development-only automated client check, inert unless the JVM runs with -Dthaumaturgicconcilium.autotest (only the Gradle dev run
 * passes it with -Pautotest, see build.gradle and tools/run_client_test.sh). It makes a fresh creative superflat world
 * "tctest", sets out tinted Destabilized Crystals and summons every new entity (checks the client renderers), takes
 * one screenshot, logs "TCTEST" lines and stops the client.
 */
@EventBusSubscriber(modid = ThaumaturgicConcilium.MODID, value = Dist.CLIENT)
public final class TCClientAutotest {
    private static final String MODE = System.getProperty("thaumaturgicconcilium.autotest");
    private static final Logger LOG = LogUtils.getLogger();
    private static final String WORLD = "tctest";
    /** Gallery mode: -Pautotest=gallery, or gallery:<id part>,<id part> for only the matching subjects. */
    private static final boolean GALLERY = MODE != null && MODE.startsWith("gallery");
    private static final java.util.List<String> GALLERY_ONLY = MODE != null && MODE.startsWith("gallery:")
        ? java.util.List.of(MODE.substring(8).split(",")) : java.util.List.of();
    /** Ticks after the scene is built before the first subject, so the spectator camera has settled. */
    private static final int WARMUP = 40;
    /** Held mode: -Pautotest=held or held:<item id> (default the Drainage Syringe). */
    private static final boolean SYRINGE = MODE != null && MODE.startsWith("held");
    private static final String HELD = MODE != null && MODE.startsWith("held:") ? MODE.substring(5) : "thaumaturgicconcilium:drainage_syringe";
    private static final long TIMEOUT_NANOS = (GALLERY ? 9L : 4L) * 60L * 1_000_000_000L;
    /** Gallery mode: ticks per subject, and the tick within that slot the shot is taken. */
    private static final int SLOT = 20;
    private static final int SHOT_AT = 15;
    private static java.util.List<String[]> gallery;

    private static boolean titleSeen;
    private static boolean creating;
    private static long titleAt;
    private static int idleTicks;
    private static int worldTicks;
    private static boolean finished;

    private TCClientAutotest() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (MODE == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || finished) {
            return;
        }
        if (titleSeen && System.nanoTime() - titleAt > TIMEOUT_NANOS) {
            LOG.info("TCTEST timeout");
            finish(mc);
            return;
        }
        try {
            step(mc);
        } catch (Throwable t) {
            LOG.error("TCTEST step failed", t);
        }
    }

    private static void step(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            if (!creating && mc.screen != null && !(mc.screen instanceof TitleScreen) && mc.getOverlay() == null && ++idleTicks > 200) {
                LOG.info("TCTEST dismissing {}", mc.screen.getClass().getName());
                mc.setScreen(new TitleScreen());
                idleTicks = 0;
            }
            if (!creating && mc.screen instanceof TitleScreen title && mc.getOverlay() == null) {
                creating = true;
                titleSeen = true;
                titleAt = System.nanoTime();
                LOG.info("TCTEST title");
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
                mc.options.framerateLimit().set(30);
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(4);
                deleteOldWorld(mc);
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, net.minecraft.world.Difficulty.NORMAL, true, new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);
                LOG.info("TCTEST creating world {}", WORLD);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261101L, false, false), ra -> ra.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), title);
            }
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        worldTicks++;
        if (worldTicks == 1) {
            LOG.info("TCTEST joined");
        } else if (GALLERY) {
            gallery(mc);
        } else if (SYRINGE) {
            syringe(mc);
        } else if (worldTicks == 60) {
            build(mc);
        } else if (worldTicks == 160) {
            shot(mc);
        } else if (worldTicks == 180) {
            mobs(mc);
        } else if (worldTicks == 280) {
            shot(mc);
        } else if (worldTicks == 300) {
            scene(mc, RIFT_SCENE);
        } else if (worldTicks == 420) {
            shot(mc);
        } else if (worldTicks == 440) {
            scene(mc, BOSS_SCENE);
        } else if (worldTicks == 540) {
            shot(mc);
        } else if (worldTicks == 560 && net.neoforged.fml.ModList.get().isLoaded("witchery")) {
            scene(mc, WITCHERY_SCENE, "tp @a 7.5 -57 -12 0 15");
        } else if (worldTicks == 660 && net.neoforged.fml.ModList.get().isLoaded("witchery")) {
            shot(mc);
        } else if (worldTicks == 680 && net.neoforged.fml.ModList.get().isLoaded("thaumichorizons")) {
            scene(mc, THAUMIC_COMPAT_SCENE, "tp @a 7.5 -57 10.5 180 15");
        } else if (worldTicks == 780 && net.neoforged.fml.ModList.get().isLoaded("thaumichorizons")) {
            shot(mc);
        } else if (worldTicks == 820) {
            scene(mc, LIMB_SCENE, "tp @a 0.5 -59 7.5 180 15");
        } else if (worldTicks == 824) {
            command(mc, "kill @e[type=thaumaturgicconcilium:overanimated]");
        } else if (worldTicks == 836) {
            IntegratedServer server = mc.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> LOG.info("TCTEST limbs {} (6 frozen + 12 from deaths expected)",
                        server.overworld().getEntities(com.thaumaturgicconcilium.mob.TCMobs.ALIVE_LIMB.get(), e -> true).size()));
            }
            shot(mc);
        } else if (worldTicks == 860) {
            LOG.info("TCTEST done");
            finish(mc);
        }
    }

    /**
     * Gallery mode (-Pautotest=gallery): one screenshot per block (placed), item (as its inventory icon) and entity
     * of this mod, named tcgal_<kind>_<id>.png, for the art review page.
     */
    private static void gallery(Minecraft mc) {
        if (worldTicks < 60) {
            return;
        }
        if (gallery == null) {
            build(mc);
            gallery = new java.util.ArrayList<>();
            String ns = ThaumaturgicConcilium.MODID;
            for (var id : net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet()) {
                if (id.getNamespace().equals(ns)) {
                    gallery.add(new String[] {"block", id.getPath(), "setblock 0 -60 0 " + id,
                        camera(1.75, -58.75, 1.75, 0.5, -59.5, 0.5)});
                }
            }
            for (var id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                if (id.getNamespace().equals(ns) && !(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)
                        instanceof net.minecraft.world.item.BlockItem)) {
                    gallery.add(new String[] {"item", id.getPath(),
                        "summon minecraft:item_display 0.5 -59 0.5 {item:{id:\"" + id + "\"},item_display:\"gui\",billboard:\"center\"}",
                        camera(0.5, -59.0, 1.6, 0.5, -59.0, 0.5)});
                }
            }
            for (var id : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
                if (id.getNamespace().equals(ns)) {
                    var dims = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id).getDimensions();
                    double size = Math.max(dims.height(), dims.width());
                    double d = 1.0 + size * 1.4;
                    double y = -60 + Math.max(dims.height(), 0.2) / 2.0;
                    gallery.add(new String[] {"entity", id.getPath(),
                        "summon " + id + " 0.5 -60 0.5 {NoAI:1b,NoGravity:1b,Silent:1b,Rotation:[0f,0f]}",
                        camera(0.5, y + size * 0.15, 0.5 + d, 0.5, y, 0.5)});
                }
            }
            if (!GALLERY_ONLY.isEmpty()) {
                gallery.removeIf(g -> GALLERY_ONLY.stream().noneMatch(g[1]::contains));
            }
            LOG.info("TCTEST gallery {} subjects", gallery.size());
            IntegratedServer server = mc.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a"));
            }
        }
        if (worldTicks < 60 + WARMUP) {
            return;
        }
        int i = (worldTicks - 60 - WARMUP) / SLOT;
        int at = (worldTicks - 60 - WARMUP) % SLOT;
        if (i >= gallery.size()) {
            LOG.info("TCTEST done");
            finish(mc);
            return;
        }
        String[] subject = gallery.get(i);
        if (at == 0) {
            IntegratedServer server = mc.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    for (String c : new String[] {"kill @e[type=!player]", "fill -6 -60 -6 6 -54 6 minecraft:air", subject[2], subject[3]}) {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    }
                    LOG.info("TCTEST gallery {} {}", subject[0], subject[1]);
                });
            }
        } else if (at == SHOT_AT) {
            String name = "tcgal_" + subject[0] + "_" + subject[1] + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> { });
        }
    }

    /**
     * Held mode (-Pautotest=held[:item]): one item in every display context, shots tcheld_<item>_<context>.png:
     * first person in the main hand next to a vanilla brush in the off hand for size (with the hotbar), third person from the front, the GUI icon large, on the ground and
     * in an item frame.
     */
    private static void syringe(Minecraft mc) {
        String item = HELD;
        switch (worldTicks) {
            case 60 -> {
                build(mc);
                mc.options.hideGui = false;
                run(mc, "kill @e[type=!player]", "fill -6 -60 -6 6 -54 6 minecraft:air", "clear @a",
                    "item replace entity @a weapon.mainhand with " + item, "item replace entity @a weapon.offhand with minecraft:brush",
                    "tp @a 0.5 -60 0.5 180 10");
            }
            case 100 -> syringeShot(mc, "firstperson");
            case 110 -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            case 150 -> syringeShot(mc, "thirdperson");
            case 160 -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.options.hideGui = true;
                run(mc, "clear @a", "gamemode spectator @a",
                    "summon minecraft:item_display 0.5 -59 0.5 {item:{id:\"" + item + "\"},item_display:\"gui\",billboard:\"center\"}",
                    camera(0.5, -59.0, 1.6, 0.5, -59.0, 0.5));
            }
            case 190 -> syringeShot(mc, "gui");
            case 200 -> run(mc, "kill @e[type=!player]", "setblock 0 -60 -1 minecraft:stone", "setblock 0 -59 -1 minecraft:stone",
                "summon minecraft:item_frame 0.5 -59 0.03 {Facing:3b,Item:{id:\"" + item + "\",count:1},Fixed:1b}",
                "summon minecraft:item 1.5 -60 0.5 {Item:{id:\"" + item + "\",count:1},PickupDelay:32767,Age:-32768}",
                camera(1.0, -59.0, 2.0, 1.0, -59.5, 0.0));
            case 240 -> syringeShot(mc, "world");
            case 260 -> {
                LOG.info("TCTEST done");
                finish(mc);
            }
            default -> { }
        }
    }

    private static void syringeShot(Minecraft mc, String context) {
        Screenshot.grab(mc.gameDirectory, "tcheld_" + HELD.substring(HELD.indexOf(':') + 1) + "_" + context + ".png", mc.getMainRenderTarget(), msg -> { });
        LOG.info("TCTEST syringe shot {}", context);
    }

    private static void run(Minecraft mc, String... commands) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                for (String c : commands) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
            });
        }
    }

    /** A tp that puts a spectator's eye at (ex, ey, ez) looking at (tx, ty, tz). */
    private static String camera(double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex;
        double dy = ty - ey;
        double dz = tz - ez;
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return String.format(java.util.Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", ex, ey - 1.62, ez, yaw, pitch);
    }

    private static void deleteOldWorld(Minecraft mc) {
        LevelStorageSource source = mc.getLevelSource();
        if (!source.levelExists(WORLD)) {
            return;
        }
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
            access.deleteLevel();
            LOG.info("TCTEST deleted old world {}", WORLD);
        } catch (Exception e) {
            LOG.warn("TCTEST could not delete old world {}", WORLD, e);
        }
    }

    /**
     * Superflat: grass at y=-61, the player stands at y=-60. A row of Destabilized Crystals (empty and filled with
     * several aspects, so the tint shows) at z=0, and the new entities summoned above it in a row at z=1.
     */
    private static void build(Minecraft mc) {
        mc.options.hideGui = true;
        mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
        java.util.List<String> commands = new java.util.ArrayList<>(java.util.List.of(
            "time set noon", "weather clear", "gamerule advance_time false", "gamerule advance_weather false",
            "gamerule spawn_mobs false", "fill -10 -60 -4 10 -52 8 minecraft:air"));
        String[] aspects = {"", "ignis", "aqua", "terra", "aer", "ordo", "perditio", "vitium"};
        for (int i = 0; i < aspects.length; i++) {
            String nbt = aspects[i].isEmpty() ? "" : "{aspect:\"thaumaturge:" + aspects[i] + "\",capacity:5}";
            commands.add("setblock " + (i - 4) + " -60 0 thaumaturgicconcilium:destabilized_crystal" + nbt);
        }
        String[] entities = {"compressed_blast", "positive_burst_orb", "charged_wisp", "wrath_effect", "distortion_effect",
            "concentrated_warp_charge", "shard_powder", "thrown_bottle"};
        for (int i = 0; i < entities.length; i++) {
            commands.add("summon thaumaturgicconcilium:" + entities[i] + " " + (i - 4 + 0.5) + " -58.5 1.5 {NoGravity:1b}");
        }
        commands.add("tp @a 0.5 -60 6.5 180 8");
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            LOG.error("TCTEST no integrated server");
            return;
        }
        server.execute(() -> {
            for (String c : commands) {
                LOG.info("TCTEST /{}", c);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
            }
            LOG.info("TCTEST blocks placed");
        });
    }

    /** Second scene: the phase 2 mobs in two rows, frozen (NoAI) so they hold still for the shot. */
    private static void mobs(Minecraft mc) {
        java.util.List<String> commands = new java.util.ArrayList<>(java.util.List.of(
            "kill @e[type=!player]", "fill -10 -60 -4 10 -52 8 minecraft:air"));
        String[] back = {"thaumaturge", "mad_thaumaturge", "dissolved", "quicksilver_elemental", "vengeful_golem",
            "overanimated", "nether_explorer"};
        String[] front = {"paranoid_warrior", "crimson_paladin", "crimson_archer", "crimson_ranger", "taint_spider_smart",
            "thaum_gib", "upcoming_hole", "crimson_orb"};
        for (int i = 0; i < back.length; i++) {
            commands.add("summon thaumaturgicconcilium:" + back[i] + " " + (i * 1.6 - 4.8) + " -60 -1 {NoAI:1b,Rotation:[0f,0f]}");
        }
        for (int i = 0; i < front.length; i++) {
            commands.add("summon thaumaturgicconcilium:" + front[i] + " " + (i * 1.3 - 4.55) + " -60 2 {NoAI:1b,NoGravity:1b,Rotation:[0f,0f]}");
        }
        commands.add("tp @a 0.5 -59 9.5 180 10");
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (String c : commands) {
                LOG.info("TCTEST /{}", c);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
            }
            LOG.info("TCTEST mobs placed");
        });
    }

    private static final String[] PHASE3_CLEAR = {"kill @e[type=!player]", "gamerule mob_griefing false",
        "fill -10 -60 -8 16 -50 8 minecraft:air", "fill -1 -61 -8 16 -61 6 minecraft:grass_block"};
    /** Third scene: the phase 3 rift set (chained rift over its Hex, Lithographer, Vis Condenser, Void Reality, items). */
    private static final String[] RIFT_SCENE = {
        "fill 1 -60 1 3 -60 3 thaumaturgicconcilium:hex_of_predictability[master=false]",
        "setblock 2 -60 2 thaumaturgicconcilium:hex_of_predictability[master=true]",
        "summon thaumaturgicconcilium:flux_rift 2.5 -57 2.5 {RiftSize:40,MaxSize:40,RiftSeed:7,Chained:1b,Hex:[I;2,-60,2]}",
        "setblock 9 -60 2 thaumaturgicconcilium:lithographer[axis=x]",
        "setblock 12 -60 2 thaumaturgicconcilium:vis_condenser",
        "setblock 14 -60 2 thaumaturgicconcilium:void_reality",
        "fill 1 -60 -4 3 -60 -2 thaumaturgicconcilium:hex_of_predictability_thick[master=false]",
        "setblock 2 -60 -3 thaumaturgicconcilium:hex_of_predictability_thick[master=true]",
        "summon item 9.5 -58.5 5.5 {Item:{id:\"thaumaturgicconcilium:rift_gem\",count:1},NoGravity:1b,PickupDelay:32767,Age:-32768}",
        "summon item 10.5 -58.5 5.5 {Item:{id:\"thaumaturgicconcilium:terra_cast_gem\",count:1},NoGravity:1b,PickupDelay:32767,Age:-32768}",
        "summon item 11.5 -58.5 5.5 {Item:{id:\"thaumaturgicconcilium:astral_monitor\",count:1},NoGravity:1b,PickupDelay:32767,Age:-32768}",
        "summon item 12.5 -58.5 5.5 {Item:{id:\"thaumaturgicconcilium:coagulating_eye_drops\",count:1},NoGravity:1b,PickupDelay:32767,Age:-32768}",
        "summon item 13.5 -58.5 5.5 {Item:{id:\"thaumaturgicconcilium:warped_thaumaturge_drum\",count:1},NoGravity:1b,PickupDelay:32767,Age:-32768}"};
    /** Fourth scene: the phase 3 bosses and flesh set. The Peeler needs the Hex under it or it bursts. */
    private static final String[] BOSS_SCENE = {
        "setblock 0 -60 2 thaumaturgicconcilium:flesh_crucible{flesh:48}",
        "setblock 2 -60 2 thaumaturgicconcilium:solid_void",
        "summon thaumaturgicconcilium:brightest_one 5 -59 2 {NoAI:1b,Type:\"thaumaturge:ignis\"}",
        "summon thaumaturgicconcilium:lesser_crimson_portal 9 -60 2 {NoAI:1b}",
        "summon thaumaturgicconcilium:crimson_pontifex 12 -60 2 {NoAI:1b,equipment:{mainhand:{id:\"thaumaturgicconcilium:pontifex_hammer\"},"
            + "head:{id:\"thaumaturgicconcilium:pontifex_robe_head\"},chest:{id:\"thaumaturgicconcilium:pontifex_robe_chest\"},"
            + "legs:{id:\"thaumaturgicconcilium:pontifex_robe_legs\"},feet:{id:\"thaumaturgicconcilium:pontifex_robe_feet\"}}}",
        "setblock 14 -61 2 thaumaturgicconcilium:hex_of_predictability",
        "summon thaumaturgicconcilium:material_peeler 14 -59 2",
        "summon thaumaturgicconcilium:protolimb 1 -59 5 {NoAI:1b,Type:0b}",
        "summon thaumaturgicconcilium:protolimb 3 -59 5 {NoAI:1b,Type:1b}",
        "summon thaumaturgicconcilium:protolimb 5 -59 5 {NoAI:1b,Type:2b}",
        "summon thaumaturgicconcilium:protolimb 7 -59 5 {NoAI:1b,Type:3b}",
        "summon thaumaturgicconcilium:protolimb 9 -59 5 {NoAI:1b,Type:4b}",
        "summon thaumaturgicconcilium:protolimb 11 -59 5 {NoAI:1b,Type:5b}",
        "summon thaumaturgicconcilium:protobody_item 13 -60 5 {Item:{id:\"thaumaturgicconcilium:protobody\",count:1},PickupDelay:32767,Age:-32768}",
        "summon armor_stand 15 -60 5 {equipment:{head:{id:\"thaumaturgicconcilium:pontifex_robe_head\"},"
            + "chest:{id:\"thaumaturgicconcilium:pontifex_robe_chest\"},legs:{id:\"thaumaturgicconcilium:pontifex_robe_legs\"},"
            + "feet:{id:\"thaumaturgicconcilium:pontifex_robe_feet\"}}}"};

    /** Fifth scene (partner mods loaded): the Witchery mobs and the Burning Site. */
    private static final String[] WITCHERY_SCENE = {
        "summon thaumaturgicconcilium:sloppy_alchemist 0 -60 0 {NoAI:1b,Rotation:[180f,0f],Passengers:[{id:\"thaumaturgicconcilium:chort\",NoAI:1b}]}",
        "summon thaumaturgicconcilium:sloppy_alchemist 2 -60 0 {NoAI:1b,Rotation:[180f,0f]}",
        "summon thaumaturgicconcilium:chort 4 -60 0 {NoAI:1b,Rotation:[180f,0f]}",
        "summon thaumaturgicconcilium:burned_witch 6 -59 0 {NoAI:1b,Rotation:[180f,0f]}",
        "place template thaumaturgicconcilium:burning_site 7 -61 -4"};
    /** Sixth scene (partner mods loaded): Horizons creatures, the Red-Powered Mind, syringe, seals and bracelets. */
    private static final String[] THAUMIC_COMPAT_SCENE = {
        "summon thaumaturgicconcilium:golem_bydlo 2 -60 0 {NoAI:1b,Rotation:[180f,0f]}",
        "summon thaumaturgicconcilium:red_powered_mind 5 -60 0 {NoAI:1b}",
        "setblock 7 -60 0 thaumaturgicconcilium:red_powered_mind",
        "setblock 8 -60 0 minecraft:lever[face=floor,facing=east,powered=true]",
        "summon thaumaturgicconcilium:dope_squid 10 -59 0 {NoAI:1b}",
        "summon minecraft:item_display 12 -59.5 0 {item:{id:\"thaumaturgicconcilium:drainage_syringe\"}}",
        "summon minecraft:item_display 13 -59.5 0 {item:{id:\"thaumaturgicconcilium:valet_seal\"}}",
        "summon minecraft:item_display 14 -59.5 0 {item:{id:\"thaumaturgicconcilium:assistant_seal\"}}",
        "summon minecraft:item_display 15 -59.5 0 {item:{id:\"thaumaturgicconcilium:casting_bracelet_infernal\"}}",
        "summon minecraft:item_display 12 -59.5 2 {item:{id:\"thaumaturgicconcilium:casting_bracelet_tainted\"}}",
        "summon minecraft:item_display 13 -59.5 2 {item:{id:\"thaumaturgicconcilium:casting_bracelet_warpwood\"}}",
        "summon minecraft:item_display 14 -59.5 2 {item:{id:\"thaumaturgicconcilium:red_powered_mind\"}}"};

    /** Limb check: a frozen row of Alive Limbs (types 0-5) behind two Overanimated that die on the next step. */
    private static final String[] LIMB_SCENE = {
        "summon thaumaturgicconcilium:thaum_gib -3.5 -60 0.5 {NoAI:1b,Type:0,Rotation:[0f,0f]}",
        "summon thaumaturgicconcilium:thaum_gib -2 -60 0.5 {NoAI:1b,Type:1,Rotation:[0f,0f]}",
        "summon thaumaturgicconcilium:thaum_gib -0.5 -60 0.5 {NoAI:1b,Type:2,Rotation:[0f,0f]}",
        "summon thaumaturgicconcilium:thaum_gib 1 -60 0.5 {NoAI:1b,Type:3,Rotation:[0f,0f]}",
        "summon thaumaturgicconcilium:thaum_gib 2.5 -60 0.5 {NoAI:1b,Type:4,Rotation:[0f,0f]}",
        "summon thaumaturgicconcilium:thaum_gib 4 -60 0.5 {NoAI:1b,Type:5,Rotation:[0f,0f]}",
        "summon thaumaturgicconcilium:overanimated -1.5 -60 3.5 {NoAI:1b,Rotation:[0f,0f]}",
        "summon thaumaturgicconcilium:overanimated 2.5 -60 3.5 {NoAI:1b,Rotation:[0f,0f]}"};

    private static void command(Minecraft mc, String c) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                LOG.info("TCTEST /{}", c);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
            });
        }
    }

    private static void scene(Minecraft mc, String[] scene) {
        scene(mc, scene, "tp @a 7.5 -57 15.5 180 15");
    }

    /** Clears the area and builds one later scene, then moves the camera. */
    private static void scene(Minecraft mc, String[] scene, String camera) {
        java.util.List<String> commands = new java.util.ArrayList<>(java.util.List.of(PHASE3_CLEAR));
        commands.addAll(java.util.List.of(scene));
        commands.add(camera);
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (String c : commands) {
                LOG.info("TCTEST /{}", c);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
            }
            LOG.info("TCTEST scene placed");
        });
    }

    private static void shot(Minecraft mc) {
        String name = "tctest_" + (System.currentTimeMillis() / 1000) + ".png";
        File file = new File(new File(mc.gameDirectory, "screenshots"), name);
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("TCTEST screenshot message: {}", msg.getString()));
        LOG.info("TCTEST shot {}", file.getAbsolutePath());
    }

    private static void finish(Minecraft mc) {
        finished = true;
        LOG.info("TCTEST stopping client");
        mc.stop();
    }
}
