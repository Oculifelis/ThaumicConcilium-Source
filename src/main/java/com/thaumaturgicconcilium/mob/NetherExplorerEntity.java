package com.thaumaturgicconcilium.mob;

import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Nether Explorer (TC4 Concilium): the body of an unlucky explorer floating face down on Nether lava (and water). Touch
 * it or hit it and it gives up a Nether Membrane, teaches that player the Nether Membrane research, and is gone.
 * Spawned by {@link TCMobs}'s Nether spawner, never naturally.
 */
public class NetherExplorerEntity extends Mob {
    public static final ResourceLocation RESEARCH = ThaumaturgicConcilium.id("nether_membrane");
    private static final double MAX_SPEED = 0.45;

    public NetherExplorerEntity(EntityType<? extends NetherExplorerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 0.5).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    public boolean breathesUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** Boat-like buoyancy on lava and water; drifts but never faster than 0.45. */
    @Override
    public void travel(Vec3 input) {
        double fluid = Math.max(getFluidHeight(FluidTags.LAVA), getFluidHeight(FluidTags.WATER));
        Vec3 v = getDeltaMovement();
        if (fluid > 0.1) {
            v = new Vec3(v.x * 0.9, Math.min(v.y + 0.04, 0.1), v.z * 0.9);
        } else if (fluid > 0.0) {
            v = new Vec3(v.x * 0.9, Math.max(v.y, 0.0) * 0.5, v.z * 0.9);
        } else {
            v = new Vec3(v.x * 0.91, v.y - 0.04, v.z * 0.91);
        }
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        if (h > MAX_SPEED) {
            v = new Vec3(v.x / h * MAX_SPEED, v.y, v.z / h * MAX_SPEED);
        }
        move(MoverType.SELF, v);
        setDeltaMovement(v);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && isAlive()) {
            loot(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, amount);
        }
        if (source.getEntity() == null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (isAlive()) {
            loot(source.getEntity() instanceof ServerPlayer sp ? sp : null);
        }
        return true;
    }

    private void loot(ServerPlayer player) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ItemEntity drop = new ItemEntity(level, getX(), getY() + 0.3, getZ(), new ItemStack(TCRegistry.NETHER_MEMBRANE.get()));
        drop.setPickUpDelay(40);
        level.addFreshEntity(drop);
        if (player != null) {
            MobUtil.unlockResearch(player, RESEARCH);
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }
}
