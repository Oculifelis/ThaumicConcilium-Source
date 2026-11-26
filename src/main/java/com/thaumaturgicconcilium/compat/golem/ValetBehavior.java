package com.thaumaturgicconcilium.compat.golem;

import com.leclowndu93150.thaumaturge.api.golems.GolemHelper;
import com.leclowndu93150.thaumaturge.api.golems.IGolemAPI;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealBehavior;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealEntity;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealFilter;
import com.leclowndu93150.thaumaturge.api.golems.tasks.Task;
import com.leclowndu93150.thaumaturge.content.golem.GolemInteractionHelper;
import com.leclowndu93150.thaumaturge.content.golem.seals.behavior.UseBehavior;
import com.leclowndu93150.thaumaturge.content.golem.tasks.TaskBoard;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Valet seal (TC4 Concilium {@code ValetGolemCore} + {@code GolemValetAI}). Put a named item in the seal's filter slot:
 * its name is the password. When the seal's owner says the password in chat within 32 blocks, a golem (deft, smart)
 * walks to the seal and uses what it carries on that block, like a Use seal: right click by default, or left click,
 * sneaking, into the air or bare-handed per the seal's settings. TC4 matched named paper held in the golem's own
 * inventory to colored markers; the seal's filter slot takes over that role. Each line said is answered once.
 */
public final class ValetBehavior implements ISealBehavior {
    private static final int SCAN_PERIOD = 5;
    private static final double EARSHOT = 32.0;

    private int clock;
    private long lastHeard = Long.MIN_VALUE;
    private @Nullable Task pending;

    @Override
    public void tick(ServerLevel level, ISealEntity seal) {
        if (lastHeard == Long.MIN_VALUE) {
            lastHeard = level.getGameTime() - SCAN_PERIOD; // lines said before the seal was there are not orders
        }
        if (++clock % SCAN_PERIOD != 0 || seal.owner() == null) {
            return;
        }
        if (pending != null && TaskBoard.of(level).isLive(pending.id())) {
            return;
        }
        String password = password(seal);
        if (password == null) {
            return;
        }
        Optional<GolemChat.Line> heard = GolemChat.last(seal.owner());
        if (heard.isEmpty() || heard.get().time() <= lastHeard) {
            return;
        }
        GolemChat.Line line = heard.get();
        lastHeard = line.time();
        if (!line.text().equals(password) || line.where().dimension() != level.dimension()
                || !line.where().pos().closerThan(seal.pos().pos(), EARSHOT)) {
            return;
        }
        if (level.getBlockState(seal.pos().pos()).isAir() != seal.setting(UseBehavior.INTO_AIR)) {
            return;
        }
        Task task = Task.atBlock(seal.pos(), seal.pos().pos());
        task.setPriority(seal.priority());
        GolemHelper.addGolemTask(level, task);
        pending = task;
    }

    private static @Nullable String password(ISealEntity seal) {
        ItemStack named = seal.filter().map((ISealFilter f) -> f.stack(0)).orElse(ItemStack.EMPTY);
        return named.isEmpty() ? null : named.getHoverName().getString().trim();
    }

    @Override
    public boolean completeTask(ServerLevel level, ISealEntity seal, IGolemAPI golem, Task task) {
        boolean bare = seal.setting(UseBehavior.BARE_HANDED);
        ItemStack held = ItemStack.EMPTY;
        if (!bare) {
            for (ItemStack carried : golem.hands().contents()) {
                if (!carried.isEmpty()) {
                    held = golem.hands().release(carried.copy());
                    break;
                }
            }
        }
        GolemInteractionHelper.golemClick(level, golem, seal.pos().pos(), seal.pos().face(), held, seal.setting(UseBehavior.LEFT_CLICK),
                seal.setting(UseBehavior.SNEAKING));
        golem.addRankXp(1);
        task.end();
        return true;
    }

    @Override
    public boolean canPerform(ISealEntity seal, IGolemAPI golem, Task task) {
        UUID owner = seal.owner();
        return owner == null || golem.ownerIdentity().map(owner::equals).orElse(true);
    }
}
