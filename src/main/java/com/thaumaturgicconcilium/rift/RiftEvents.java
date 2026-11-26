package com.thaumaturgicconcilium.rift;

import com.thaumaturgicconcilium.TCDataMaps;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.casters.ICaster;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityJarNode;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.equipment.runic.RunicShieldState;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.spell.SpellUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Game events of the rift set (TC4 {@code TCEntityEventHandler} and {@code WandHandler}): a broken node opens a rift;
 * a wand builds the Hex and the Lithographer and puts a jarred node on the Hex; Xylography takes the crafter's name;
 * every 10 ticks a held Astral Monitor reads its player and a lithographed player gets the party status.
 */
public final class RiftEvents {
    public static final Identifier RESEARCH_HEX = ThaumaturgicConcilium.id("hex_of_predictability");
    public static final Identifier RESEARCH_LITHOGRAPHER = ThaumaturgicConcilium.id("lithographer");

    private RiftEvents() {
    }

    static void register(IEventBus bus) {
        bus.addListener(RiftEvents::onBreak);
        bus.addListener(RiftEvents::onRightClickBlock);
        bus.addListener(RiftEvents::onCrafted);
        bus.addListener(RiftEvents::onTooltip);
        bus.addListener(RiftEvents::onPlayerTick);
    }

    // ------------------------------------------------------------------ node break -> wild rift

