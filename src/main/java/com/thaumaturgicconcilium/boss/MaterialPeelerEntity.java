package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.effect.WispEffects;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.TCUtil;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.spell.ConductorLock;
import com.thaumaturgicconcilium.spell.ConductorTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Peeler of Matter (TC4 Concilium {@code MaterialPeeler}): a hole in matter that a chained rift opens when it is fed a
 * Primordial Life. It only holds while it floats over the Hex of Predictability (any block in the tag
 * {@code thaumaturgicconcilium:peeler_anchors}, about two blocks below it); without one it bursts and taints the ground.
 * A Dematerialization spell with an Auram crystal locked does the same.
 *
 * <p>A player who touches it is flipped between solid and ethereal (once a second), with ten seconds of nausea. It
 * leaves alone a player who is ethereal by the Pontifex robe's own toggle.
 */
public class MaterialPeelerEntity extends Entity implements ConductorTarget {
    /** What holds a Peeler (the Hex of Predictability, filled in by the rift helper's data). */
    public static final TagKey<Block> ANCHORS = TagKey.create(Registries.BLOCK, ThaumaturgicConcilium.id("peeler_anchors"));

    public int innerRotation;

    public MaterialPeelerEntity(EntityType<? extends MaterialPeelerEntity> type, Level level) {
        super(type, level);
        innerRotation = random.nextInt(100000);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        innerRotation++;
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount % 300 == 0) {
            level.playSound(null, getX(), getY(), getZ(), TTSounds.EVILPORTAL.get(), SoundSource.HOSTILE, (float) (0.15 + random.nextGaussian() * 0.066),
                    (float) (0.75 + random.nextGaussian() * 0.1));
        }
        if (tickCount % 5 == 0 && !anchored(level)) {
            collapse(level);
        }
    }

    /**
     * TC4 looked for the Hex tile at (ceil x, ceil y - 2, ceil z), which only lined up for the rift's own spot; the port
     * accepts an anchor anywhere in the 3x3 columns one to three blocks below.
     */
    private boolean anchored(ServerLevel level) {
        BlockPos base = BlockPos.containing(getX(), getY(), getZ());
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-1, -3, -1), base.offset(1, -1, 1))) {
            if (level.getBlockState(p).is(ANCHORS)) {
                return true;
            }
        }
        BlockPos ceil = new BlockPos(Mth.ceil(getX()), Mth.ceil(getY() - 2.0), Mth.ceil(getZ()));
        return level.getBlockState(ceil).is(ANCHORS);
    }

    /** Burst, craft-fail sound, gone; ten random columns within 5 blocks get taint fibres, flux gas or goo. */
    public void collapse(ServerLevel level) {
        level.sendParticles(WispEffects.burst(2.0F), getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, getX(), getY(), getZ(), TTSounds.CRAFTFAIL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        BlockPos center = BlockPos.containing(getX() + random.nextGaussian() * 1.5, getY(), getZ() + random.nextGaussian() * 1.5);
        TCUtil.taintArea(level, center, 5);
        discard();
    }

    @Override
    public void playerTouch(Player player) {
        if (tickCount % 20 != 0 || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (!Ethereal.robeToggled(player)) {
            Ethereal.setEthereal(player, !Ethereal.isEthereal(player));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 10));
        }
        level.sendParticles(WispEffects.burst(2.0F), player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.WANDFAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public boolean onDematerialize(Player caster, ConductorLock.State lock, float potency) {
        if (!lock.aspect().equals(TTAspects.AURAM) || !(level() instanceof ServerLevel level)) {
            return false;
        }
        collapse(level);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, damage);
        }
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
    }
}
