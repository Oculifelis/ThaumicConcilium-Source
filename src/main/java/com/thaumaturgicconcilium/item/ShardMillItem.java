package com.thaumaturgicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.thaumaturgicconcilium.TCDataMaps;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.entity.ShardPowderEntity;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.phys.Vec3;

/**
 * Shard Mill. TC4 shards became Thaumaturge vis crystals, so the mill grinds vis crystals.
 * <ul>
 * <li>Sneak-use opens or closes the hopper.</li>
 * <li>Open and empty, holding use for a second grinds one vis crystal it accepts: the one in your off hand, else the first in
 * your inventory (or a Forbidden Magic sin shard, see the {@code mill_shard} data map). A crystal makes 20 ticks of powder.</li>
 * <li>Closed, holding use sprays that powder (2-4 motes a tick). Each mote does its aspect's trick when it settles.</li>
 * </ul>
 * The Greatwood mill takes the aspects in {@code #thaumaturgicconcilium:greatwood_mill} (the six primals; any other aspect in
 * the tag sprays aspect orbs, like the old balanced shard); the Silverwood mill the ones in
 * {@code #thaumaturgicconcilium:silverwood_mill} (Forbidden Magic's sins, flux and desire).
 */
public class ShardMillItem extends Item {
    public static final TagKey<IAspect> GREATWOOD = TagKey.create(IAspect.REGISTRY_KEY,
            com.thaumaturgicconcilium.ThaumaturgicConcilium.id("greatwood_mill"));
    public static final TagKey<IAspect> SILVERWOOD = TagKey.create(IAspect.REGISTRY_KEY,
            com.thaumaturgicconcilium.ThaumaturgicConcilium.id("silverwood_mill"));
    private static final int TICKS_PER_CRYSTAL = 20;

    private final boolean silverwood;

    public ShardMillItem(Properties properties, boolean silverwood) {
        super(properties);
        this.silverwood = silverwood;
    }

    private static MillCharge charge(ItemStack stack) {
        return stack.getOrDefault(TCRegistry.MILL_CHARGE.get(), new MillCharge(TTAspects.AER, 0, false));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MillCharge c = charge(stack);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                stack.set(TCRegistry.MILL_CHARGE.get(), c.withOpen(!c.open()));
                level.playSound(null, player.blockPosition(), TCRegistry.SOUND_CLACK.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
        }
        if (c.open() ? c.ticks() > 0 || findCrystal(player).isEmpty() : c.ticks() <= 0) {
            return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return new InteractionResultHolder<>(InteractionResult.CONSUME, player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return charge(stack).open() ? TICKS_PER_CRYSTAL : 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        MillCharge c = charge(stack);
        int used = getUseDuration(stack, user) - remaining;
        if (c.open()) {
            if (used % 5 == 0) {
                server.playSound(null, user.blockPosition(), TCRegistry.SOUND_MILL.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            }
            return;
        }
        if (c.ticks() <= 0) {
            user.releaseUsingItem();
            return;
        }
        Vec3 look = user.getLookAngle();
        Vec3 from = user.getEyePosition().add(look.scale(0.8)).add(0, -0.2, 0);
        int motes = 2 + server.getRandom().nextInt(3);
        for (int i = 0; i < motes; i++) {
            ShardPowderEntity mote = new ShardPowderEntity(server, from.x, from.y, from.z, c.aspect(), user, 0);
            mote.setDeltaMovement(look.scale(0.5).add(server.getRandom().nextGaussian() * 0.08, server.getRandom().nextGaussian() * 0.05,
                    server.getRandom().nextGaussian() * 0.08));
            server.addFreshEntity(mote);
        }
        if (used % 4 == 0) {
            server.playSound(null, user.blockPosition(), TCRegistry.SOUND_SHARD_DROP.get(), SoundSource.PLAYERS, 0.4F, 1.4F);
        }
        stack.set(TCRegistry.MILL_CHARGE.get(), c.withTicks(c.ticks() - 1));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        MillCharge c = charge(stack);
        if (!level.isClientSide() && c.open() && c.ticks() <= 0 && user instanceof Player player) {
            findCrystal(player).ifPresent(crystal -> millAspect(player, crystal).ifPresent(aspect -> {
                crystal.consume(1, player);
                stack.set(TCRegistry.MILL_CHARGE.get(), new MillCharge(aspect, TICKS_PER_CRYSTAL, true));
            }));
        }
        return stack;
    }

    private Optional<ItemStack> findCrystal(Player player) {
        if (millAspect(player, player.getOffhandItem()).isPresent()) {
            return Optional.of(player.getOffhandItem());
        }
        for (ItemStack s : com.thaumaturgicconcilium.util.Inv.all(player.getInventory())) {
            if (millAspect(player, s).isPresent()) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /**
     * The aspect a stack grinds into, if this mill takes it: a vis crystal by its aspect, or an item in the
     * {@code mill_shard} data map (Forbidden Magic's sin shards). Either way the aspect must be in this mill's tag.
     */
    private Optional<ResourceKey<IAspect>> millAspect(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Optional<? extends Holder<IAspect>> holder;
        AspectInstance a = stack.get(TTDataComponents.CRYSTAL_ASPECT.get());
        if (a != null) {
            holder = Optional.of(a.aspect());
        } else {
            TCDataMaps.MillShard shard = stack.getItemHolder().getData(TCDataMaps.MILL_SHARD);
            holder = shard == null ? Optional.empty() : player.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(shard.aspect());
        }
        return holder.filter(h -> h.is(silverwood ? SILVERWOOD : GREATWOOD)).flatMap(Holder::unwrapKey);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.shard_mill").withStyle(ChatFormatting.GRAY));
        MillCharge c = charge(stack);
        tooltip.add(Component.translatable(c.open() ? "tooltip.thaumaturgicconcilium.mill_open" : "tooltip.thaumaturgicconcilium.mill_closed")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (c.ticks() > 0) {
            tooltip.add(Component.translatable("tooltip.thaumaturgicconcilium.mill_loaded",
                    Component.translatable("aspect." + c.aspect().location().getNamespace() + "." + c.aspect().location().getPath()),
                    c.ticks()).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
