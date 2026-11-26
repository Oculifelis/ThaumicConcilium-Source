package com.thaumaturgicconcilium.compat.horizons;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.golems.IGolemAPI;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.CompatBContent;
import com.thaumaturgicconcilium.compat.CompatBContent.Drainage;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drainage Syringe (TC4 Concilium {@code DrainageSyringe}, vis mode). Use it on a creature within 3 blocks and keep holding:
 * every 2 seconds a creature above half health takes a tenth of its health and the syringe gains 10 vis (up to 100);
 * a player stabbed this way also loses 50 vis of each primal from the wands they carry. Sneak-use to take the cap off;
 * then holding use pours 10 vis of a random primal into a wand on the hotbar every second. Golems have no blood to give.
 * The original's mana and blood modes needed Botania and Blood Magic and are not ported.
 */
public class DrainageSyringeItem extends Item {
    public static final int MAX_VIS = 100;
    private static final int DRAW = 10;
    private static final int DRAW_PERIOD = 40;
    private static final int FEED_PERIOD = 20;
    private static final double REACH = 3.0;
    /** TC4 {@code VIS_COST}: 5000 centivis (50 vis) of every primal, taken from a stabbed player's wands. */
    private static final int PLAYER_DRAIN_CENTIVIS = 5000;
    private static final SoundEvent STAB = SoundEvent.createVariableRangeEvent(ThaumaturgicConcilium.id("stab"));

    public DrainageSyringeItem(Properties props) {
        super(props);
    }

    public static Drainage data(ItemStack stack) {
        return stack.getOrDefault(CompatBContent.DRAINAGE.get(), Drainage.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Drainage d = data(stack);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                stack.set(CompatBContent.DRAINAGE.get(), new Drainage(d.amount(), !d.open()));
            }
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
        }
        if (d.open()) {
            player.startUsingItem(hand);
            return new InteractionResultHolder<>(InteractionResult.CONSUME, player.getItemInHand(hand));
        }
        if (pointed(player) instanceof LivingEntity) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), STAB, SoundSource.PLAYERS, 1.0F, level.getRandom().nextFloat());
            player.startUsingItem(hand);
            return new InteractionResultHolder<>(InteractionResult.CONSUME, player.getItemInHand(hand));
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)) {
            return;
        }
        int used = getUseDuration(stack, user) - remaining;
        Drainage d = data(stack);
        if (!d.open()) {
            if (used > 0 && used % DRAW_PERIOD == 0 && pointed(player) instanceof LivingEntity living) {
                draw(server, player, stack, d, living);
            }
        } else if (used > 0 && used % FEED_PERIOD == 0 && d.amount() > 0) {
            feed(server, player, stack, d);
        }
    }

    private static void draw(ServerLevel level, Player player, ItemStack stack, Drainage d, LivingEntity living) {
        if (living instanceof AbstractGolem || living instanceof IGolemAPI || living.getHealth() <= living.getMaxHealth() / 2.0F) {
            return;
        }
        if (!living.hurt(level.damageSources().playerAttack(player), living.getMaxHealth() / 10.0F)) {
            return;
        }
        stack.set(CompatBContent.DRAINAGE.get(), new Drainage(Mth.clamp(d.amount() + DRAW, 0, MAX_VIS), false));
        if (living instanceof Player victim) {
            drainWands(victim);
        }
    }

    /** TC4 {@code consumeVisFromInventory(VIS_COST)}: up to 50 vis of each primal out of the victim's wands. */
    private static void drainWands(Player victim) {
        for (ResourceKey<IAspect> aspect : TTAspects.PRIMALS) {
            int left = PLAYER_DRAIN_CENTIVIS;
            for (ItemStack wand : com.thaumaturgicconcilium.util.Inv.all(victim.getInventory())) {
                if (left <= 0) {
                    break;
                }
                if (!(wand.getItem() instanceof ItemWand)) {
                    continue;
                }
                int have = WandVisHelper.getVis(wand, aspect);
                int take = Math.min(have, left);
                if (take > 0) {
                    WandVisHelper.storeVis(wand, aspect, have - take);
                    left -= take;
                }
            }
        }
    }

    /**
     * Open: 10 vis of a random primal into a hotbar wand with room. TC4 emptied the syringe by 10 even when no wand
     * could take the vis, losing it; here the vis stays in the syringe until a wand has room.
     */
    private static void feed(ServerLevel level, Player player, ItemStack stack, Drainage d) {
        List<ResourceKey<IAspect>> primals = TTAspects.PRIMALS;
        ResourceKey<IAspect> aspect = primals.get(level.getRandom().nextInt(primals.size()));
        ItemStack wand = WandVisHelper.findWandInHotbarWithRoom(player, aspect, DRAW);
        if (wand.isEmpty()) {
            return;
        }
        WandVisHelper.addVis(wand, aspect, DRAW, true);
        stack.set(CompatBContent.DRAINAGE.get(), new Drainage(Math.max(0, d.amount() - DRAW), true));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F,
                level.getRandom().nextFloat());
    }

    /** TC4 {@code EntityUtils.getPointedEntity(..., 3.0, 0.1)}: the living thing the player looks at within 3 blocks. */
    private static @Nullable Entity pointed(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).scale(REACH);
        Vec3 end = eye.add(look);
        AABB box = player.getBoundingBox().expandTowards(look).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e.isPickable() && !e.isSpectator(), REACH * REACH);
        return hit == null ? null : hit.getEntity();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Drainage d = data(stack);
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.drainage_syringe.amount", d.amount(), MAX_VIS)
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable(d.open() ? "tooltip.thaumaturgicconcilium.drainage_syringe.open"
                : "tooltip.thaumaturgicconcilium.drainage_syringe.closed").withStyle(ChatFormatting.GRAY));
    }
}
