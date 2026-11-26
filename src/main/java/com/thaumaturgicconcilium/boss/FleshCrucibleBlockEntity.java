package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.mob.MadThaumaturgeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The flesh in a Crucible of Flesh. TC4 kept it as Corpus essentia; Thaumaturge has no Corpus, so it is a plain
 * amount, 0-64.
 */
public class FleshCrucibleBlockEntity extends BlockEntity {
    public static final int MAX = 64;
    private static final int PER_THAUMATURGE = 2;
    private static final DustParticleOptions BUBBLE = new DustParticleOptions(0xED478C, 0.7F);

    private int flesh;

    public FleshCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(TCBosses.FLESH_CRUCIBLE_BE.get(), pos, state);
    }

    public int flesh() {
        return flesh;
    }

    public void setFlesh(int amount) {
        flesh = Mth.clamp(amount, 0, MAX);
        changed();
    }

    /** TC4: floor(amount / 100 * 14), +1 when not empty. */
    public int comparator() {
        return Mth.floor(flesh / 100.0F * 14.0F) + (flesh > 0 ? 1 : 0);
    }

    /** A Mad Thaumaturge melted down: +2 flesh, up to 64. */
    public void smelt(MadThaumaturgeEntity entity) {
        if (level == null) {
            return;
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), TTSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2F,
                1.0F + level.getRandom().nextFloat() * 0.4F);
        entity.discard();
        setFlesh(flesh + PER_THAUMATURGE);
    }

    /** One point of flesh back into a player who has lost some. */
    public void sip(Player player) {
        if (level == null || flesh <= 0) {
            return;
        }
        setFlesh(flesh - 1);
        Ethereal.setFleshLost(player, Ethereal.fleshLost(player) - 1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.GORE.get(), SoundSource.PLAYERS, 0.3F,
                1.0F + level.getRandom().nextFloat() * 0.4F);
    }

    /** Pink bubbles while there is flesh and no redstone signal. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, FleshCrucibleBlockEntity be) {
        if (be.flesh <= 0 || level.hasNeighborSignal(pos)) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextInt(4) != 0) {
            return;
        }
        double x = pos.getX() + (5 + random.nextInt(22)) / 32.0;
        double z = pos.getZ() + (5 + random.nextInt(22)) / 32.0;
        level.addParticle(BUBBLE, x, be.surface(), z, 0.0, 0.02, 0.0);
    }

    /** World height of the flesh surface (TC4: y + 0.3 + amount / 100). */
    public double surface() {
        return worldPosition.getY() + 0.3 + flesh / 100.0;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("flesh", flesh);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        flesh = in.getIntOr("flesh", 0);
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
