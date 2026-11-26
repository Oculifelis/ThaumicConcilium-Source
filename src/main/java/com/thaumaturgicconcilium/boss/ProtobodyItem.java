package com.thaumaturgicconcilium.boss;

import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Protobody (TC4 Concilium): an etheric copy of a body. Dropped, it becomes a {@link ProtobodyItemEntity} (2 s pickup delay). */
public class ProtobodyItem extends Item {
    public ProtobodyItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public @Nullable Entity createEntity(Level level, Entity location, ItemStack stack) {
        ProtobodyItemEntity item = new ProtobodyItemEntity(level, location.getX(), location.getY(), location.getZ(), stack);
        item.setDeltaMovement(location.getDeltaMovement());
        item.setPickUpDelay(40);
        return item;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.protobody").withStyle(ChatFormatting.GRAY));
    }
}
