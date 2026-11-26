package com.thaumaturgicconcilium.compat.horizons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Red-Powered Mind tile (TC4 Concilium {@code RedPoweredMindTile}): keeps the redstone power fed into the block (the
 * Lithographer reads it as the brain's thought strength) and, on the client, turns the brain toward the nearest player
 * within 6 blocks like an enchanting table's book.
 */
public class RedPoweredMindBlockEntity extends BlockEntity {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2.0F;

    private int power;
    public float rota;
    public float rotb;
    private float target;

    public RedPoweredMindBlockEntity(BlockPos pos, BlockState state) {
        super(HorizonsContent.RED_POWERED_MIND_BE.get(), pos, state);
    }

    public int power() {
        return power;
    }

    void setPower(int value) {
        if (value == power) {
            return;
        }
        power = value;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RedPoweredMindBlockEntity be) {
        be.rotb = be.rota;
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0, false);
        if (player != null) {
            be.target = (float) Mth.atan2(player.getZ() - (pos.getZ() + 0.5), player.getX() - (pos.getX() + 0.5));
        } else {
            be.target += 0.01F;
        }
        be.rota = wrap(be.rota);
        be.target = wrap(be.target);
        be.rota += wrap(be.target - be.rota) * 0.04F;
    }

    public static float wrap(float angle) {
        while (angle >= PI) {
            angle -= TAU;
        }
        while (angle < -PI) {
            angle += TAU;
        }
        return angle;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Power", power);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        power = in.getIntOr("Power", 0);
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
