package com.thaumaturgicconcilium.compat.arsmagica;

import at.minecraftschurli.mods.arsmagicalegacy.api.ArsMagicaApi;
import at.minecraftschurli.mods.arsmagicalegacy.api.constants.AMTranslations;
import at.minecraftschurli.mods.arsmagicalegacy.api.spell.SpellCastContext;
import at.minecraftschurli.mods.arsmagicalegacy.api.spell.SpellComponent;
import at.minecraftschurli.mods.arsmagicalegacy.api.spell.SpellComponentCastResult;
import at.minecraftschurli.mods.arsmagicalegacy.api.spell.SpellModifier;
import at.minecraftschurli.mods.arsmagicalegacy.attachment.SummonMinionsAttachment;
import at.minecraftschurli.mods.arsmagicalegacy.init.AMAttachments;
import com.thaumaturgicconcilium.boss.Ethereal;
import com.thaumaturgicconcilium.boss.LesserPortalEntity;
import com.thaumaturgicconcilium.boss.TCBosses;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crimson Raid (TC4 {@code AM2Integration.CrimsonRaid}): an Ars Magica spell component that opens a Lesser Crimson Portal
 * where the spell lands (on the block face hit, or at the entity hit). Only a caster wearing the full Pontifex robe can
 * cast it (the cultists it calls spare that wearer). The portal counts as one of the caster's summons, so the summon
 * limit applies, and it closes after 400 ticks (TC4 AM2 summon duration).
 */
public class CrimsonRaidComponent extends SpellComponent.CastBoth {
    public static final int DURATION = 400;

    @Override
    public SpellComponentCastResult castBlock(List<SpellModifier> modifiers, SpellCastContext ctx, BlockHitResult hit) {
        return raid(ctx, Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection())));
    }

    @Override
    public SpellComponentCastResult castEntity(List<SpellModifier> modifiers, SpellCastContext ctx, EntityHitResult hit) {
        return raid(ctx, hit.getEntity().position());
    }

    private SpellComponentCastResult raid(SpellCastContext ctx, Vec3 at) {
        if (!(ctx.level() instanceof ServerLevel level)) {
            return SpellComponentCastResult.pass(ctx.spell());
        }
        LivingEntity caster = ctx.caster();
        if (!(caster instanceof Player player)) {
            return SpellComponentCastResult.failure(ctx.spell(), AMTranslations.SPELL_FAIL_NO_CASTER);
        }
        if (!Ethereal.isFullSet(player)) {
            return SpellComponentCastResult.failure(ctx.spell(), Component.translatable("message.thaumaturgicconcilium.crimson_raid.robe"));
        }
        SummonMinionsAttachment minions = caster.getData(AMAttachments.SUMMON_MINIONS);
        if (minions.size() >= ArsMagicaApi.spellHelper().getMaxSummons(caster)) {
            return SpellComponentCastResult.failure(ctx.spell(), AMTranslations.SPELL_FAIL_COMPONENT_SUMMON_TOO_MANY);
        }
        LesserPortalEntity portal = TCBosses.LESSER_PORTAL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (portal == null) {
            return SpellComponentCastResult.pass(ctx.spell());
        }
        portal.snapTo(at.x, at.y, at.z, caster.getYRot(), 0.0F);
        portal.setData(AMAttachments.SUMMON_OWNER, caster.getUUID());
        portal.setData(ArsMagicaCompat.RAID_UNTIL, level.getGameTime() + DURATION);
        caster.setData(AMAttachments.SUMMON_MINIONS, minions.add(portal.getUUID()));
        level.addFreshEntity(portal);
        return SpellComponentCastResult.success(ctx.spell());
    }
}
