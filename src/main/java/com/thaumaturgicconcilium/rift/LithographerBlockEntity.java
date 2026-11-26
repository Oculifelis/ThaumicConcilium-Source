package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * TC4 {@code LithographerTile}: four monitor slots and a three-step projection (lower 20 ticks, imprint, raise 20 ticks).
 * The imprint needs a Vis Condenser on top carrying a Sensus Destabilized Crystal with at least 32 charge, and a player
 * asleep in the bed head below.
 */
public class LithographerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 4;
    /** Sensus charge one imprint costs (TC4 32). */
    public static final int CHARGE = 32;

    private final SimpleContainer monitors = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            LithographerBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return AstralMonitorItem.isLoaded(stack);
        }
    };
    /** The projection cycle. Saved by name; {@link #id()} is the number the renderer switches on. */
    public enum Projection {
        IDLE("idle", 0),
        LOWERING("lowering", 1),
        IMPRINTING("imprinting", 2),
        RAISING("raising", 3);

        private final String serialName;
        private final int id;

        Projection(String serialName, int id) {
            this.serialName = serialName;
            this.id = id;
        }

        public int id() {
            return id;
        }

        static Projection byName(String name) {
            for (Projection p : values()) {
                if (p.serialName.equals(name)) {
                    return p;
                }
            }
            return IDLE;
        }
    }

    private static final int STEP_TICKS = 20;

    private Projection projection = Projection.IDLE;
    /** Ticks left in the current step (0 when idle). The direction comes from the step, never from a sign. */
    private int timer;

    public LithographerBlockEntity(BlockPos pos, BlockState blockState) {
        super(TCRifts.LITHOGRAPHER_BE.get(), pos, blockState);
    }

    public SimpleContainer monitors() {
        return monitors;
    }

    /** The renderer's signed deploy value: counting down while lowering or imprinting, up from -20 while raising. */
    public int deploy() {
        return projection == Projection.RAISING ? -timer : timer;
    }

    public int state() {
        return projection.id();
    }

    public Projection projection() {
        return projection;
    }

    /** Starts a projection; ignored unless idle (it used to restart mid-raise with the plates half down). */
    public void project() {
        if (projection == Projection.IDLE) {
            enter(Projection.LOWERING);
        }
    }

    /** The one place the cycle changes step: sets the step and its full timer, and syncs it. */
    private void enter(Projection next) {
        projection = next;
        timer = next == Projection.IDLE ? 0 : STEP_TICKS;
        changed();
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, LithographerBlockEntity be) {
        if (be.timer > 0) {
            be.timer--;
        }
        if (level.isClientSide() || be.timer != 0) {
            return;
        }
        switch (be.projection) {
            case LOWERING -> {
                be.enter(Projection.IMPRINTING);
                be.imprint((ServerLevel) level);
            }
            case IMPRINTING -> be.enter(Projection.RAISING);
            case RAISING -> be.enter(Projection.IDLE);
            case IDLE -> {
            }
        }
    }

    private void imprint(ServerLevel server) {
        BlockPos p = worldPosition;
        if (!(server.getBlockEntity(p.above()) instanceof VisCondenserBlockEntity)) {
            return;
        }
        if (!(server.getBlockEntity(p.above(2)) instanceof DestabilizedCrystalBlockEntity crystal)
                || !crystal.aspect().map(TTAspects.SENSUS::equals).orElse(false) || crystal.capacity() < CHARGE) {
            return;
        }
        BlockState bed = server.getBlockState(p.below());
        if (!(bed.getBlock() instanceof BedBlock) || bed.getValue(BedBlock.PART) != BedPart.HEAD) {
            return;
        }
        Vec3 bedAt = Vec3.atCenterOf(p.below());
        ServerPlayer sleeper = server.getEntitiesOfClass(ServerPlayer.class, new AABB(bedAt, bedAt).inflate(2.0),
                pl -> pl.position().distanceTo(bedAt) <= 2.0).stream().min((a, b) -> Double.compare(a.distanceToSqr(bedAt), b.distanceToSqr(bedAt)))
                .orElse(null);
        if (sleeper == null || !sleeper.isSleeping()) {
            return;
        }
        List<RiftData.Target> targets = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            targets.add(AstralMonitorItem.target(monitors.getItem(i)));
        }
        RiftData.Lithograph old = sleeper.getData(TCRifts.LITHOGRAPH);
        sleeper.setData(TCRifts.LITHOGRAPH, new RiftData.Lithograph(true, old.relieved(), List.copyOf(targets)));
        sleeper.syncData(TCRifts.LITHOGRAPH);
        crystal.drain(CHARGE);
        server.playSound(null, p, TTSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        RiftUtil.bolt(server, Vec3.atCenterOf(p).add(0, 0.5, 0), Vec3.atCenterOf(p).add(0, -0.5, 0), 0x00D9FF, 0.3F);
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Called from the block's onRemove. */
    public void dropMonitors(BlockPos pos) {
        if (level != null) {
            Containers.dropContents(level, pos, monitors);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueOutput out = new ValueOutput(tag$, reg$);
        super.saveAdditional(out.tag(), out.registries());
        out.putString("projection", projection.serialName);
        out.putInt("timer", timer);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, monitors.getItem(i));
        }
        ContainerHelper.saveAllItems(out.tag(), items, out.registries());
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag$, net.minecraft.core.HolderLookup.Provider reg$) {
        ValueInput in = new ValueInput(tag$, reg$);
        super.loadAdditional(in.tag(), in.registries());
        projection = Projection.byName(in.getStringOr("projection", Projection.IDLE.serialName));
        timer = projection == Projection.IDLE ? 0 : Math.max(0, Math.min(STEP_TICKS, in.getIntOr("timer", 0)));
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in.tag(), items, in.registries());
        for (int i = 0; i < SLOTS; i++) {
            monitors.setItem(i, items.get(i));
        }
    }

    /** Saved projection fields in one line, for the save/load round-trip self-test. */
    public String snapshot() {
        return "projection=" + projection.serialName + " timer=" + timer + " deploy=" + deploy();
    }

    public void selfTestSet(Projection p, int ticks) {
        projection = p;
        timer = ticks;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumaturgicconcilium.lithographer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LithographerMenu(id, inventory, this);
    }
}
