package com.thaumaturgicconcilium.crimson;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.entity.ai.CultistHurtByTargetGoal;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Crimson Archer: a crimson cultist bowman (25 HP) that regenerates, keeps its distance and looses a "primal" arrow
 * every second. Guards the Crimson Avanpost. Without a bow it rushes in to melee.
 *
 * <p>Thaumaturge has no primal arrows, so each shot is a vanilla arrow carrying one of six primal tricks picked at
 * random (the original's {@code EntityPrimalArrow} type 0-5): aer levitates, ignis burns, aqua slows, terra hits
 * harder, ordo weakens, perditio withers. The bow's own enchantments (Power, Punch, Flame) apply as usual.
 */
public class CrimsonArcher extends EntityCultist implements KitingAttackGoal.KitingMob {
    private static final ResourceKey<LootTable> EQUIPMENT = ResourceKey.create(Registries.LOOT_TABLE, ThaumaturgicConcilium.id("equipment/crimson_archer"));

    /**
     * Archers waiting to be called in around this one (the Avanpost's 0-4 surface archers). The structure template
     * puts {@code "thaumaturgicconcilium:escorts": 1b} on one of its archers; on its first server tick it rolls the count.
     */
    private boolean escorts;

    public CrimsonArcher(EntityType<? extends CrimsonArcher> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCultist.createCultistAttributes().add(Attributes.MAX_HEALTH, 25.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new KitingAttackGoal(this, 20, 20.0F));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, t -> TCCrimson.isThaumaturge(t)));
    }

    @Override
    protected ResourceKey<LootTable> equipmentTable() {
        return EQUIPMENT;
    }

    /** The original's bow enchanting: 25% x local difficulty for a level 5 + difficulty x 0-17 enchantment. */
    @Override
    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        enchantSpawnedWeapon(level, random, difficulty);
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
        if (tickCount % 40 == 0) {
            heal(1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (escorts && level() instanceof ServerLevel level) {
            escorts = false;
            callEscorts(level);
        }
    }

    /** 0-4 more archers on the surface within 8 blocks, persistent like the tower's own. */
    private void callEscorts(ServerLevel level) {
        int n = getRandom().nextInt(5);
        for (int i = 0; i < n; i++) {
            int x = getBlockX() - 8 + getRandom().nextInt(16);
            int z = getBlockZ() - 8 + getRandom().nextInt(16);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, getBlockY(), z));
            CrimsonArcher archer = TCCrimson.CRIMSON_ARCHER.get().create(level);
            if (archer == null) {
                continue;
            }
            archer.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, getRandom().nextFloat() * 360.0F, 0.0F);
            archer.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.STRUCTURE, null);
            archer.setPersistenceRequired();
            level.addFreshEntity(archer);
        }
    }

    @Override
    public boolean meleeMode() {
        return !(getMainHandItem().getItem() instanceof BowItem);
    }

    @Override
    public void kiteAttack(ServerLevel level, LivingEntity target) {
        ItemStack bow = getMainHandItem();
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), 1.0F, bow);
        int type = getRandom().nextInt(6);
        arrow.setBaseDamage(2.0 + getRandom().nextInt(8) + (type == 3 ? 1.0 : 0.0));
        primal(arrow, type);
        double dx = target.getX() + target.getDeltaMovement().x - getX();
        double dz = target.getZ() + target.getDeltaMovement().z - getZ();
        double dy = target.getY(0.3333) - arrow.getY();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.06, dz, 2.5F, 1.5F);
        playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level.addFreshEntity(arrow);
    }

    /** The six primal arrows as vanilla arrow tricks. */
    private static void primal(AbstractArrow arrow, int type) {
        switch (type) {
            case 0 -> addEffect(arrow, new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
            case 1 -> arrow.igniteForSeconds(5.0F);
            case 2 -> addEffect(arrow, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            case 3 -> {
                // terra: the heavier arrow, +1 base damage (set by the caller)
            }
            case 4 -> addEffect(arrow, new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            default -> addEffect(arrow, new MobEffectInstance(MobEffects.WITHER, 100, 0));
        }
    }

    private static void addEffect(AbstractArrow arrow, MobEffectInstance effect) {
        if (arrow instanceof Arrow a) {
            a.addEffect(effect);
        }
    }

    @Override
    public float getVoicePitch() {
        return (getRandom().nextFloat() - getRandom().nextFloat()) * 0.2F + 1.5F;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
        super.addAdditionalSaveData(out.tag());
        if (escorts) {
            out.putBoolean("thaumaturgicconcilium:escorts", true);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
        super.readAdditionalSaveData(in.tag());
        escorts = in.getBooleanOr("thaumaturgicconcilium:escorts", false);
    }
}
