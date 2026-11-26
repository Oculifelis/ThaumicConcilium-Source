package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gem of Rifts, active (TC4 {@code RiftGem} meta 1; the inert gem is a plain item a chained rift turns into this).
 * Hold use: every 4 seconds, with no rift within 16 blocks, it spends 10 Perditio and 10 Aer vis from a hotbar wand and
 * opens an interdimensional rift where you look. Sneak-use cycles the current cast; sneak-swing flips inflate/deflate.
 * Casts come from Terra-Cast Gems combined with it at a crafting grid ({@link TerraGemRecipe}).
 */
public class RiftGemItem extends Item {
    /** TC4 COST: 1000 centivis each. */
    public static final Map<ResourceKey<IAspect>, Integer> COST = costOf(TTAspects.PERDITIO, TTAspects.AER);
    static final int INTERVAL = 80;
    /** TC4 {@code count % 80 == 0} with Integer.MAX_VALUE use time fired first after 7 ticks. */
    static final int FIRST = 7;
    static final double NO_RIFT_RANGE = 16.0;

    public RiftGemItem(Properties props) {
        super(props);
    }

    static Map<ResourceKey<IAspect>, Integer> costOf(ResourceKey<IAspect> a, ResourceKey<IAspect> b) {
        Map<ResourceKey<IAspect>, Integer> m = new LinkedHashMap<>();
        m.put(a, 1000);
        m.put(b, 1000);
        return Map.copyOf(m);
    }

    /** An active rift gem in either hand of {@code player}, or empty. */
    public static ItemStack heldActive(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s.getItem() instanceof RiftGemItem) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                stack.set(TCRifts.RIFT_GEM_DATA.get(), stack.getOrDefault(TCRifts.RIFT_GEM_DATA.get(), RiftData.Gem.EMPTY).cycle());
            }
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return new InteractionResultHolder<>(InteractionResult.CONSUME, player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    static boolean due(ItemStack stack, LivingEntity user, int remaining, Item item) {
        int used = item.getUseDuration(stack, user) - remaining;
        return used % INTERVAL == FIRST;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayer player) || !due(stack, user, remaining, this)) {
            return;
        }
        ServerLevel server = (ServerLevel) player.level();
        if (!server.getEntitiesOfClass(RiftEntity.class, player.getBoundingBox().inflate(NO_RIFT_RANGE)).isEmpty()) {
            return;
        }
        if (WandVisHelper.consumeSpecificFromHotbar(player, COST, true)) {
            HitResult hit = player.pick(5.0, 1.0F, false);
            Vec3 at = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(5.0)) : hit.getLocation();
            server.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.ZAP.get(), SoundSource.PLAYERS, 1.0F,
                    1.0F + (server.getRandom().nextFloat() - server.getRandom().nextFloat()) * 0.2F);
            RiftUtil.bolt(server, player.getEyePosition().subtract(0, 0.2, 0), at, 0x333333, 0.3F);
            RiftEntity.summon(server, at.add(0, 1.5, 0));
        }
        player.stopUsingItem();
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Player player && player.isShiftKeyDown() && !player.level().isClientSide()) {
            stack.set(TCRifts.RIFT_GEM_DATA.get(), stack.getOrDefault(TCRifts.RIFT_GEM_DATA.get(), RiftData.Gem.EMPTY).toggleReverse());
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        costLines(tooltip::add, COST);
        RiftData.Gem gem = stack.getOrDefault(TCRifts.RIFT_GEM_DATA.get(), RiftData.Gem.EMPTY);
        tooltip.add(Component.translatable(gem.reverse() ? "tooltip.thaumaturgicconcilium.riftgem.deflate" : "tooltip.thaumaturgicconcilium.riftgem.inflate")
                .withStyle(ChatFormatting.GRAY));
        if (!gem.casts().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.riftgem", gem.casts().size(), RiftData.Gem.MAX_CASTS)
                    .withStyle(ChatFormatting.GRAY));
            gem.currentCast().ifPresent(c -> tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.terragem", castName(c))
                    .withStyle(ChatFormatting.GRAY)));
        }
    }

    static Component castName(RiftData.Cast cast) {
        return BuiltInRegistries.BLOCK.getOptional(cast.block()).map(b -> (Component) b.getName()).orElse(Component.literal(cast.block().toString()))
                .copy().append(Component.literal(" (" + cast.dimension().location() + ")").withStyle(ChatFormatting.DARK_GRAY));
    }

    static void costLines(Consumer<Component> tooltip, Map<ResourceKey<IAspect>, Integer> cost) {
        tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium.vis_cost").withStyle(ChatFormatting.GRAY));
        cost.forEach((aspect, centivis) -> tooltip.accept(Component.literal(" ").append(
                Component.translatable("aspect." + aspect.location().getNamespace() + "." + aspect.location().getPath()))
                .append(Component.literal(" x " + centivis / 100)).withStyle(ChatFormatting.GRAY)));
    }
}