    private static void onBreak(BreakBlockEvent event) {
        if (event.getPlayer() == null || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        // TC4's TileJarNode extended TileNode, so breaking a node jar opened a rift too; only free nodes do here
        if (!(level.getBlockEntity(pos) instanceof BlockEntityNode node) || node instanceof BlockEntityJarNode) {
            return;
        }
        TCRifts.RiftOpening opening = event.getState().getBlock().builtInRegistryHolder().getData(TCRifts.RIFT_OPENING);
        int chance = opening == null ? 0 : opening.chance();
        if (chance > level.getRandom().nextInt(100)) {
            RiftEntity.openWild(level, Vec3.atCenterOf(pos), node.getAspects().totalAmount());
        }
    }

    // ------------------------------------------------------------------ wand structures

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof ICaster)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        boolean handled = false;
        if (state.is(TCRifts.HEX_CENTER) && hexFits(level, pos)) {
            handled = true;
            if (level instanceof ServerLevel server) {
                formHex(server, pos, state, player, held);
            }
        } else if (state.is(TTBlocks.JAR_NODE.get()) && level.getBlockEntity(pos.below()) instanceof HexBlockEntity hex && !hex.hasRift()) {
            handled = true;
            if (level instanceof ServerLevel server) {
                riftFromJar(server, pos, hex);
            }
        } else if (state.is(TTBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get()) && lithographerAxis(level, pos) != null) {
            handled = true;
            if (level instanceof ServerLevel server) {
                formLithographer(server, pos, player, held);
            }
        }
        if (handled) {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** TC4 {@code checkStructure}, made strict: the eight around the centre are the ring, and no hex is within the 5x5. */
    private static boolean hexFits(Level level, BlockPos centre) {
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 0, 2))) {
            if (level.getBlockState(p).is(TCRifts.HEX_BLOCKS)) {
                return false;
            }
            int dx = Math.abs(p.getX() - centre.getX());
            int dz = Math.abs(p.getZ() - centre.getZ());
            if (dx <= 1 && dz <= 1 && (dx | dz) != 0 && !level.getBlockState(p).is(TCRifts.HEX_RING)) {
                return false;
            }
        }
        return true;
    }

    private static void formHex(ServerLevel level, BlockPos centre, BlockState centreState, Player player, ItemStack wand) {
        if (!KnowledgeAccess.of(player).isResearchComplete(RESEARCH_HEX)) {
            return;
        }
        BlockState ring = (player.isShiftKeyDown() ? TCRifts.HEX_THICK : TCRifts.HEX).get().defaultBlockState();
        if (!WandVisHelper.payCosts(wand, player, TCDataMaps.wandBuildCost(ring.getBlock()), true, true)) {
            player.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.not_enough_vis"));
            return;
        }
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
            if (!p.equals(centre)) {
                level.setBlock(p.immutable(), ring, Block.UPDATE_ALL);
            }
        }
        level.setBlock(centre, ring.setValue(HexBlock.MASTER, true), Block.UPDATE_ALL);
        if (level.getBlockEntity(centre) instanceof HexBlockEntity hex) {
            hex.setCentre(centreState);
        }
        level.sendParticles(ParticleTypes.END_ROD, centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5, 20, 1.0, 0.3, 1.0, 0.02);
        level.playSound(null, centre, TTSounds.WAND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** TC4: a wand on a jarred node standing on a free hex master (no Peeler of Matter about) opens a chained rift. */
    private static void riftFromJar(ServerLevel level, BlockPos jar, HexBlockEntity hex) {
        if (SpellUtil.entityType(RiftEntity.PEELER).map(t -> !level.getEntities(t, hex.riftArea(), e -> true).isEmpty()).orElse(false)) {
            return;
        }
        level.removeBlock(jar, false);
        RiftEntity.construct(level, jar.above(), hex.getBlockPos(), 25);
    }

    /** The axis the gold pillars stand on, or null when the frame is incomplete. */
    private static Direction.Axis lithographerAxis(Level level, BlockPos construct) {
        if (!level.getBlockState(construct.below()).is(TTBlocks.METAL_THAUMIUM_BLOCK.get())) {
            return null;
        }
        BlockState bed = level.getBlockState(construct.below(2));
        if (!(bed.getBlock() instanceof BedBlock) || bed.getValue(BedBlock.PART) != BedPart.HEAD) {
            return null;
        }
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Direction d = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            if (gold(level, construct.below().relative(d)) && gold(level, construct.below(2).relative(d))
                    && gold(level, construct.below().relative(d.getOpposite())) && gold(level, construct.below(2).relative(d.getOpposite()))) {
                return axis;
            }
        }
        return null;
    }

    private static boolean gold(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.GOLD_BLOCK);
    }

    private static void formLithographer(ServerLevel level, BlockPos construct, Player player, ItemStack wand) {
        Direction.Axis axis = lithographerAxis(level, construct);
        if (axis == null || !KnowledgeAccess.of(player).isResearchComplete(RESEARCH_LITHOGRAPHER)) {
            return;
        }
        if (!WandVisHelper.payCosts(wand, player, TCDataMaps.wandBuildCost(TCRifts.LITHOGRAPHER.get()), true, true)) {
            player.sendOverlayMessage(Component.translatable("message.thaumaturgicconcilium.not_enough_vis"));
            return;
        }
        Direction d = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        BlockPos body = construct.below();
        for (BlockPos p : List.of(construct, body.relative(d), body.below().relative(d), body.relative(d.getOpposite()),
                body.below().relative(d.getOpposite()))) {
            level.removeBlock(p, false);
        }
        level.setBlock(body, TCRifts.LITHOGRAPHER.get().defaultBlockState().setValue(LithographerBlock.AXIS, axis), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.END_ROD, body.getX() + 0.5, body.getY() + 1.0, body.getZ() + 0.5, 20, 0.6, 0.6, 0.6, 0.02);
        level.playSound(null, body, TTSounds.WAND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ xylography

    private static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack out = event.getCrafting();
        if (XylographyRecipe.BLANK.equals(out.get(TCRifts.XYLOGRAPHY.get()))) {
            out.set(TCRifts.XYLOGRAPHY.get(), event.getEntity().getName().getString());
        }
    }

    /** TC4 {@code tc.tooltip.xylography}: a carved wand shows whose it is. */
    private static void onTooltip(ItemTooltipEvent event) {
        String name = event.getItemStack().get(TCRifts.XYLOGRAPHY.get());
        if (name != null && !name.isBlank()) {
            event.getToolTip().add(Component.translatable("tooltip.thaumaturgicconcilium.xylography", name).withStyle(ChatFormatting.GOLD));
        }
    }

    // ------------------------------------------------------------------ monitor and party status

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        ItemStack monitor = player.getMainHandItem();
        if (monitor.getItem() instanceof AstralMonitorItem) {
            AstralMonitorItem.target(monitor).player().map(name -> player.level().getServer().getPlayerList().getPlayerByName(name))
                    .ifPresent(watched -> {
                        RiftData.Readings r = new RiftData.Readings(health(watched), ExtraReadings.blood(watched),
                                ExtraReadings.mana(watched), runes(watched));
                        if (!r.equals(monitor.get(TCRifts.ASTRAL_READINGS.get()))) {
                            monitor.set(TCRifts.ASTRAL_READINGS.get(), r);
                        }
                    });
        }
        RiftData.Lithograph lith = player.getData(TCRifts.LITHOGRAPH);
        if (!lith.showing()) {
            return;
        }
        List<Integer> health = new ArrayList<>();
        List<Integer> blood = new ArrayList<>();
        List<Integer> mana = new ArrayList<>();
        List<Integer> runes = new ArrayList<>();
        List<Integer> brains = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            RiftData.Target t = i < lith.monitored().size() ? lith.monitored().get(i) : RiftData.Target.NONE;
            ServerPlayer watched = t.player().map(n -> player.level().getServer().getPlayerList().getPlayerByName(n)).orElse(null);
            health.add(watched == null ? -1 : health(watched));
            // Blood and mana come from optional compat (Neo Vitae, Ars Magica Legacy); without them the glyphs stay dark, as TC4
            blood.add(ExtraReadings.blood(watched));
            mana.add(ExtraReadings.mana(watched));
            runes.add(watched == null ? -1 : runes(watched));
            brains.add(t.pos().map(gp -> brain(player, gp)).orElse(-1));
        }
        PacketDistributor.sendToPlayer(player, new PartyStatusPayload(health, blood, mana, runes, brains));
    }

    private static int health(Player p) {
        return Math.round(p.getHealth() / p.getMaxHealth() * 10.0F);
    }

    /** TC4 {@code round(charge / max * 10) - 1}: 0..9, -1 for none (TC4 sent 0 for players without shielding). */
    private static int runes(Player p) {
        RunicShieldState shield = p.getData(TTAttachments.RUNIC_SHIELD);
        if (shield == null || shield.maxCharge <= 0) {
            return -1;
        }
        float charge = Math.min(p.getAbsorptionAmount(), shield.maxCharge);
        return Mth.clamp(Math.round(charge / shield.maxCharge * 10.0F) - 1, -1, 9);
    }

    /**
     * A watched place's 4-bit brain value (TC4: the Red-Powered Mind's power): a {@link AstralHooks.BrainSource} block's
     * own reading (the Red-Powered Mind, phase 4 compat), otherwise the strongest redstone signal the block there gives out.
     */
    private static int brain(ServerPlayer player, net.minecraft.core.GlobalPos gp) {
        ServerLevel level = player.level().getServer().getLevel(gp.dimension());
        if (level == null || !level.isLoaded(gp.pos())) {
            return -1;
        }
        BlockState state = level.getBlockState(gp.pos());
        if (state.getBlock() instanceof AstralHooks.BrainSource brain) {
            return brain.brainPower(level, gp.pos());
        }
        int best = 0;
        for (Direction d : Direction.values()) {
            best = Math.max(best, state.getSignal(level, gp.pos(), d));
        }
        return best;
    }
}
