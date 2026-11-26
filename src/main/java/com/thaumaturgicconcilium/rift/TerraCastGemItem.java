package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Terra-Cast Gem, active (TC4 {@code TerraCastGem} meta 1). Hold use on a solid, breakable block that has Terra: every
 * 4 seconds, with no rift within 16 blocks, it spends 10 Terra and 10 Aer vis, tears the block out of the world and
 * remembers it and its dimension (data component {@code terra_cast}). Combined with an active Gem of Rifts, the cast
 * becomes one of the rift gem's destinations.
 */
public class TerraCastGemItem extends Item {
    public static final Map<ResourceKey<IAspect>, Integer> COST = RiftGemItem.costOf(TTAspects.TERRA, TTAspects.AER);

    public TerraCastGemItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayer player) || !RiftGemItem.due(stack, user, remaining, this)) {
            return;
        }
        ServerLevel server = player.level();
        if (!server.getEntitiesOfClass(RiftEntity.class, player.getBoundingBox().inflate(RiftGemItem.NO_RIFT_RANGE)).isEmpty()) {
            return;
        }
        HitResult hit = player.pick(5.0, 1.0F, false);
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = bhr.getBlockPos();
        BlockState state = server.getBlockState(pos);
        if (!state.blocksMotion() || state.getDestroySpeed(server, pos) < 0.0F || !hasTerra(server, state)) {
            return;
        }
        // TC4 also spent the vis on the client; only the server pays here
        if (!WandVisHelper.consumeSpecificFromHotbar(player, COST, true)) {
            return;
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.ZAP.get(), SoundSource.PLAYERS, 1.0F,
                1.0F + (server.getRandom().nextFloat() - server.getRandom().nextFloat()) * 0.2F);
        stack.set(TCRifts.TERRA_CAST.get(), new RiftData.Cast(BuiltInRegistries.BLOCK.getKey(state.getBlock()), server.dimension()));
        Vec3 at = Vec3.atCenterOf(pos);
        RiftUtil.bolt(server, player.getEyePosition().subtract(0, 0.2, 0), at, 0x00FF00, 0.3F);
        server.levelEvent(2001, pos, Block.getId(state));
        server.removeBlock(pos, false);
    }

    private static boolean hasTerra(ServerLevel level, BlockState state) {
        ItemStack asItem = new ItemStack(state.getBlock());
        return !asItem.isEmpty() && AspectIndexAccess.of(asItem).amountOf(TTAspects.TERRA, level.registryAccess()) > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RiftGemItem.costLines(tooltip, COST);
        RiftData.Cast cast = stack.get(TCRifts.TERRA_CAST.get());
        if (cast != null) {
            tooltip.accept(Component.translatable("tooltip.thaumaturgicconcilium.terragem", RiftGemItem.castName(cast)).withStyle(ChatFormatting.GRAY));
        }
    }
}
