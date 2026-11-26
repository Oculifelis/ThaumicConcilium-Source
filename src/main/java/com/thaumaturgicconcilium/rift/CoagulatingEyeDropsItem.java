package com.thaumaturgicconcilium.rift;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Coagulating Eye Drops (TC4 {@code CoagulatingEyeDrops}): 100 uses. Held for three seconds, a lithographed player
 * hides the astral overlay, or brings it back; each use costs one point of durability.
 */
public class CoagulatingEyeDropsItem extends Item {
    private static final int USE_TICKS = 60;

    public CoagulatingEyeDropsItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return new InteractionResultHolder<>(InteractionResult.CONSUME, player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer player) {
            RiftData.Lithograph lith = player.getData(TCRifts.LITHOGRAPH);
            if (lith.lithographed()) {
                player.setData(TCRifts.LITHOGRAPH, new RiftData.Lithograph(true, !lith.relieved(), lith.monitored()));
                player.syncData(TCRifts.LITHOGRAPH);
                stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F,
                        2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);
            }
        }
        return stack;
    }
}
