package com.thaumaturgicconcilium.compat.witchery;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import dev.sterner.witchery.content.entity.ImpEntity;
import dev.sterner.witchery.core.api.event.ImpEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;

/**
 * End-to-end check of the imp deal, run on the server thread by the dev client autotest when Witchery is loaded. It
 * posts Witchery's own {@code ImpEvents.Interact} for a real imp, then checks the offering, the pact, the mobs of the
 * {@code honours_imp_pact} tag and a broken pact. Logs one {@code SELFTEST PASS} or {@code SELFTEST FAIL} line per check.
 */
public final class ImpPactSelfTest {
    private ImpPactSelfTest() {
    }

    private static void check(String name, boolean ok, String detail) {
        ThaumaturgicConcilium.LOG.info("SELFTEST {} {}: {}", ok ? "PASS" : "FAIL", name, detail);
    }

    private static Mob spawn(ServerLevel level, ServerPlayer player, String id, double dx, List<Entity> spawned) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
        Entity e = type.create(level, EntitySpawnReason.COMMAND);
        if (!(e instanceof Mob mob)) {
            check("imp_pact_spawn_" + id, false, "could not create " + id);
            return null;
        }
        mob.setNoAi(true);
        mob.setPos(player.getX() + dx, player.getY(), player.getZ() + 4.0);
        level.addFreshEntity(mob);
        spawned.add(mob);
        return mob;
    }

    private static boolean targets(Mob mob, ServerPlayer player) {
        mob.setTarget(null);
        mob.setTarget(player);
        boolean t = mob.getTarget() == player;
        mob.setTarget(null);
        return t;
    }

    public static void run(MinecraftServer server) {
        List<Entity> spawned = new ArrayList<>();
        ServerPlayer player = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
        if (player == null) {
            check("imp_pact", false, "no player");
            return;
        }
        ServerLevel level = player.level();
        GameType mode = player.gameMode.getGameModeForPlayer();
        ItemStack held = player.getMainHandItem().copy();
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.setData(ImpPact.PACT_END, 0L);
            check("imp_pact_duration", ImpPact.duration() == ImpPact.DEFAULT_DURATION, "duration " + ImpPact.duration());
            Mob imp = spawn(level, player, "witchery:imp", 0, spawned);
            Mob chort = spawn(level, player, "thaumaturgicconcilium:chort", 2, spawned);
            Mob sloppy = spawn(level, player, "thaumaturgicconcilium:sloppy_alchemist", 4, spawned);
            Mob demon = spawn(level, player, "witchery:demon", 6, spawned);
            if (!(imp instanceof ImpEntity impEntity) || chort == null || sloppy == null || demon == null) {
                return;
            }
            check("imp_pact_no_pact_targets", targets(chort, player), "chort targets a player with no pact");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 2));
            ImpEvents.Interact wrong = NeoForge.EVENT_BUS.post(new ImpEvents.Interact(impEntity, player, InteractionHand.MAIN_HAND));
            check("imp_pact_ignores_non_offering", !wrong.isCanceled() && !ImpPact.hasPact(player)
                    && player.getMainHandItem().getCount() == 2, "dirt left " + player.getMainHandItem().getCount());

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, 2));
            ImpEvents.Interact deal = NeoForge.EVENT_BUS.post(new ImpEvents.Interact(impEntity, player, InteractionHand.MAIN_HAND));
            long left = player.getData(ImpPact.PACT_END) - level.getGameTime();
            check("imp_pact_offering_taken", deal.isCanceled() && player.getMainHandItem().getCount() == 1 && ImpPact.hasPact(player)
                    && left == ImpPact.duration(), "gold left " + player.getMainHandItem().getCount() + ", pact ticks " + left);

            for (Mob mob : List.of(chort, sloppy, imp, demon)) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
                check("imp_pact_spares_" + id.replace(':', '_'), mob.getType().builtInRegistryHolder().is(ImpPact.HONOURS) && !targets(mob, player),
                        id + " in tag " + mob.getType().builtInRegistryHolder().is(ImpPact.HONOURS));
            }

            chort.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
            check("imp_pact_broken_for_hurt_mob", targets(chort, player), "chort after being hit, broken "
                    + chort.getData(ImpPact.BROKEN).contains(player.getUUID()));
            check("imp_pact_kept_for_other_mobs", !targets(sloppy, player) && !targets(demon, player) && ImpPact.hasPact(player),
                    "sloppy alchemist and demon still spare the player");

            player.setData(ImpPact.PACT_END, level.getGameTime());
            check("imp_pact_expires", targets(sloppy, player), "sloppy alchemist targets once the pact ends");
        } catch (Exception ex) {
            ThaumaturgicConcilium.LOG.error("SELFTEST FAIL imp_pact crashed", ex);
        } finally {
            spawned.forEach(Entity::discard);
            player.setData(ImpPact.PACT_END, 0L);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            player.setGameMode(mode);
        }
    }
}
