package com.thaumaturgicconcilium.compat.witchery;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.MobUtil;
import java.util.EnumSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sloppy Alchemist (TC4 {@code SloppyAlchemist}): an alchemist bled half-witless, ridden on all fours by a Chort (it
 * spawns with one on its back). While ridden it is a fire-proof 1 x 0.8 beast (25 HP, 8 attack, step 1) that backs away
 * and circles within 16 blocks, then rams: a 12-tick wind-up hop and a charge at 1.1 through the target and 3 blocks past
 * it, hitting once with a big knockback (2.5 across, 0.65 up). Freed of its rider it stands up (0.5 x 1.8) and does
 * nothing more; right-clicked, a freed one thanks the player with a reward (loot table
 * {@code thaumaturgicconcilium:gameplay/sloppy_alchemist_reward}) and leaves, one that was never ridden just leaves.
 *
 * <p>Port fixes: "freed" is now saved (the original forgot it on reload, so a rescued alchemist lost its reward), and the
 * thanks are sent by the server, so they always match the reward.
 */
public class SloppyAlchemistEntity extends Monster {
    private static final ResourceKey<LootTable> REWARD = ResourceKey.create(Registries.LOOT_TABLE,
            ThaumaturgicConcilium.id("gameplay/sloppy_alchemist_reward"));
    private static final EntityDimensions RIDDEN = EntityDimensions.scalable(1.0F, 0.8F).withEyeHeight(0.65F);

    private boolean wasRidden;

