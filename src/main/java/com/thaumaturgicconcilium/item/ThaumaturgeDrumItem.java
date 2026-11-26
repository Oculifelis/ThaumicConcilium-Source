package com.thaumaturgicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.entity.EntityMindSpider;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Thaumaturge's Drum: beat it (hold use) and every 1.5 seconds a wisp of a random primal answers, while fewer than 16
 * are near. The Warped Drum, reforged through a rift, calls harmless mind spiders instead, twice as often.
 */
public class ThaumaturgeDrumItem extends Item {
    private final boolean warped;

    public ThaumaturgeDrumItem(Properties properties, boolean warped) {
        super(properties);
        this.warped = warped;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int used = getUseDuration(stack, user) - remaining;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (used % 8 == 0) {
            server.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (warped) {
            if (used % 10 == 0 && server.getEntitiesOfClass(EntityMindSpider.class, user.getBoundingBox().inflate(32)).size() < 16) {
                EntityMindSpider spider = TTEntities.MIND_SPIDER.get().create(server, EntitySpawnReason.MOB_SUMMONED);
                if (spider != null) {
                    spider.setPos(user.getX() + server.getRandom().nextGaussian(), user.getY(), user.getZ() + server.getRandom().nextGaussian());
                    spider.bindIllusion(user.getName().getString());
                    server.addFreshEntity(spider);
                }
            }
        } else if (used % 30 == 0 && server.getEntitiesOfClass(WispEntity.class, user.getBoundingBox().inflate(64, 32, 64)).size() < 16) {
            WispEntity wisp = TTEntities.WISP.get().create(server, EntitySpawnReason.MOB_SUMMONED);
            if (wisp != null) {
                wisp.setAspect(TTAspects.PRIMALS.get(server.getRandom().nextInt(TTAspects.PRIMALS.size())).identifier());
                wisp.setPos(user.getX() + server.getRandom().nextGaussian() * 2, user.getY() + 2, user.getZ() + server.getRandom().nextGaussian() * 2);
                server.addFreshEntity(wisp);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium." + (warped ? "warped_drum" : "thaumaturge_drum"))
                .withStyle(ChatFormatting.GRAY));
    }
}
