package com.thaumaturgicconcilium.compat.arsmagica;

import at.minecraftschurli.mods.arsmagicalegacy.api.ArsMagicaApi;
import at.minecraftschurli.mods.arsmagicalegacy.api.constants.AMRegistries;
import at.minecraftschurli.mods.arsmagicalegacy.api.magic.Skill;
import at.minecraftschurli.mods.arsmagicalegacy.api.spell.SpellPart;
import at.minecraftschurli.mods.arsmagicalegacy.attachment.SummonMinionsAttachment;
import at.minecraftschurli.mods.arsmagicalegacy.init.AMAttachments;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.research.ResearchEvent;
import com.mojang.serialization.Codec;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.boss.LesserPortalEntity;
import com.thaumaturgicconcilium.rift.ExtraReadings;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Ars Magica Legacy compat (TC4 {@code AM2Integration}): the Crimson Raid spell component, its hidden skill (learned with
 * the Crimson Raid research, data in tools/gen_compat_a.py), the 400-tick life of the portal it opens, and the "mana"
 * glyph of the Astral Monitor and the Lithographer (the watched player's Ars Magica mana).
 *
 * <p>The original only registered the component with Tainted Magic installed (its portal needed it); the port's Lesser
 * Crimson Portal is Thaumic Concilium's own, so that gate is gone.
 */
public final class ArsMagicaCompat {
    public static final Identifier RESEARCH = ThaumaturgicConcilium.id("crimson_raid");
    public static final Identifier SKILL = ThaumaturgicConcilium.id("crimson_raid");

    public static final DeferredRegister<SpellPart> SPELL_PARTS = DeferredRegister.create(AMRegistries.Keys.SPELL_PART, ThaumaturgicConcilium.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
            ThaumaturgicConcilium.MODID);

    public static final DeferredHolder<SpellPart, CrimsonRaidComponent> CRIMSON_RAID = SPELL_PARTS.register("crimson_raid", CrimsonRaidComponent::new);
    /** Game time at which a raid portal closes (saved, so a reload does not make it permanent). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> RAID_UNTIL = ATTACHMENTS.register("raid_until",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("until")).build());

    private ArsMagicaCompat() {
    }

    public static void register(IEventBus modBus) {
        SPELL_PARTS.register(modBus);
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(ArsMagicaCompat::researched);
        NeoForge.EVENT_BUS.addListener(ArsMagicaCompat::login);
        NeoForge.EVENT_BUS.addListener(ArsMagicaCompat::portalTick);
        ExtraReadings.setMana(ArsMagicaCompat::mana);
    }

    // ------------------------------------------------------------------ the skill

    private static void researched(ResearchEvent.Completed event) {
        if (event.research().equals(RESEARCH) && event.player() instanceof ServerPlayer player) {
            learn(player);
        }
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KnowledgeAccess.of(player).isResearchComplete(RESEARCH)) {
            learn(player);
        }
    }

    private static void learn(ServerPlayer player) {
        ResourceKey<Skill> key = ResourceKey.create(AMRegistries.Keys.SKILL, SKILL);
        Holder<Skill> skill = player.level().registryAccess().lookup(AMRegistries.Keys.SKILL).flatMap(r -> r.get(key)).orElse(null);
        if (skill != null && !ArsMagicaApi.magicHelper().knows(player, skill)) {
            ArsMagicaApi.magicHelper().learn(player, skill);
        }
    }

    // ------------------------------------------------------------------ the portal's life

    private static void portalTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof LesserPortalEntity portal) || !(portal.level() instanceof ServerLevel level) || !portal.hasData(RAID_UNTIL)) {
            return;
        }
        if (level.getGameTime() < portal.getData(RAID_UNTIL)) {
            return;
        }
        if (portal.hasData(AMAttachments.SUMMON_OWNER)) {
            UUID owner = portal.getData(AMAttachments.SUMMON_OWNER);
            if (level.getEntity(owner) instanceof LivingEntity caster) {
                SummonMinionsAttachment minions = caster.getData(AMAttachments.SUMMON_MINIONS);
                caster.setData(AMAttachments.SUMMON_MINIONS, minions.remove(portal.getUUID()));
            }
        }
        portal.discard();
    }

    // ------------------------------------------------------------------ mana glyph

    static int mana(ServerPlayer player) {
        double max = ArsMagicaApi.manaHelper().getMaxMana(player);
        return ExtraReadings.scale(ArsMagicaApi.manaHelper().getMana(player), max);
    }
}
