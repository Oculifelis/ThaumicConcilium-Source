package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import com.thaumaturgicconcilium.crimson.Chains;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of the Pontifex's robe (TC4 Concilium {@code PontifexRobe}): epic, dyeable (undyed crimson 0xA01818), 5 runic
 * shielding and 5% vis discount per piece, 3 warp; the cap reveals like Goggles of Revealing. Wearing all four:
 * <ul>
 * <li>the robe key (L) toggles its own ethereal state ({@link RobeTogglePayload}), which keeps the wearer chained
 * (no hands) for as long as it is on;</li>
 * <li>crimson cultists will not target the wearer, and follow the wearer's attacks ({@link Ethereal}).</li>
 * </ul>
 */
public class PontifexRobeItem extends net.minecraft.world.item.ArmorItem implements IVisDiscountGear {
    public static final int RUNIC_CHARGE = 5;
    public static final int WARP = 3;
    public static final int VIS_DISCOUNT = 5;
    public static final int UNDYED = 0xA01818;

    public PontifexRobeItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public Type armorType() {
        return getType();
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return VIS_DISCOUNT;
    }

    /** Only the robe (chest piece) ticks the set's chain, so it is applied once a tick. */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level$, Entity owner, int slot$, boolean selected$) {
        if (!(level$ instanceof ServerLevel level)) {
            return;
        }
        EquipmentSlot slot = com.thaumaturgicconcilium.util.Inv.slotOf(owner, stack);
        if (getType() == Type.CHESTPLATE && slot == EquipmentSlot.CHEST && owner instanceof Player player && Ethereal.robeToggled(player)
                && Ethereal.isFullSet(player)) {
            Chains.chain(player, Chains.DURATION);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.vis_discount", VIS_DISCOUNT).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.pontifex_robe").withStyle(ChatFormatting.GRAY));
    }
}
