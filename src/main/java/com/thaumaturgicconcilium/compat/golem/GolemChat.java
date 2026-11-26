package com.thaumaturgicconcilium.compat.golem;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.GlobalPos;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * TC4 Concilium {@code GolemChatHandler}: the last chat line each player said, for Valet seals to listen to. Kept with
 * when and where it was said, so a seal answers each line once and only within earshot (32 blocks, as TC4's golem
 * search). Forgotten when the player logs out.
 */
public final class GolemChat {
    public record Line(String text, long time, GlobalPos where) {
    }

    private static final Map<UUID, Line> LAST = new ConcurrentHashMap<>();

    private GolemChat() {
    }

    public static Optional<Line> last(UUID player) {
        return Optional.ofNullable(LAST.get(player));
    }

    static void onChat(ServerChatEvent event) {
        var player = event.getPlayer();
        LAST.put(player.getUUID(), new Line(event.getRawText().trim(), player.level().getGameTime(),
                GlobalPos.of(player.level().dimension(), player.blockPosition())));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
