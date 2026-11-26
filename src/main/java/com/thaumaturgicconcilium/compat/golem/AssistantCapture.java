package com.thaumaturgicconcilium.compat.golem;

import com.leclowndu93150.thaumaturge.api.golems.IGolemAPI;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The Assistant's jarring blow (TC4 {@code GolemAssistantAI.jarEntity}): when a golem sent by an Assistant seal, carrying
 * an empty Warded Jar, deals a blow that would kill its quarry, the blow is cancelled, one jar is used and the creature
 * drops as a jarred creature. The jar maker is supplied by the Thaumic Horizons link (its Stasis Jar); without Horizons
 * the Assistant simply fights.
 */
public final class AssistantCapture {
    /** How long a golem counts as assisting after its seal set it on a quarry: a minute. */
    private static final long ASSIST_TICKS = 1200L;
    private static final Map<UUID, Long> ASSISTING = new ConcurrentHashMap<>();
    private static @Nullable Function<LivingEntity, ItemStack> capturer;

    private AssistantCapture() {
    }

    public static void setCapturer(Function<LivingEntity, ItemStack> jarMaker) {
        capturer = jarMaker;
    }

    static void assisting(IGolemAPI golem, long now) {
        ASSISTING.put(golem.asEntity().getUUID(), now);
    }

    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Function<LivingEntity, ItemStack> jarMaker = capturer;
        LivingEntity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (jarMaker == null || !(attacker instanceof IGolemAPI golem) || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        Long since = ASSISTING.get(attacker.getUUID());
        if (since == null || level.getGameTime() - since > ASSIST_TICKS) {
            return;
        }
        if (event.getAmount() < target.getHealth() || !AssistantBehavior.quarry(target)) {
            return;
        }
        ItemStack jar = ItemStack.EMPTY;
        for (ItemStack carried : golem.hands().contents()) {
            if (carried.is(TTItems.JAR_NORMAL.get())) {
                jar = carried;
                break;
            }
        }
        if (jar.isEmpty()) {
            return;
        }
        ItemStack jarred = jarMaker.apply(target);
        if (jarred.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        golem.hands().release(jar.copyWithCount(1));
        golem.swingArm();
        level.playSound(null, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), TTSounds.CRAFTFAIL.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        golem.asEntity().spawnAtLocation(jarred, 1.0F);
        target.discard();
    }
}
