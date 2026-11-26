package com.thaumaturgicconcilium.item;

import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Dump Jackboots: sneak while holding a block and every other tick the boots set that block under your feet and push you
 * up half a block, so you pillar up without jumping. Each block placed is used up; falling is forgiven.
 */
public class DumpJackbootsItem extends net.minecraft.world.item.ArmorItem {
    public DumpJackbootsItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, net.minecraft.world.item.ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level$, Entity owner, int slot$, boolean selected$) {
        if (!(level$ instanceof ServerLevel level)) {
            return;
        }
        EquipmentSlot slot = com.thaumaturgicconcilium.util.Inv.slotOf(owner, stack);
        if (slot != EquipmentSlot.FEET || !(owner instanceof Player player) || !player.isShiftKeyDown() || player.tickCount % 2 != 0) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        BlockPos below = player.blockPosition().below();
        if (!level.getBlockState(below).canBeReplaced()) {
            below = player.blockPosition();
            if (!level.getBlockState(below).canBeReplaced()) {
                return;
            }
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (!state.canSurvive(level, below) || !level.mayInteract(player, below)) {
            return;
        }
        level.setBlockAndUpdate(below, state);
        player.setDeltaMovement(player.getDeltaMovement().x, 0.5, player.getDeltaMovement().z);
        player.hurtMarked = true;
        player.resetFallDistance();
        if (!player.hasInfiniteMaterials()) {
            held.shrink(1);
        }
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.dump_jackboots").withStyle(ChatFormatting.GRAY));
    }
}
