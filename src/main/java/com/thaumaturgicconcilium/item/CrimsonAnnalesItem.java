package com.thaumaturgicconcilium.item;

import com.leclowndu93150.thaumaturge.api.capability.IPlayerKnowledge;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.content.research.ResearchProgression;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Crimson Annales, a cult chronicle found in dungeon chests. Read it once you know the Crimson Rites and it teaches the
 * Crimson Annales research (the way into the Pontifex's secrets); before that its script makes no sense.
 */
public class CrimsonAnnalesItem extends Item {
    private static final Identifier CRIMSON_RITES = Identifier.parse("thaumaturge:crimson_rites");
    private static final Identifier ANNALES = ThaumaturgicConcilium.id("crimson_annales");

    public CrimsonAnnalesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            IPlayerKnowledge knowledge = KnowledgeAccess.of(sp);
            if (!knowledge.isResearchComplete(CRIMSON_RITES)) {
                sp.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.annales_unreadable").withStyle(ChatFormatting.DARK_RED));
            } else if (knowledge.isResearchComplete(ANNALES)) {
                sp.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.annales_known").withStyle(ChatFormatting.DARK_RED));
            } else {
                ResearchProgression.grant(sp, ANNALES);
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
                sp.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.annales_learned").withStyle(ChatFormatting.DARK_RED));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium.crimson_annales").withStyle(ChatFormatting.GRAY));
    }
}
