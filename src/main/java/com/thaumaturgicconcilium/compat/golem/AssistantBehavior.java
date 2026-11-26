package com.thaumaturgicconcilium.compat.golem;

import com.leclowndu93150.thaumaturge.api.golems.GolemHelper;
import com.leclowndu93150.thaumaturge.api.golems.IGolemAPI;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealBehavior;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealEntity;
import com.leclowndu93150.thaumaturge.api.golems.seals.SealArea;
import com.leclowndu93150.thaumaturge.api.golems.tasks.Task;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Assistant seal (TC4 Concilium {@code AssistantGolemCore} + {@code GolemAssistantAI} + {@code AIAssistantHomeTake}): a
 * fighting, smart golem carrying empty Warded Jars hunts the creatures in the seal's area, and a blow that would kill
 * one seals it in a Stasis Jar instead (Thaumic Horizons; see {@link AssistantCapture}). Without jars it asks for one
 * (TC4 fetched jars from its home chest). TC4's target check was commented out, so its assistant went for anything
 * its other cores allowed, players and tamed pets included; it now takes only untamed adult animals and non-creeper
 * monsters, as TC4's unused check intended.
 */
public final class AssistantBehavior implements ISealBehavior {
    private static final int SCAN_PERIOD = 20;
    private static final int MARK_LIFE = SCAN_PERIOD + 10;

    private int clock;

    @Override
    public void tick(ServerLevel level, ISealEntity seal) {
        if (++clock % SCAN_PERIOD != 0) {
            return;
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, SealArea.bounds(seal), AssistantBehavior::quarry)) {
            Task hunt = Task.onEntity(seal.pos(), target);
            hunt.setLife(MARK_LIFE);
            hunt.setPriority(seal.priority());
            GolemHelper.addGolemTask(level, hunt);
        }
    }

    static boolean quarry(LivingEntity target) {
        if (!target.isAlive() || target instanceof Player || target instanceof AbstractGolem || target instanceof Creeper || target.isBaby()) {
            return false;
        }
        if (target instanceof TamableAnimal tame && tame.isTame()) {
            return false;
        }
        return target instanceof Animal || target instanceof Enemy;
    }

    static boolean holdsJar(IGolemAPI golem) {
        for (ItemStack carried : golem.hands().contents()) {
            if (carried.is(TTItems.JAR_NORMAL.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onTaskStarted(ServerLevel level, ISealEntity seal, IGolemAPI golem, Task task) {
        if (task.entity() instanceof LivingEntity quarry && quarry(quarry) && golem.asEntity() instanceof Mob hunter) {
            hunter.setTarget(quarry);
            AssistantCapture.assisting(golem, level.getGameTime());
            golem.addRankXp(1);
        }
        task.end();
    }

    @Override
    public boolean completeTask(ServerLevel level, ISealEntity seal, IGolemAPI golem, Task task) {
        return true;
    }

    @Override
    public boolean canPerform(ISealEntity seal, IGolemAPI golem, Task task) {
        if (holdsJar(golem)) {
            return true;
        }
        GolemHelper.requestProvisioning(golem.level(), seal, new ItemStack(TTItems.JAR_NORMAL.get()));
        return false;
    }
}
