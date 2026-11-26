package com.thaumaturgicconcilium.item;

import com.thaumaturgicconcilium.entity.ThrownConciliumBottle;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

/** Bottle of Clear Water and Bottle of Thick Taint: thrown like a splash potion (pitch raised 20 degrees, speed 0.5). */
public class ConciliumBottleItem extends Item implements ProjectileItem {
    public enum Kind { CLEAR_WATER, THICK_TAINT }

    private final Kind kind;

    public ConciliumBottleItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel server) {
            ThrownConciliumBottle bottle = new ThrownConciliumBottle(server, player, stack);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            server.addFreshEntity(bottle);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new ThrownConciliumBottle(level, pos.x(), pos.y(), pos.z(), stack);
    }
}
