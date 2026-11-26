package com.thaumaturgicconcilium.crimson;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The "chained" state from the Ethereal Shackles: for its duration a player cannot interact with blocks or entities,
 * start using an item or attack (the original cancelled PlayerInteractEvent, PlayerUseItemEvent.Start and
 * AttackEntityEvent). Checked on both sides, since the end time is synced.
 */
public final class Chains {
    /** The original's {@code chainedTime = 100}. */
    public static final int DURATION = 100;

    private Chains() {
    }

    public static boolean isChained(Player player) {
        return player.level().getGameTime() < player.getData(TCCrimson.CHAINED_UNTIL);
    }

    public static void chain(Player player, int ticks) {
        player.setData(TCCrimson.CHAINED_UNTIL, player.level().getGameTime() + ticks);
    }

    static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, PlayerInteractEvent.RightClickBlock.class, Chains::block);
        bus.addListener(EventPriority.HIGH, PlayerInteractEvent.RightClickItem.class, Chains::block);
        bus.addListener(EventPriority.HIGH, PlayerInteractEvent.LeftClickBlock.class, Chains::block);
        bus.addListener(EventPriority.HIGH, PlayerInteractEvent.EntityInteract.class, Chains::block);
        bus.addListener(EventPriority.HIGH, PlayerInteractEvent.EntityInteractSpecific.class, Chains::block);
        bus.addListener(EventPriority.HIGH, AttackEntityEvent.class, Chains::block);
        bus.addListener(EventPriority.HIGH, LivingEntityUseItemEvent.Start.class, e -> {
            if (e.getEntity() instanceof Player p && isChained(p)) {
                e.setCanceled(true);
            }
        });
    }

    private static <E extends PlayerEvent & ICancellableEvent> void block(E event) {
        if (isChained(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
