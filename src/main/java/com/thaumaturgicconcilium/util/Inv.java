package com.thaumaturgicconcilium.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Every stack in a player inventory: main, armor and offhand. */
public final class Inv {
    private Inv() {}

    public static List<ItemStack> all(Inventory inv) {
        List<ItemStack> out = new ArrayList<>(inv.getContainerSize());
        for (int i = 0; i < inv.getContainerSize(); i++) {
            out.add(inv.getItem(i));
        }
        return out;
    }

    /** The equipment slot that holds this exact stack, or null when it is only in the inventory. */
    public static EquipmentSlot slotOf(Entity owner, ItemStack stack) {
        if (owner instanceof LivingEntity le) {
            for (EquipmentSlot s : EquipmentSlot.values()) {
                if (le.getItemBySlot(s) == stack) {
                    return s;
                }
            }
        }
        return null;
    }
}
