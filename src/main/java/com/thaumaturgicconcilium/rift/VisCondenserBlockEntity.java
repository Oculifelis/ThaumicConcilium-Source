package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.content.aspect.EntityAspects;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** TC4 {@code VisCondenserTile}: the cooldown, the bolt ({@code shoot}) and the pour into a Hex ({@code sendEssentia}). */
public class VisCondenserBlockEntity extends BlockEntity {
    /** Most charge one bolt spends (TC4 16). */
    public static final int MAX_BLAST = 16;
    public static final int COOLDOWN = 40;
    private static final int HEX_RANGE = 8;

    private int cooldown;

    public VisCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(TCRifts.VIS_CONDENSER_BE.get(), pos, state);
    }

    public int cooldown() {
        return cooldown;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VisCondenserBlockEntity condenser) {
        if (condenser.cooldown > 0) {
            condenser.cooldown--;
            if (condenser.cooldown == 0 && !level.isClientSide()) {
                condenser.changed();
            }
        }
        if (!level.isClientSide() && level.getRandom().nextInt(20) == 0 && level.getBlockEntity(pos.above()) instanceof DestabilizedCrystalBlockEntity) {
            level.playSound(null, pos.above(), TTSounds.JACOBS.get(), SoundSource.BLOCKS, 0.01F, 1.0F);
        }
    }

    /** TC4 {@code onNeighborBlockChange}. */
    void redstone(ServerLevel server) {
        if (!(server.getBlockEntity(worldPosition.above()) instanceof DestabilizedCrystalBlockEntity crystal) || crystal.aspect().isEmpty()
                || crystal.capacity() == 0 || cooldown != 0) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int power = VisCondenserBlock.sidePower(server, worldPosition, side);
            if (power == 0) {
                continue;
            }
            Direction away = side.getOpposite();
            BlockPos row = worldPosition.above();
            Vec3 from = Vec3.atLowerCornerOf(row.relative(away));
            Vec3 to = Vec3.atLowerCornerOf(row.relative(away, power));
            BlockHitResult hit = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
            BlockPos target = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : row.relative(away, power);
            shoot(server, crystal, target, power);
            cooldown = COOLDOWN;
        }
        if (VisCondenserBlock.sidePower(server, worldPosition, Direction.DOWN) != 0 && crystal.capacity() != 0) {
            sendEssentia(server, crystal);
        }
        changed();
    }

    private void shoot(ServerLevel server, DestabilizedCrystalBlockEntity crystal, BlockPos target, int power) {
        // TC4 also set the cooldown after the first side, so only one side fires per pulse
        if (cooldown != 0 || crystal.capacity() == 0) {
            return;
        }
        int blast = Math.min(crystal.capacity(), MAX_BLAST);
        int color = crystal.aspect().map(a -> RiftUtil.aspectColor(server, a)).orElse(0xFFFFFF);
        BlockPos c = crystal.getBlockPos();
        AABB box = new AABB(Math.min(c.getX() - 0.6, target.getX() - 0.6), c.getY() - 0.3, Math.min(c.getZ() - 0.6, target.getZ() - 0.6),
                Math.max(target.getX() + 0.6, c.getX() + 1.0), target.getY() + 0.3, Math.max(target.getZ() + 0.6, c.getZ() + 1.0));
        crystal.drain(blast);
        float damage = Mth.clamp(blast + 1 - power, 0, MAX_BLAST);
        List<LivingEntity> hit = server.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity e : hit) {
            e.hurt(server.damageSources().magic(), damage);
            if (e.isDeadOrDying()) {
                dropEssence(server, e);
            }
        }
        server.playSound(null, worldPosition, TTSounds.SHOCK.get(), SoundSource.BLOCKS, 0.8F, server.getRandom().nextFloat() * 0.1F + 0.9F);
        RiftUtil.bolt(server, Vec3.atCenterOf(worldPosition).add(0, 1.0, 0), Vec3.atCenterOf(target), color, 0.01F);
    }

    /** TC4 dropped a wisp essence of one of the victim's aspects; here a vis crystal of that aspect. */
    private static void dropEssence(ServerLevel server, LivingEntity e) {
        AspectList aspects = EntityAspects.of(e);
        if (aspects.isEmpty()) {
            return;
        }
        AspectInstance pick = aspects.entries().get(server.getRandom().nextInt(aspects.size()));
        ItemStack crystal = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get());
        crystal.set(TTDataComponents.CRYSTAL_ASPECT.get(), new AspectInstance(pick.aspect(), 1));
        e.spawnAtLocation(crystal, 0.2F);
    }

    /** TC4 {@code sendEssentia}: the crystal's whole charge goes to the first hex with a rift within 8 blocks. */
    private void sendEssentia(ServerLevel server, DestabilizedCrystalBlockEntity crystal) {
        BlockPos c = crystal.getBlockPos();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-HEX_RANGE, -HEX_RANGE, -HEX_RANGE), c.offset(HEX_RANGE, HEX_RANGE, HEX_RANGE))) {
            if (server.getBlockEntity(p) instanceof HexBlockEntity hex && hex.hasRift()) {
                int color = crystal.aspect().map(a -> RiftUtil.aspectColor(server, a)).orElse(0xFFFFFF);
                crystal.aspect().ifPresent(a -> hex.receive(a, crystal.capacity()));
                crystal.drain(crystal.capacity());
                server.playSound(null, worldPosition, TTSounds.SHOCK.get(), SoundSource.BLOCKS, 0.8F, server.getRandom().nextFloat() * 0.1F + 0.9F);
                RiftUtil.bolt(server, Vec3.atCenterOf(worldPosition).add(0, 1.0, 0), Vec3.atCenterOf(p).add(0, 1.0, 0), color, 0.01F);
                return;
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueOutput out = new ValueOutput(tag$, reg$);
        super.saveAdditional(out.tag(), out.registries());
        out.putInt("cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueInput in = new ValueInput(tag$, reg$);
        super.loadAdditional(in.tag(), in.registries());
        cooldown = in.getIntOr("cooldown", 0);
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
