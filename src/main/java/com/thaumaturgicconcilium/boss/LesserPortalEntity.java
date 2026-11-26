package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.entity.portal.CultistPortals;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.crimson.TCCrimson;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lesser Crimson Portal (TC4 Concilium {@code LesserPortal}): a 500-health tear that never moves and calls crimson
 * cultists while a player is within 48 blocks. Stage 0-4 a cultist every 15-24 ticks (faster each stage); stages 5-11
 * (and after 13) a slower beat, 20 ticks more for every cultist already within 32; stage 12 a long pause. It heals 1 a
 * tick before stage 12 and pays 5-9 health for every cultist after it. A portal opened by a chained rift (a "boss"
 * portal) spends stage 13 on the Crimson Pontifex instead, and closes. Touching it zaps for 8; it bursts (power 2, no
 * block damage) when it dies.
 *
 * <p>Minions: Thaumaturge's cultist knight and cleric, and the Concilium's Crimson Paladin, Archer and Ranger, one in
 * five each.
 */
public class LesserPortalEntity extends Monster {
    private static final float TOUCH_DAMAGE = 8.0F;
    private static final double PLAYER_RANGE = 48.0;
    private static final double CULTIST_RANGE = 32.0;
    private static final int HOME_RADIUS = 32;
    /** Entity tag the chained rift puts on the portal a Crimson Rites offering opens (rift package, by string). */
    public static final String RIFT_BOSS_TAG = "thaumaturgicconcilium.rift_boss";

    /** The waves a portal goes through, derived from its stage number by {@link #of(int)} (the stage is the saved value). */
    public enum Wave {
        /** Stages 0-4: a cultist every 15-24 ticks, faster each stage; the portal heals. */
        RUSH,
        /** Stages 5-11: the slower beat; the portal still heals. */
        STEADY,
        /** Stage 12: one cultist, then a long pause. */
        PAUSE,
        /** Stage 13: a boss portal brings the Crimson Pontifex and closes; a plain portal moves straight on. */
        FINALE,
        /** Stages 14+: the slower beat, and every cultist costs the portal 5-9 health. */
        DRAIN;

        public static Wave of(int stage) {
            if (stage <= 4) {
                return RUSH;
            }
            if (stage <= 11) {
                return STEADY;
            }
            return stage == 12 ? PAUSE : stage == 13 ? FINALE : DRAIN;
        }

        boolean heals() {
            return this == RUSH || this == STEADY;
        }

        boolean minionsCostHealth() {
            return this == FINALE || this == DRAIN;
        }
    }

    private static final int FIRST_DELAY = 200;

    private int stage;
    /** Ticks to the next beat. Saved with the stage, so a reload continues the wave instead of restarting the delay. */
    private int stageCounter = FIRST_DELAY;
    private boolean bossPortal;
    public int pulse;

