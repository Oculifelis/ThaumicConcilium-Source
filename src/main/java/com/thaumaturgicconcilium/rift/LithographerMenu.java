package com.thaumaturgicconcilium.rift;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** TC4 {@code LithographerContainer}: four Astral Monitor slots in a diamond, the player inventory below. */
public class LithographerMenu extends AbstractContainerMenu {
    private static final int[][] SLOT_XY = {{52, 30}, {80, 2}, {108, 30}, {80, 58}};

    private final Container monitors;
    private final @Nullable LithographerBlockEntity lithographer;

    public LithographerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, inventory.player.level().getBlockEntity(extra.readBlockPos()) instanceof LithographerBlockEntity be ? be : null);
    }

    public LithographerMenu(int id, Inventory inventory, @Nullable LithographerBlockEntity lithographer) {
        super(TCRifts.LITHOGRAPHER_MENU.get(), id);
        this.lithographer = lithographer;
        this.monitors = lithographer != null ? lithographer.monitors() : new SimpleContainer(LithographerBlockEntity.SLOTS);
        for (int i = 0; i < LithographerBlockEntity.SLOTS; i++) {
            addSlot(new Slot(monitors, i, SLOT_XY[i][0], SLOT_XY[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return AstralMonitorItem.isLoaded(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row$ = 0; row$ < 3; row$++) {
            for (int col$ = 0; col$ < 9; col$++) {
                addSlot(new net.minecraft.world.inventory.Slot(inventory, col$ + row$ * 9 + 9, 8 + col$ * 18, 84 + row$ * 18));
            }
        }
        for (int col$ = 0; col$ < 9; col$++) {
            addSlot(new net.minecraft.world.inventory.Slot(inventory, col$, 8 + col$ * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = LithographerBlockEntity.SLOTS;
        if (index < own) {
            if (!moveItemStackTo(stack, own, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!AstralMonitorItem.isLoaded(stack) || !moveItemStackTo(stack, 0, own, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return lithographer == null || (!lithographer.isRemoved()
                && player.distanceToSqr(lithographer.getBlockPos().getCenter()) <= 64.0);
    }
}
