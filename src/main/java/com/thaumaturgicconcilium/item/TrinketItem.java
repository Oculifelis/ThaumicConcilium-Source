package com.thaumaturgicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.warp.IPlayerWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.TCUtil;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The three worn Concilium trinkets (Curios; the slot comes from the item's Curios tag).
 * <ul>
 * <li>Ring of Blustering Light (ring): 50 runic shielding; every 3 ticks adds 1 centivis of a random primal to a hotbar
 * wand with room. Wearing it is what lets Spotlight, Warp Extraction and Alchemist's Spree work.</li>
 * <li>Burdening Amulet (necklace): every second your tamed creatures within 32 blocks attack what you look at. While you
 * carry temporary warp (and no Warp Ward) it gives you Deadly Gaze and slowly burns that warp off.</li>
 * <li>Tight Belt (belt): while you carry temporary warp (and no Warp Ward) it gives you Thaumorrhea and now and then
 * squeezes a point of warp out as flux gas over your head.</li>
 * </ul>
 */
public class TrinketItem extends Item implements ICurioItem {
    private static final String WARP_WARD = "thaumaturge:warp_ward";

    public TrinketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void curioTick(SlotContext slot, ItemStack stack) {
        if (!(slot.entity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (stack.is(TCRegistry.RING_OF_BLUSTERING_LIGHT.get())) {
            if (player.tickCount % 3 == 0) {
                ResourceKey<IAspect> primal = TTAspects.PRIMALS.get(player.getRandom().nextInt(TTAspects.PRIMALS.size()));
                ItemStack wand = WandVisHelper.findWandInHotbarWithRoom(player, primal, 1);
                if (!wand.isEmpty()) {
                    WandVisHelper.addRealVis(wand, primal, 1, true);
                }
            }
        } else if (stack.is(TCRegistry.BURDENING_AMULET.get())) {
            if (player.tickCount % 20 == 0) {
                directPets(level, player);
            }
            warpStrain(player, "thaumaturge:death_gaze", WarpHelper.getWarp(player).get(WarpType.TEMPORARY), 10, false);
        } else if (stack.is(TCRegistry.TIGHT_BELT.get())) {
            warpStrain(player, "thaumaturge:thaumarhia", WarpHelper.getActualWarp(player), 25, true);
        }
    }

    private static void directPets(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getLookAngle().scale(32);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, eye.add(reach),
                player.getBoundingBox().expandTowards(reach).inflate(1), e -> e instanceof LivingEntity l && l.isAlive(), 0F);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        for (Mob pet : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32))) {
            if (pet != target && pet instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID() == null ? null
                    : own.getOwnerUUID())) {
                pet.setTarget(target);
            }
        }
    }

    /** Warp strain: an effect scaled by warp/15 (max IV), and a 1-in-{@code chance} tick roll that spends a point of temporary warp. */
    private static void warpStrain(ServerPlayer player, String effect, int warp, int chance, boolean fluxGas) {
        IPlayerWarp pw = WarpHelper.getWarp(player);
        int temporary = pw.get(WarpType.TEMPORARY);
        if (temporary <= 0 || TCUtil.effect(WARP_WARD).map(player::hasEffect).orElse(false)) {
            return;
        }
        TCUtil.giveEffect(player, effect, 60, Math.min(3, warp / 15));
        if (player.getRandom().nextInt(chance) == 0) {
            pw.reduce(WarpType.TEMPORARY, 1);
            if (fluxGas) {
                BlockPos above = player.blockPosition().above(2);
                if (player.level().getBlockState(above).isAir()) {
                    TCUtil.block("thaumaturge:flux_gas").ifPresent(b -> player.level().setBlockAndUpdate(above, b.defaultBlockState()));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium." + BuiltInRegistries.ITEM.getKey(this).getPath())
                .withStyle(ChatFormatting.GRAY));
    }
}
