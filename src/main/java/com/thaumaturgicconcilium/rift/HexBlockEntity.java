package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.TCUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The master of a Hex of Predictability (TC4 {@code HexOfPredictabilityTile}). It watches the space above it for a
 * chained rift. While one is there, Vis Condensers below charged crystals pour essentia into it (and heat it to 1200);
 * a single item thrown into the rift area becomes the result of the Chained Rift recipe the essentia and the thrower's
 * research allow. While the hex is hot it eats Void Slag lying near (+2 heat a tick each), spits slag back out at the
 * lone item, and at 2400 heat the rift collapses.
 */
public class HexBlockEntity extends BlockEntity {
    public static final int HEAT_COLLAPSE = 2400;
    public static final int HEAT_CONDENSER = 1200;
    private static final Codec<Map<ResourceKey<IAspect>, Integer>> ESSENTIA = ChainedRiftRecipe.ASPECTS;

    private final Map<ResourceKey<IAspect>, Integer> essentia = new HashMap<>();
    private int heat;
    private boolean hasRift;
    private BlockState centre = Blocks.BEDROCK.defaultBlockState();
    private boolean dissolving;

    public HexBlockEntity(BlockPos pos, BlockState state) {
        super(TCRifts.HEX_BE.get(), pos, state);
    }

    public Map<ResourceKey<IAspect>, Integer> essentia() {
        return essentia;
    }

    public int heat() {
        return heat;
    }

    public boolean hasRift() {
        return hasRift;
    }

    void setCentre(BlockState centre) {
        this.centre = centre;
        setChanged();
    }

    /** Vis Condenser fed this hex: TC4 {@code sendEssentia}. */
    public void receive(ResourceKey<IAspect> aspect, int amount) {
        RiftUtil.add(essentia, aspect, amount);
        heat = HEAT_CONDENSER;
        changed();
    }

    /** The space a chained rift of this hex lives in (TC4: x-1..x+1, y..y+4 around the block corner). */
    public AABB riftArea() {
        return new AABB(worldPosition.getX() - 1.0, worldPosition.getY(), worldPosition.getZ() - 1.0, worldPosition.getX() + 2.0,
                worldPosition.getY() + 5.0, worldPosition.getZ() + 2.0);
    }

