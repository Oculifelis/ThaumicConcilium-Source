package com.thaumaturgicconcilium.block;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.thaumaturgicconcilium.TCRegistry;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;

/**
 * A Destabilized Crystal: one aspect and a capacity, filled one vis crystal at a time. The Vis Conductor spell drains
 * it, polishment and the Vis Condenser spend it.
 */
public class DestabilizedCrystalBlockEntity extends BlockEntity {
    private static final Codec<ResourceKey<IAspect>> ASPECT = ResourceKey.codec(IAspect.REGISTRY_KEY);
    private ResourceKey<IAspect> aspect;
    private int capacity;

    public DestabilizedCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(TCRegistry.DESTABILIZED_CRYSTAL_BE.get(), pos, state);
    }

    public Optional<ResourceKey<IAspect>> aspect() {
        return Optional.ofNullable(aspect);
    }

    public int capacity() {
        return capacity;
    }

    /** Adds one charge of {@code added}; an empty crystal takes any aspect, a filled one only its own. */
    public boolean fill(ResourceKey<IAspect> added) {
        if (aspect != null && capacity > 0 && !aspect.equals(added)) {
            return false;
        }
        aspect = added;
        capacity++;
        changed();
        return true;
    }

    /** Takes up to {@code amount} charge; returns how much was taken. */
    public int drain(int amount) {
        int taken = Math.min(amount, capacity);
        if (taken > 0) {
            capacity -= taken;
            changed();
        }
        return taken;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueOutput out = new ValueOutput(tag$, reg$);
        super.saveAdditional(out.tag(), out.registries());
        if (aspect != null) {
            out.store("aspect", ASPECT, aspect);
        }
        out.putInt("capacity", capacity);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueInput in = new ValueInput(tag$, reg$);
        super.loadAdditional(in.tag(), in.registries());
        aspect = in.read("aspect", ASPECT).orElse(null);
        capacity = in.getIntOr("capacity", 0);
        if (level != null && level.isClientSide()) {
            // re-mesh so the aspect tint follows the new aspect
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
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
