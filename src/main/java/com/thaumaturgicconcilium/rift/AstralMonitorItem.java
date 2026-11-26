package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Astral Monitor (TC4 {@code AstralMonitor}): sneak-use to put a xylographed wand (or, from phase 4, an item with an
 * {@code astral_target}) into it. Held, it shows the named player's health, blood, mana and runic shielding as
 * constellations (data component {@code astral_readings}, refreshed every 10 ticks on the server); in a Lithographer it
 * becomes one of the four targets of the imprint. The wand is kept in the vanilla {@code container} component.
 */
public class AstralMonitorItem extends Item {
    public AstralMonitorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new AstralMonitorMenu(id, inv, hand), player.getItemInHand(hand).getHoverName()),
                    buf -> buf.writeEnum(hand));
        }
        return InteractionResult.SUCCESS;
    }

    /** The item inside, or empty. */
    public static ItemStack inside(ItemStack monitor) {
        return monitor.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    public static void setInside(ItemStack monitor, ItemStack content) {
        monitor.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(content.copyWithCount(Math.min(1, content.getCount())))));
    }

    /** What the Astral Monitor's slot takes: a wand carrying a name, or anything carrying an astral target. */
    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.has(TCRifts.ASTRAL_TARGET.get()) || AstralHooks.target(stack).isPresent()) {
            return true;
        }
        return stack.getItem() instanceof ItemWand && named(stack).isPresent();
    }

    /** TC4 {@code Xylography}: the name on the wand, blank until the crafting player is known. */
    public static Optional<String> named(ItemStack wand) {
        return Optional.ofNullable(wand.get(TCRifts.XYLOGRAPHY.get())).map(String::trim).filter(s -> !s.isEmpty());
    }

    /** A monitor holding a usable wand or target (TC4 Lithographer slot check). */
    public static boolean isLoaded(ItemStack stack) {
        return stack.getItem() instanceof AstralMonitorItem && accepts(inside(stack));
    }

    public static RiftData.Target target(ItemStack monitor) {
        if (!(monitor.getItem() instanceof AstralMonitorItem)) {
            return RiftData.Target.NONE;
        }
        ItemStack in = inside(monitor);
        Optional<String> name = named(in);
        if (name.isPresent()) {
            return new RiftData.Target(name, Optional.empty());
        }
        GlobalPos pos = in.get(TCRifts.ASTRAL_TARGET.get());
        if (pos == null) {
            pos = AstralHooks.target(in).orElse(null);
        }
        return pos == null ? RiftData.Target.NONE : new RiftData.Target(Optional.empty(), Optional.of(pos));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RiftData.Target target = target(stack);
        target.player().ifPresent(n -> tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium.monitor", n).withStyle(ChatFormatting.GRAY)));
        target.pos().ifPresent(p -> tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium.monitor_pos", p.pos().getX(), p.pos().getY(),
                p.pos().getZ()).withStyle(ChatFormatting.GRAY)));
    }
}
