package com.thaumaturgicconcilium.mob;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Thaumaturge's trade table (TC4 {@code ContainerThaumaturge}): one offer slot and four payout slots, laid out like
 * Thaumaturge's Pech screen so it can share that sheet. The trade button runs {@link #trade}.
 */
public class ThaumaturgeMenu extends AbstractContainerMenu {
    public static final int TRADE_BUTTON = 0;
    private static final int OFFER = 0;
    private static final int PAYOUTS = 4;
    private static final int TABLE = 1 + PAYOUTS;

    private final @Nullable ThaumaturgeEntity trader;
    private final Container table = new SimpleContainer(TABLE);

    public ThaumaturgeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, resolve(inventory, extra.readVarInt()));
    }

    public ThaumaturgeMenu(int id, Inventory inventory, @Nullable ThaumaturgeEntity trader) {
        super(TCMobs.THAUMATURGE_MENU.get(), id);
        this.trader = trader;
        addSlot(new Slot(table, OFFER, 36, 29));
        for (int cell = 0; cell < PAYOUTS; cell++) {
            addSlot(new Slot(table, 1 + cell, 106 + cell % 2 * 18, 20 + cell / 2 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
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

    private static @Nullable ThaumaturgeEntity resolve(Inventory inventory, int entityId) {
        Entity e = inventory.player.level().getEntity(entityId);
        return e instanceof ThaumaturgeEntity t ? t : null;
    }

    /** The client can only see that something is offered and the payout slots are clear; the server decides the rest. */
    public boolean canTrade() {
        if (table.getItem(OFFER).isEmpty()) {
            return false;
        }
        for (int i = 1; i < TABLE; i++) {
            if (!table.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == TRADE_BUTTON) {
            if (player instanceof ServerPlayer sp && trader != null && canTrade()) {
                trade(sp, trader);
            }
            return true;
        }
        return super.clickMenuButton(player, button);
    }

    private void trade(ServerPlayer player, ThaumaturgeEntity thaumaturge) {
        ServerLevel level = (ServerLevel) player.level();
        ThaumaturgeTrade trade = ThaumaturgeTrade.get(level, ThaumaturgeTrade.THAUMATURGE).orElse(null);
        if (trade == null) {
            return;
        }
        RandomSource random = level.getRandom();
        ItemStack offer = table.getItem(OFFER);
        if (trade.isHintInput(offer)) {
            if (MobUtil.giveHint(player, trade.hints(), random)) {
                offer.shrink(1);
                table.setChanged();
            }
            broadcastChanges();
            return;
        }
        int value = trade.valueOf(offer);
        if (value <= 0) {
            MobUtil.chat(player, "message.thaumaturgicconcilium.thaumaturge.refuse", 1, random);
            return;
        }
        if (random.nextFloat() < trade.raiseChance()) {
            value += random.nextInt(2);
        } else if (random.nextFloat() < trade.lowerChance()) {
            value -= random.nextInt(2);
        }
        offer.shrink(1);
        List<ItemStack> payout = roll(trade, value, random);
        for (ItemStack stack : payout) {
            if (!shelve(stack)) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        table.setChanged();
        thaumaturge.playSound(SoundEvents.VILLAGER_TRADE, 0.6F, 0.8F);
        MobUtil.chat(player, "message.thaumaturgicconcilium.thaumaturge.trade", 3, random);
        broadcastChanges();
    }

    /** TC4's walk: outputs in weighted random order; each gives while value > rand(10), the walk stops at value < rand(10). */
    private static List<ItemStack> roll(ThaumaturgeTrade trade, int value, RandomSource random) {
        List<ThaumaturgeTrade.Output> pool = new ArrayList<>(trade.outputs());
        List<ItemStack> out = new ArrayList<>();
        while (!pool.isEmpty() && out.size() < PAYOUTS) {
            int total = pool.stream().mapToInt(ThaumaturgeTrade.Output::weight).sum();
            int r = random.nextInt(total);
            ThaumaturgeTrade.Output pick = pool.get(0);
            for (ThaumaturgeTrade.Output o : pool) {
                r -= o.weight();
                if (r < 0) {
                    pick = o;
                    break;
                }
            }
            pool.remove(pick);
            if (value > random.nextInt(10)) {
                out.add(pick.result().copy());
            }
            if (value < random.nextInt(10)) {
                break;
            }
        }
        return out;
    }

    private boolean shelve(ItemStack stack) {
        for (int i = 1; i < TABLE; i++) {
            ItemStack held = table.getItem(i);
            if (held.isEmpty()) {
                table.setItem(i, stack);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() + stack.getCount() <= held.getMaxStackSize()) {
                held.grow(stack.getCount());
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack moving = slot.getItem();
        if (moving.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = moving.copy();
        boolean moved = index >= TABLE ? moveItemStackTo(moving, OFFER, OFFER + 1, false) : moveItemStackTo(moving, TABLE, TABLE + 36, true);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, moving);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return trader != null && trader.isAlive() && !trader.isAngry() && player.canInteractWithEntity(trader, 4.0);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            for (int i = 0; i < TABLE; i++) {
                ItemStack left = table.removeItemNoUpdate(i);
                if (!left.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(left);
                }
            }
        }
    }
}
