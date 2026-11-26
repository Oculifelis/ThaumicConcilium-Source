package com.thaumaturgicconcilium.rift;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * TC4 {@code AstralMonitorContainer}: one slot (78, 30) for the wand, kept in the held monitor's {@code container}
 * component. The monitor's own inventory slot is locked while the screen is open.
 */
public class AstralMonitorMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final int lockedSlot;
    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            save();
        }
    };

    public AstralMonitorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, extra.readEnum(InteractionHand.class));
    }

    public AstralMonitorMenu(int id, Inventory inventory, InteractionHand hand) {
        super(TCRifts.ASTRAL_MONITOR_MENU.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        this.lockedSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;
        input.setItem(0, AstralMonitorItem.inside(player.getItemInHand(hand)));
        addSlot(new Slot(input, 0, 78, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AstralMonitorItem.accepts(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            boolean locked = col == lockedSlot;
            addSlot(new Slot(inventory, col, 8 + col * 18, 142) {
                @Override
                public boolean mayPickup(Player p) {
                    return !locked && super.mayPickup(p);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !locked && super.mayPlace(stack);
                }
            });
        }
    }

    private void save() {
        ItemStack monitor = player.getItemInHand(hand);
        if (monitor.getItem() instanceof AstralMonitorItem && !player.level().isClientSide()) {
            AstralMonitorItem.setInside(monitor, input.getItem(0));
            monitor.remove(TCRifts.ASTRAL_READINGS.get());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!AstralMonitorItem.accepts(stack) || !moveItemStackTo(stack, 0, 1, false)) {
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
    public boolean stillValid(Player p) {
        return p.getItemInHand(hand).getItem() instanceof AstralMonitorItem;
    }
}
