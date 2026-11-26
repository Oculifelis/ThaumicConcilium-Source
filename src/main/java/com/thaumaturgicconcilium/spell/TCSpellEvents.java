package com.thaumaturgicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.content.aspect.EntityAspects;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game-bus listeners of the spell package. */
final class TCSpellEvents {
    static final ResourceKey<SpellPart> VIS_CONDUCTOR = SpellUtil.part("vis_conductor");
    static final ResourceKey<SpellPart> DEMATERIALIZATION = SpellUtil.part("dematerialization");
    static final ResourceKey<SpellPart> TAINT_ANIMATION = SpellUtil.part("taint_animation");
    static final ResourceKey<SpellPart> THOUGHT_MANIFESTATION = SpellUtil.part("thought_manifestation");
    static final ResourceKey<SpellPart> FLUX_VAPORIZATION = SpellUtil.part("flux_vaporization");
    /** Vis crystals a magic kill with a Vis Conductor drops (TC4: a wisp essence holding 2 of the aspect). */
    private static final int KILL_CRYSTALS = 2;

    private TCSpellEvents() {
    }

    static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, true, SpellCastEvent.Pre.class, TCSpellEvents::onCast);
        bus.addListener(PlayerTickEvent.Post.class, TCSpellEvents::onPlayerTick);
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, TCSpellEvents::onLogout);
        bus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> drop(e.getEntity()));
        bus.addListener(LivingChangeTargetEvent.class, TCSpellEvents::onTarget);
        bus.addListener(LivingDropsEvent.class, TCSpellEvents::onDrops);
    }

    private static void onCast(SpellCastEvent.Pre event) {
        if (!event.isCanceled()) {
            SpellUtil.recordCast(event.caster(), event.style(), event.power());
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Channel.tick(player);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        drop(event.getEntity());
        SpellUtil.forget(event.getEntity().getUUID());
    }

    private static void drop(Player player) {
        Channel.clear(player);
        ConductorLock.clear(player);
    }

    /** Taint Animation: tainted non-boss creatures will not pick a player holding the spell as a target. */
    private static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || mob.level().isClientSide()) {
            return;
        }
        if (!mob.getType().is(ThaumaturgeEntityTypeTags.TAINTED) || mob.getType().is(Tags.EntityTypes.BOSSES)) {
            return;
        }
        if (SpellUtil.holdsPart(player, TAINT_ANIMATION) || SpellUtil.holdsPart(player, THOUGHT_MANIFESTATION)
                || SpellUtil.holdsPart(player, FLUX_VAPORIZATION)) {
            event.setCanceled(true);
        }
    }

    /**
     * Vis Conductor: a creature killed by magic damage from a player holding the spell (not the Wisp Launcher) drops
     * vis crystals of one of its aspects.
     */
    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return;
        }
        if (!SpellUtil.holdsPart(player, VIS_CONDUCTOR) && !SpellUtil.holdsPart(player, DEMATERIALIZATION)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        AspectList aspects = EntityAspects.of(victim);
        if (aspects.isEmpty()) {
            return;
        }
        List<AspectInstance> entries = aspects.entries();
        AspectInstance pick = entries.get(victim.getRandom().nextInt(entries.size()));
        ItemStack crystal = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get(), KILL_CRYSTALS);
        crystal.set(TTDataComponents.CRYSTAL_ASPECT.get(), new AspectInstance(pick.aspect(), 1));
        event.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), crystal));
    }
}
