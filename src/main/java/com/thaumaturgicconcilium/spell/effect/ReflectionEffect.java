package com.thaumaturgicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.aspect.AspectComponents;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.Channel;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reflection (and Oblivion), costs no vis. While held it whispers every {@code whisper_interval} ticks and, every tick,
 * trades {@code xp} experience (when the player has at least {@code xp + potency}) for {@code warp + warp_per_potency
 * * potency} temporary warp. Oblivion instead picks a random compound aspect every {@code oblivion_interval} ticks;
 * if the player has any of it in their research pool, one point is forgotten for {@code oblivion_warp +
 * oblivion_warp_per_potency * potency} temporary warp.
 */
public final class ReflectionEffect extends AbstractEffectBehavior {
    public static final MapCodec<ReflectionEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("whisper_interval", 20).forGetter(e -> e.whisperInterval),
            Codec.INT.optionalFieldOf("xp", 10).forGetter(e -> e.xp),
            Codec.INT.optionalFieldOf("warp", 1).forGetter(e -> e.warp),
            Codec.FLOAT.optionalFieldOf("warp_per_potency", 1.0F).forGetter(e -> e.warpPerPotency),
            Codec.BOOL.optionalFieldOf("oblivion", false).forGetter(e -> e.oblivion),
            Codec.INT.optionalFieldOf("oblivion_interval", 5).forGetter(e -> e.oblivionInterval),
            Codec.INT.optionalFieldOf("oblivion_warp", 10).forGetter(e -> e.oblivionWarp),
            Codec.FLOAT.optionalFieldOf("oblivion_warp_per_potency", 2.0F).forGetter(e -> e.oblivionWarpPerPotency),
            Identifier.CODEC.optionalFieldOf("whisper").forGetter(e -> e.whisper)).apply(i, ReflectionEffect::new));

    private final int whisperInterval;
    private final int xp;
    private final int warp;
    private final float warpPerPotency;
    private final boolean oblivion;
    private final int oblivionInterval;
    private final int oblivionWarp;
    private final float oblivionWarpPerPotency;
    private final Optional<Identifier> whisper;

    public ReflectionEffect(int whisperInterval, int xp, int warp, float warpPerPotency, boolean oblivion, int oblivionInterval, int oblivionWarp,
            float oblivionWarpPerPotency, Optional<Identifier> whisper) {
        this.whisperInterval = whisperInterval;
        this.xp = xp;
        this.warp = warp;
        this.warpPerPotency = warpPerPotency;
        this.oblivion = oblivion;
        this.oblivionInterval = oblivionInterval;
        this.oblivionWarp = oblivionWarp;
        this.oblivionWarpPerPotency = oblivionWarpPerPotency;
        this.whisper = whisper;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpells.REFLECTION.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (!(target.entity().orElse(null) instanceof ServerPlayer player)) {
            return;
        }
        float potency = SpellUtil.potency(ctx, power);
        int ticks = SpellUtil.pulseTicks(ctx);
        Channel.Session session = Channel.of(ctx);
        if (session.due("whisper", ticks, whisperInterval) > 0) {
            SpellUtil.play(ctx.level(), whisper, player.position(), 0.5F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
        }
        if (oblivion) {
            int due = session.due("oblivion", ticks, oblivionInterval);
            List<Holder.Reference<IAspect>> compounds = ctx.registries().lookupOrThrow(IAspect.REGISTRY_KEY).listElements()
                    .filter(h -> !h.value().isPrimal()).toList();
            for (int k = 0; k < due && !compounds.isEmpty(); k++) {
                Holder<IAspect> aspect = compounds.get(ctx.random().nextInt(compounds.size()));
                if (AspectPoolAccess.amount(player, aspect) > 0 && AspectPoolAccess.spend(player, aspect, 1)) {
                    player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.oblivion_response", AspectComponents.name(aspect))
                            .withStyle(ChatFormatting.DARK_PURPLE));
                    WarpHelper.addWarp(player, Math.round(oblivionWarp + oblivionWarpPerPotency * potency), WarpType.TEMPORARY);
                }
            }
            return;
        }
        int gain = Math.round(warp + warpPerPotency * potency);
        for (int k = 0; k < ticks; k++) {
            if (player.totalExperience < xp + potency) {
                break;
            }
            player.giveExperiencePoints(-xp);
            WarpHelper.addWarp(player, gain, WarpType.TEMPORARY);
        }
    }
}