    public static Optional<HexBlockEntity> findMaster(Level level, BlockPos around) {
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-1, 0, -1), around.offset(1, 0, 1))) {
            if (level.getBlockEntity(p) instanceof HexBlockEntity hex) {
                return Optional.of(hex);
            }
        }
        return Optional.empty();
    }

    /** Turns the 3x3 back into the ring of Ancient Stone around the original centre block. */
    public void dissolve() {
        if (dissolving || level == null) {
            return;
        }
        dissolving = true;
        BlockState ring = TCUtil.block("thaumaturge:stone_ancient").map(Block::defaultBlockState).orElse(Blocks.STONE_BRICKS.defaultBlockState());
        BlockState middle = centre;
        BlockPos at = worldPosition;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 0, 1))) {
            if (!p.equals(at) && level.getBlockState(p).is(TCRifts.HEX_BLOCKS)) {
                level.setBlock(p.immutable(), ring, Block.UPDATE_ALL);
            }
        }
        level.setBlock(at, middle, Block.UPDATE_ALL);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexBlockEntity hex) {
        if (level instanceof ServerLevel server) {
            hex.tick(server);
        }
    }

    private boolean ringIntact() {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-1, 0, -1), worldPosition.offset(1, 0, 1))) {
            if (!p.equals(worldPosition) && !level.getBlockState(p).is(TCRifts.HEX_BLOCKS)) {
                return false;
            }
        }
        return true;
    }

    private void tick(ServerLevel server) {
        long time = server.getGameTime();
        if (time % 20 == 0 && !ringIntact()) {
            dissolve();
            return;
        }
        if (time % 60 == 0) {
            boolean rift = !server.getEntitiesOfClass(RiftEntity.class, riftArea()).isEmpty();
            if (rift != hasRift) {
                hasRift = rift;
                if (!rift) {
                    essentia.clear();
                }
                changed();
            }
        }
        if (!hasRift) {
            return;
        }
        if (heat >= HEAT_COLLAPSE) {
            List<RiftEntity> rifts = server.getEntitiesOfClass(RiftEntity.class, riftArea());
            if (!rifts.isEmpty()) {
                rifts.get(0).setCollapsing(0);
                hasRift = false;
                heat = 0;
                essentia.clear();
                changed();
                return;
            }
        }
        BlockPos p = worldPosition;
        if (!essentia.isEmpty()) {
            AABB near = new AABB(p.getX() - 8.0, p.getY() - 3.0, p.getZ() - 8.0, p.getX() + 9.0, p.getY() + 3.0, p.getZ() + 9.0);
            int slag = server.getEntitiesOfClass(ItemEntity.class, near, e -> e.getItem().is(TCRegistry.VOID_SLAG.get())).size();
            if (slag > 0) {
                heat += 2 * slag;
                setChanged();
            }
        }
        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class,
                new AABB(p.getX() - 2.0, p.getY(), p.getZ() - 2.0, p.getX() + 3.0, p.getY() + 4.0, p.getZ() + 3.0));
        if (items.size() != 1 || !items.get(0).isAlive()) {
            return;
        }
        ItemEntity item = items.get(0);
        if (heat > 0) {
            if (time % 60 == 0 && server.getRandom().nextInt(10) >= 8) {
                int count = 3 + server.getRandom().nextInt(3);
                for (int i = 0; i < count; i++) {
                    ItemEntity spat = new ItemEntity(server, p.getX() + (-0.5 + server.getRandom().nextDouble()) * 2.0, p.getY() + 2,
                            p.getZ() + (-0.5 + server.getRandom().nextDouble()) * 2.0, new ItemStack(TCRegistry.VOID_SLAG.get()));
                    launch(spat, 2.0, 0.2F);
                    server.addFreshEntity(spat);
                }
            }
            heat = Math.max(0, heat - 3);
            changed();
            return;
        }
        if (!(item.getOwner() instanceof Player thrower)) {
            return;
        }
        ItemStack thrown = item.getItem();
        ChainedRiftRecipe.find(server, thrower, essentia, thrown).flatMap(ChainedRiftRecipe::output).ifPresent(result -> {
            ItemStack catalyst = thrown.copy();
            item.discard();
            ItemEntity out = new ItemEntity(server, item.getX(), item.getY(), item.getZ(), result.copy());
            EventHooks.firePlayerCraftingEvent(thrower, out.getItem(), new SimpleContainer(catalyst));
            server.sendParticles(ParticleTypes.END_ROD, out.getX(), out.getY(), out.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            launch(out, 5.0, 0.4F);
            server.addFreshEntity(out);
            essentia.clear();
            changed();
        });
    }

    /** TC4 {@code MiscHelper.setEntityMotionFromVector} toward a random point {@code spread} wide and one block up. */
    private void launch(ItemEntity e, double spread, float speed) {
        Vec3 target = e.position().add((-0.5 + level.getRandom().nextDouble()) * spread, 1.0, (-0.5 + level.getRandom().nextDouble()) * spread);
        e.setDeltaMovement(target.subtract(e.position()).normalize().scale(speed));
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("essentia", ESSENTIA, Map.copyOf(essentia));
        out.putInt("heat", heat);
        out.putBoolean("has_rift", hasRift);
        out.store("centre", BlockState.CODEC, centre);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        essentia.clear();
        in.read("essentia", ESSENTIA).ifPresent(essentia::putAll);
        heat = in.getIntOr("heat", 0);
        hasRift = in.getBooleanOr("has_rift", false);
        centre = in.read("centre", BlockState.CODEC).orElse(Blocks.BEDROCK.defaultBlockState());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
