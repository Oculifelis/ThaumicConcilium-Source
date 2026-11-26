package com.thaumaturgicconcilium;

import com.leclowndu93150.thaumaturge.api.infusion.InfusionCraftedEvent;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import com.thaumaturgicconcilium.spell.ConductorLock;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;

/** Game-bus hooks: polishment on finished infusions. */
public final class TCEvents {
    private TCEvents() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(TCEvents::polish);
        NeoForge.EVENT_BUS.addListener(TCEvents::replaceOnSpawn);
    }

    /** Spawn replacement (data map {@code spawn_replacement} on entity types). */
    private static void replaceOnSpawn(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var entity = event.getEntity();
        TCDataMaps.Replacement r = entity.getType().builtInRegistryHolder().getData(TCDataMaps.SPAWN_REPLACEMENT);
        if (r == null || level.getRandom().nextFloat() >= r.chance()) {
            return;
        }
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(r.entity());
        if (type.isEmpty()) {
            return;
        }
        var made = type.get().create(level, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
        if (made == null) {
            return;
        }
        made.snapTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        if (made instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.EntitySpawnReason.CONVERSION, null);
            if (entity instanceof net.minecraft.world.entity.Mob old && old.isPersistenceRequired()) {
                mob.setPersistenceRequired();
            }
        }
        event.setCanceled(true);
        level.getServer().execute(() -> level.addFreshEntity(made));
    }

    /** The item's own polishment, else that of the first spell part on it that has one. */
    private static TCDataMaps.Polishment polishmentOf(ItemStack result, ServerLevel level) {
        TCDataMaps.Polishment need = result.typeHolder().getData(TCDataMaps.POLISHMENT);
        Spell spell = result.get(TTDataComponents.SPELL.get());
        if (need != null || spell == null) {
            return need;
        }
        var parts = level.registryAccess().lookupOrThrow(SpellPart.REGISTRY_KEY);
        for (SpellNode node : spell.nodes()) {
            var data = parts.get(node.part()).map(h -> h.getData(TCDataMaps.SPELL_POLISHMENT)).orElse(null);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    /**
     * Polishment: the result needs an extra aspect drawn through a Vis Conductor (see {@link TCDataMaps.Polishment}). The
     * conductor's charge and the crystal it drew from are spent; with no or too little charge the craft is lost.
     */
    private static void polish(InfusionCraftedEvent event) {
        ItemStack result = event.getResult();
        ServerPlayer player = event.getPlayer();
        ServerLevel level = event.getLevel();
        TCDataMaps.Polishment need = polishmentOf(result, level);
        if (need == null) {
            return;
        }
        Optional<ConductorLock.State> lock = player == null ? Optional.empty() : ConductorLock.get(player);
        boolean ok = player != null && player.blockPosition().distSqr(event.getMatrixPos()) <= 16 * 16 && lock.isPresent()
                && lock.get().aspect().equals(need.aspect()) && lock.get().amount() >= need.amount();
        if (ok) {
            ConductorLock.clear(player);
            if (level.getBlockEntity(lock.get().crystal()) instanceof DestabilizedCrystalBlockEntity) {
                level.setBlockAndUpdate(lock.get().crystal(), Blocks.AIR.defaultBlockState());
            }
            level.playSound(null, event.getMatrixPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            return;
        }
        event.setResult(ItemStack.EMPTY);
        level.playSound(null, event.getMatrixPos(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.polishment_failed",
                    need.amount(), Component.translatable("aspect." + need.aspect().identifier().getNamespace() + "."
                            + need.aspect().identifier().getPath())).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
