package com.thaumaturgicconcilium.item;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import java.util.function.Consumer;
import java.util.List;
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
import org.jetbrains.annotations.Nullable;

/**
 * Runic Body and Legs Windings: almost no armour, but 200 runic shielding each (Thaumaturge's runic charge component)
 * and a 3% vis discount. Worn, they also restore 5 shielding every second on top of Thaumaturge's own recharge.
 */
public class RunicWindingsItem extends net.minecraft.world.item.ArmorItem implements IVisDiscountGear {
    public RunicWindingsItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, net.minecraft.world.item.ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return 3;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level$, Entity owner, int slot$, boolean selected$) {
        if (!(level$ instanceof ServerLevel level)) {
            return;
        }
        EquipmentSlot slot = com.thaumaturgicconcilium.util.Inv.slotOf(owner, stack);
        if ((slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS) && owner instanceof Player player && player.tickCount % 20 == 0) {
            float max = (float) player.getAttributeValue(Attributes.MAX_ABSORPTION);
            if (player.getAbsorptionAmount() < max) {
                player.setAbsorptionAmount(Math.min(max, player.getAbsorptionAmount() + 5));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.runic_windings").withStyle(ChatFormatting.GRAY));
    }
}
