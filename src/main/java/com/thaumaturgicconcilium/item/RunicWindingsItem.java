package com.thaumaturgicconcilium.item;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * Runic Body and Legs Windings: almost no armour, but 200 runic shielding each (Thaumaturge's runic charge component)
 * and a 3% vis discount. Worn, they also restore 5 shielding every second on top of Thaumaturge's own recharge.
 */
public class RunicWindingsItem extends Item implements IVisDiscountGear {
    public RunicWindingsItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return 3;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if ((slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS) && owner instanceof Player player && player.tickCount % 20 == 0) {
            float max = (float) player.getAttributeValue(Attributes.MAX_ABSORPTION);
            if (player.getAbsorptionAmount() < max) {
                player.setAbsorptionAmount(Math.min(max, player.getAbsorptionAmount() + 5));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium.runic_windings").withStyle(ChatFormatting.GRAY));
    }
}
