package com.thaumaturgicconcilium.compat.bases;

import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Casting Bracelet (TC4 Concilium {@code Bracelets}, made with Thaumic Bases' bracelet model): a staff-class caster worn
 * on the wrist. Its cap and core are fixed per bracelet (TC4 metadata variants are separate items here, each with its
 * parts as a default component). It spends a third less vis than its cap would on a wand, channels twice per tick, and
 * its foci cool down in half the time (see {@link BasesContent}).
 */
public class BraceletItem extends ItemWand {
    public BraceletItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId());
    }

    @Override
    public boolean usesStaffRod(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasSceptreCore(ItemStack stack) {
        return false;
    }

    /** TC4: {@code onUsingFocusTick} twice a tick (count and count + 1). */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        super.onUseTick(level, user, stack, remaining);
        if (user.isUsingItem() && user.getUseItem() == stack) {
            super.onUseTick(level, user, stack, remaining - 1);
        }
    }
}