    public LesserPortalEntity(EntityType<? extends LesserPortalEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.ARMOR, 5.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    /** A portal from a chained rift: its stage 13 brings the Crimson Pontifex. */
    public void setBossPortal(boolean value) {
        bossPortal = value;
    }

    public boolean isBossPortal() {
        return bossPortal;
    }

    @Override
    public void tick() {
        super.tick();
        if (pulse > 0) {
            pulse--;
        }
        if (!(level() instanceof ServerLevel level) || !isAlive()) {
            return;
        }
        if (!bossPortal && entityTags().contains(RIFT_BOSS_TAG)) {
            bossPortal = true;
        }
        if (stageCounter <= 0) {
            if (level.getNearestPlayer(this, PLAYER_RANGE) != null) {
                level.broadcastEntityEvent(this, CultistPortals.PULSE_EVENT);
                switch (wave()) {
                    case RUSH -> {
                        stageCounter = 15 + random.nextInt(10 - stage) - stage;
                        spawnMinion(level);
                    }
                    case PAUSE -> {
                        stageCounter = 50 + timing() * 2 + random.nextInt(50);
                        spawnMinion(level);
                    }
                    case FINALE -> {
                        if (bossPortal) {
                            spawnBoss(level);
                        }
                    }
                    case STEADY, DRAIN -> {
                        int a = timing();
                        stageCounter = a + random.nextInt(5 + a / 3);
                        spawnMinion(level);
                    }
                }
                stage++;
            } else {
                stageCounter = 30 + random.nextInt(30);
            }
        } else {
            stageCounter--;
        }
        if (wave().heals()) {
            heal(1.0F);
        }
    }

    public Wave wave() {
        return Wave.of(stage);
    }

    private int timing() {
        return CultistPortals.cultistsNear(this, CULTIST_RANGE) * 20;
    }

    private @Nullable Mob rollMinion(ServerLevel level) {
        EntityType<? extends Mob> type = switch (random.nextInt(5)) {
            case 0 -> TTEntities.CULTIST_KNIGHT.get();
            case 1 -> TTEntities.CULTIST_CLERIC.get();
            case 2 -> TCCrimson.CRIMSON_PALADIN.get();
            case 3 -> TCCrimson.CRIMSON_ARCHER.get();
            default -> TCCrimson.CRIMSON_RANGER.get();
        };
        return type.create(level, EntitySpawnReason.MOB_SUMMONED);
    }

    private void spawnMinion(ServerLevel level) {
        Mob cultist = rollMinion(level);
        if (cultist == null) {
            return;
        }
        CultistPortals.summon(this, level, cultist);
        cultist.setHomeTo(blockPosition(), HOME_RADIUS);
        level.playSound(null, cultist.getX(), cultist.getY(), cultist.getZ(), TTSounds.WANDFAIL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        if (wave().minionsCostHealth()) {
            hurtServer(level, damageSources().fellOutOfWorld(), 5 + random.nextInt(5));
        }
    }

    private void spawnBoss(ServerLevel level) {
        CrimsonPontifexEntity boss = TCBosses.CRIMSON_PONTIFEX.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) {
            return;
        }
        boss.snapTo(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat(),
                random.nextFloat() * 360.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        level.addFreshEntity(boss);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), TTSounds.WANDFAIL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        hurtServer(level, damageSources().fellOutOfWorld(), getHealth());
    }

    @Override
    public void playerTouch(Player player) {
        CultistPortals.touch(this, player, TOUCH_DAMAGE);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == CultistPortals.PULSE_EVENT) {
            pulse = CultistPortals.PULSE_TICKS;
            for (int i = 0; i < 20; i++) {
                level().addParticle(ParticleTypes.POOF, getRandomX(1.0), getRandomY(), getRandomZ(1.0), random.nextGaussian() * 0.02,
                        random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel && !isRemoved() && !dead) {
            CultistPortals.collapse(this, 2.0F);
        }
        super.die(source);
    }

    // ---------------------------------------------------------------- immobile, unaffected
    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected boolean considersEntityAsAlly(net.minecraft.world.entity.Entity other) {
        return other instanceof EntityCultist || other instanceof CrimsonPontifexEntity || super.considersEntityAsAlly(other);
    }

    // ---------------------------------------------------------------- sounds, save
    @Override
    protected float getSoundVolume() {
        return CultistPortals.SOUND_VOLUME;
    }

    @Override
    public int getAmbientSoundInterval() {
        return CultistPortals.AMBIENT_INTERVAL;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return TTSounds.MONOLITH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TTSounds.ZAP.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TTSounds.SHOCK.get();
    }

    /**
     * TC4 did not save the boss flag, so a boss portal reloaded before stage 13 never brought the Pontifex; it did not
     * save the beat countdown either, so every reload restarted a 200-tick delay. Both are saved with the stage now.
     */
    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("stage", stage);
        out.putInt("stage_counter", stageCounter);
        out.putBoolean("boss", bossPortal);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        stage = Math.max(0, in.getIntOr("stage", 0));
        stageCounter = in.getIntOr("stage_counter", FIRST_DELAY);
        bossPortal = in.getBooleanOr("boss", false);
    }

    /** Saved wave fields in one line, for the save/load round-trip self-test. */
    public String snapshot() {
        return "stage=" + stage + " wave=" + wave() + " counter=" + stageCounter + " boss=" + bossPortal;
    }

    public void selfTestSet(int stage, int counter, boolean boss) {
        this.stage = stage;
        this.stageCounter = counter;
        this.bossPortal = boss;
    }
}
