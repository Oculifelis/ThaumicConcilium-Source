package com.thaumaturgicconcilium.boss;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pontifex's Hammer (TC4 Concilium {@code PontifexHammer}): an epic sword-class weapon (18 damage, 1000 uses, warp 3)
 * that mends one point a second. Held down (right click), once a second it drains every living thing within 6 blocks
 * across and 2 up or down: magic damage equal to the wielder's attack damage, and for each one hurt the hammer takes 2
 * wear and the wielder heals 2.
 */
public class PontifexHammerItem extends Item {
    private static final DustParticleOptions DRAIN = new DustParticleOptions(0xB01020, 1.0F);

    public PontifexHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (stack.isDamaged() && owner instanceof LivingEntity && owner.tickCount % 20 == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (remaining % 20 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        float damage = (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(6.0, 2.0, 6.0),
                e -> e != user && e.isAlive())) {
            if (e.hurtServer(server, server.damageSources().indirectMagic(user, user), damage)) {
                stack.hurtAndBreak(2, user, user.getUsedItemHand());
                user.heal(2.0F);
            }
            Vec3 to = user.position().add(0.0, user.getBbHeight() * 0.6, 0.0);
            Vec3 from = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            for (int i = 0; i < 6; i++) {
                Vec3 p = from.lerp(to, i / 6.0);
                server.sendParticles(DRAIN, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
            }
            if (stack.isEmpty()) {
                return;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium.pontifex_hammer").withStyle(ChatFormatting.GRAY));
    }
}
