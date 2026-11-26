package com.thaumaturgicconcilium.compat.neovitae;

import com.breakinblocks.neovitae.api.NeoVitaeAPI;
import com.breakinblocks.neovitae.api.soul.IAnima;
import com.breakinblocks.neovitae.common.item.BloodOrbItem;
import com.thaumaturgicconcilium.rift.ExtraReadings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Neo Vitae (the Blood Magic successor): feeds the "blood" glyph of the Astral Monitor and the Lithographer. TC4 read
 * the watched player's soul network essence against the capacity of the best Blood Orb they had bound. Neo Vitae keeps
 * no "best orb" per network, so the port takes the largest orb capacity in the watched player's inventory; with no
 * orb on them the glyph stays dark (-1), as TC4's did for a player without a network.
 */
public final class NeoVitaeCompat {
    private NeoVitaeCompat() {
    }

    public static void register() {
        ExtraReadings.setBlood(NeoVitaeCompat::blood);
    }

    static int blood(ServerPlayer player) {
        int max = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof BloodOrbItem orb) {
                max = Math.max(max, orb.getAnimaCapacity(stack));
            }
        }
        if (max <= 0) {
            return -1;
        }
        IAnima anima = NeoVitaeAPI.getInstance().getAnima(player.getUUID());
        if (anima == null) {
            return -1;
        }
        return ExtraReadings.scale(anima.getCurrentEV(), max);
    }
}
