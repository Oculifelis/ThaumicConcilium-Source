package com.thaumaturgicconcilium.mob;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.content.crucible.BlockEntityCrucible;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;

/**
 * The Quicksilver Crucible's store of Permutatio and its fusing pulse. Thaumaturge's crucible has no public way to
 * change its aspects, so the fuse writes the crucible's private {@code aspects} field by reflection and syncs it; if
 * that field is ever renamed the crucible simply stops fusing (logged once).
 */
public class QuicksilverCrucibleBlockEntity extends BlockEntity {
    public static final int MAX = 64;
    public static final int CONVERTED_START = 20;
    private static final int PER_QUICKSILVER = 2;
    private static final int INTERVAL = 20;
    private static final int RANGE = 8;
    private static final ResourceLocation QUICKSILVER = ResourceLocation.parse("thaumaturge:quicksilver");
    private static Field aspectsField;
    private static boolean reflectionFailed;

    private int permutatio;

    public QuicksilverCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(TCMobs.QUICKSILVER_CRUCIBLE_BE.get(), pos, state);
    }

    public int permutatio() {
        return permutatio;
    }

    public void setPermutatio(int amount) {
        permutatio = Math.max(0, Math.min(MAX, amount));
        changed();
    }

    public int comparator() {
        return permutatio == 0 ? 0 : 1 + permutatio * 14 / MAX;
    }

    /** Quicksilver dropped in: +2 Permutatio per item, up to 64; the rest stays as an item. */
    public void absorb(ItemEntity entity) {
        Item quicksilver = BuiltInRegistries.ITEM.getOptional(QUICKSILVER).orElse(null);
        ItemStack stack = entity.getItem();
        if (quicksilver == null || !stack.is(quicksilver)) {
            return;
        }
        int room = (MAX - permutatio) / PER_QUICKSILVER;
        int used = Math.min(room, stack.getCount());
        if (used <= 0) {
            return;
        }
        stack.shrink(used);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
        setPermutatio(permutatio + used * PER_QUICKSILVER);
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuicksilverCrucibleBlockEntity be) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % INTERVAL != 0 || be.permutatio <= 0 || server.hasNeighborSignal(pos)) {
            return;
        }
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-RANGE, -RANGE, -RANGE), pos.offset(RANGE, RANGE, RANGE))) {
            if (server.getBlockEntity(at) instanceof BlockEntityCrucible crucible && fuse(server, crucible)) {
                be.setPermutatio(be.permutatio - 1);
                return;
            }
        }
    }

    /** Fuses the crucible's last two aspects into their compound: -1 each, +1 compound. */
    private static boolean fuse(ServerLevel level, BlockEntityCrucible crucible) {
        AspectList list = crucible.getAspects();
        List<AspectInstance> entries = list.entries();
        if (entries.size() < 2) {
            return false;
        }
        Holder<IAspect> a = entries.get(entries.size() - 1).aspect();
        Holder<IAspect> b = entries.get(entries.size() - 2).aspect();
        Optional<Holder.Reference<IAspect>> compound = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).listElements()
                .filter(h -> isCompoundOf(h.value(), a, b)).findFirst();
        if (compound.isEmpty()) {
            return false;
        }
        AspectList fused = list.remove(a, 1).remove(b, 1).add(compound.get(), 1);
        if (!write(crucible, fused)) {
            return false;
        }
        level.playSound(null, crucible.getBlockPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4F, 1.4F);
        return true;
    }

    private static boolean isCompoundOf(IAspect aspect, Holder<IAspect> a, Holder<IAspect> b) {
        List<Holder<IAspect>> parts = aspect.components();
        return parts.size() == 2 && ((same(parts.get(0), a) && same(parts.get(1), b)) || (same(parts.get(0), b) && same(parts.get(1), a)));
    }

    private static boolean same(Holder<IAspect> x, Holder<IAspect> y) {
        return x.unwrapKey().isPresent() && x.unwrapKey().equals(y.unwrapKey());
    }

    private static boolean write(BlockEntityCrucible crucible, AspectList aspects) {
        if (reflectionFailed) {
            return false;
        }
        try {
            if (aspectsField == null) {
                aspectsField = BlockEntityCrucible.class.getDeclaredField("aspects");
                aspectsField.setAccessible(true);
            }
            aspectsField.set(crucible, aspects);
            ((AbstractSyncedBlockEntity) crucible).setChangedAndSync();
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            reflectionFailed = true;
            ThaumaturgicConcilium.LOG.warn("Quicksilver Crucible cannot change Thaumaturge crucible aspects; fusing is off", e);
            return false;
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueOutput out = new ValueOutput(tag$, reg$);
        super.saveAdditional(out.tag(), out.registries());
        out.putInt("permutatio", permutatio);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueInput in = new ValueInput(tag$, reg$);
        super.loadAdditional(in.tag(), in.registries());
        permutatio = in.getIntOr("permutatio", 0);
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