    public SloppyAlchemistEntity(EntityType<? extends SloppyAlchemistEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 25.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new RamGoal(this, 0.22, 1.1, 60, 16.0F));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, t -> isRidden()));
    }

    /** Ridden by a Chort (the beast form). */
    public boolean isRidden() {
        return getFirstPassenger() instanceof ChortEntity;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        ChortEntity chort = WitcheryMobs.CHORT.get().create(level.getLevel());
        if (chort != null) {
            chort.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
            chort.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
            if (chort.startRiding(this, true)) {
                level.addFreshEntity(chort);
                wasRidden = true;
            }
        }
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && getFirstPassenger() instanceof ChortEntity chort) {
            wasRidden = true;
            LivingEntity t = chort.getTarget();
            if (t != null && t.isAlive()) {
                setTarget(t);
            }
        }
    }

    // ---- shape: on all fours while ridden ----

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return isRidden() ? RIDDEN : super.getDefaultDimensions(pose);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        refreshDimensions();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        refreshDimensions();
    }

    /** The Chort sits on the back (TC4 mounted offset 0.3 above a 0.8 tall body plus the rider's own seat height). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height(), 0.0);
    }

    // ---- the freed alchemist ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isRidden()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            for (int i = 0; i < 20; i++) {
                level().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                        getY() + random.nextDouble() * getBbHeight(), getZ() + (random.nextDouble() - 0.5) * getBbWidth(), 0.0, 0.02, 0.0);
            }
            return InteractionResult.SUCCESS;
        }
        ServerLevel level = (ServerLevel) level();
        MobUtil.play(level, this, MobUtil.sound(ThaumaturgicConcilium.id("breath").toString()), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (wasRidden) {
            player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.sloppy_alchemist.thanks." + random.nextInt(3))
                    .withStyle(ChatFormatting.DARK_RED));
            LootTable table = level.getServer().reloadableRegistries().getLootTable(REWARD);
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.ORIGIN, position()).create(LootContextParamSets.GIFT);
            table.getRandomItems(params, stack -> {
                if (!player.addItem(stack)) {
                    spawnAtLocation(stack);
                }
            });
        } else {
            player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.sloppy_alchemist.confused").withStyle(ChatFormatting.DARK_RED));
        }
        discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
        super.addAdditionalSaveData(out.tag());
        out.putBoolean("was_ridden", wasRidden);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
        super.readAdditionalSaveData(in.tag());
        wasRidden = in.getBooleanOr("was_ridden", false);
    }

    /**
     * TC4 {@code SloppyAlchemistAttackAI}, only while a Chort rides it: keep facing the target; within {@code maxDistance}
     * back away at {@code evadeSpeed} while strafing (30-tick legs, factor 0.1); after {@code chargeInterval} ticks of
     * seeing the target, wind up for 12 ticks (a 0.45 hop) and ram at {@code ramSpeed} toward a point 3 blocks past the
     * target, hitting once when within reach.
     */
    static final class RamGoal extends Goal {
        private static final int WINDUP = 12;
        private static final double KNOCK_H = 2.5;
        private static final double KNOCK_V = 0.65;
        private static final double OVERSHOOT = 3.0;
        private static final double STRAFE = 0.1;

        private final SloppyAlchemistEntity mob;
        private final double evadeSpeed;
        private final double ramSpeed;
        private final int chargeInterval;
        private final float maxDistance;
        private @Nullable LivingEntity target;
        private int strafeTime;
        private int strafeDir = 1;
        /** EVADE: back off and strafe while {@code chargeTime} builds; WINDUP: the hop; RAM: the charge. */
        enum Phase { EVADE, WINDUP, RAM }

        private Phase phase = Phase.EVADE;
        private int chargeTime;
        // WINDUP only
        private int windup;
        // RAM only
        private boolean hit;
        private double ramX;
        private double ramZ;

        RamGoal(SloppyAlchemistEntity mob, double evadeSpeed, double ramSpeed, int chargeInterval, float maxDistance) {
            this.mob = mob;
            this.evadeSpeed = evadeSpeed;
            this.ramSpeed = ramSpeed;
            this.chargeInterval = chargeInterval;
            this.maxDistance = maxDistance;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            if (!mob.isRidden() || t == null || !t.isAlive()) {
                return false;
            }
            target = t;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && mob.isRidden();
        }

        @Override
        public void stop() {
            target = null;
            enter(Phase.EVADE);
            mob.getNavigation().stop();
        }

        /** The one place the goal changes phase; every counter that belongs to a phase starts fresh. */
        private void enter(Phase next) {
            phase = next;
            chargeTime = 0;
            windup = 0;
            hit = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = target;
            if (t == null) {
                return;
            }
            float yaw = (float) (Math.atan2(t.getZ() - mob.getZ(), t.getX() - mob.getX()) * 180.0 / Math.PI) - 90.0F;
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
            double dx = mob.getX() - t.getX();
            double dz = mob.getZ() - t.getZ();
            double dy = mob.getY() - t.getY();
            double distSq = dx * dx + dz * dz;
            Vec3 v = mob.getDeltaMovement();
            if (phase == Phase.WINDUP) {
                windup++;
                mob.setDeltaMovement(0.0, windup == 1 && mob.onGround() ? 0.45 : v.y, 0.0);
                if (windup >= WINDUP) {
                    enter(Phase.RAM);
                    double rx = t.getX() - mob.getX();
                    double rz = t.getZ() - mob.getZ();
                    double len = Math.max(1.0E-4, Math.sqrt(rx * rx + rz * rz));
                    ramX = t.getX() + rx / len * OVERSHOOT;
                    ramZ = t.getZ() + rz / len * OVERSHOOT;
                }
                return;
            }
            if (phase == Phase.RAM) {
                double rx = ramX - mob.getX();
                double rz = ramZ - mob.getZ();
                double len = Math.max(1.0E-4, Math.sqrt(rx * rx + rz * rz));
                mob.setDeltaMovement(rx / len * ramSpeed, v.y, rz / len * ramSpeed);
                if (!hit && distSq < 2.2 && dy * dy < 2.2 && mob.level() instanceof ServerLevel level) {
                    mob.doHurtTarget(t);
                    double kx = t.getX() - mob.getX();
                    double kz = t.getZ() - mob.getZ();
                    double klen = Math.sqrt(kx * kx + kz * kz);
                    if (klen > 0.0) {
                        t.push(kx / klen * KNOCK_H, KNOCK_V, kz / klen * KNOCK_H);
                        t.hurtMarked = true;
                    }
                    hit = true;
                }
                if (hit || len < 0.6) {
                    enter(Phase.EVADE);
                }
                return;
            }
            if (distSq < maxDistance * maxDistance) {
                double len = Math.max(1.0E-4, Math.sqrt(distSq));
                double bx = dx / len;
                double bz = dz / len;
                if (strafeTime <= 0) {
                    strafeTime = 30;
                    strafeDir = mob.getRandom().nextBoolean() ? 1 : -1;
                } else {
                    strafeTime--;
                }
                mob.setDeltaMovement(bx * evadeSpeed - bz * STRAFE * strafeDir, v.y, bz * evadeSpeed + bx * STRAFE * strafeDir);
                mob.getNavigation().stop();
            }
            chargeTime = mob.getSensing().hasLineOfSight(t) ? chargeTime + 1 : 0;
            if (chargeTime >= chargeInterval) {
                enter(Phase.WINDUP);
            }
        }
    }
}
