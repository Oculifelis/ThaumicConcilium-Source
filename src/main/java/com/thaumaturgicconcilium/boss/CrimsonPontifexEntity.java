package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.entity.ai.CultistHurtByTargetGoal;
import com.leclowndu93150.thaumaturge.content.entity.boss.EntityCultistLeader;
import com.leclowndu93150.thaumaturge.content.entity.boss.EntityThaumaturgeBoss;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.mob.EnslavePayload;
import com.thaumaturgicconcilium.mob.MobUtil;
import com.thaumaturgicconcilium.mob.ThaumaturgeTrade;
import com.thaumaturgicconcilium.spell.entity.ConcentratedWarpChargeEntity;
import com.thaumaturgicconcilium.spell.entity.WarpChargeImmune;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The Crimson Pontifex (TC4 Concilium {@code CrimsonPontifex}), the Crimson Cult's lost leader, brought back through a
 * chained rift and a Lesser Crimson Portal. A Thaumaturge boss (boss bar, no despawning) with 150 health, a 25-damage
 * hammer and the full Pontifex robe.
 * <ul>
 * <li>Every hit on him is divided by (crimson cultists within 8 + 3), and those cultists keep Regeneration II.</li>
 * <li>Every 20 ticks of sight of a player within 16 he casts one of: a Concentrated Warp Charge on the player (unless
 * one of theirs is already within 7); mind control (the Vengeful Golem's enslavement, ten ticks); a Lesser Crimson
 * Portal near him (fewer than 3 within 64); or a life drain on every living thing within 6 that is not a cultist, 25
 * damage each and 10 health back for each.</li>
 * <li>He drops a rare loot bag, and his killer comes away with the start of one lost research (data:
 * {@code thaumaturgicconcilium:crimson_pontifex} trade hints; Peeler of Matter 7, Hammer 2, Robe 1 in 10).</li>
 * </ul>
 * Fixes: TC4's life drain removed the Pontifex from the wrong list, so it hit and healed him and only fired with a
 * cultist near; the cultist count never dropped back to zero once cultists had been near.
 */
public class CrimsonPontifexEntity extends EntityThaumaturgeBoss implements WarpChargeImmune {
    private static final List<String> TITLES = List.of("Ivius", "Ufarihm", "Ihith", "Pemonar", "Shagron", "Ugimaex", "Qroleus", "Oxon", "Rheforn",
            "Zubras");
    private static final ResourceKey<ThaumaturgeTrade> LOST_RESEARCH = ResourceKey.create(ThaumaturgeTrade.REGISTRY_KEY,
            ThaumaturgicConcilium.id("crimson_pontifex"));
    private static final double ATTACK_RANGE = 16.0;
    private static final DustParticleOptions DRAIN = new DustParticleOptions(0xB01020, 1.2F);

    private int title;
    private int attackCounter;
    private int aggroCooldown;
    private int cultists;
    private boolean continuousAttack;
    private @Nullable LivingEntity targeted;

    public CrimsonPontifexEntity(EntityType<? extends CrimsonPontifexEntity> type, Level level) {
        super(type, level);
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MAX_HEALTH, 150.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 25.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.7, false));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ---------------------------------------------------------------- spawn, name, gear
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        equip();
        ItemStack weapon = getMainHandItem();
        if (getRandom().nextFloat() < 0.5F * difficulty.getSpecialMultiplier()) {
            EnchantmentHelper.enchantItemFromProvider(weapon, level.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty,
                    getRandom());
        }
        title = getRandom().nextInt(TITLES.size());
        assignTitle();
        return data;
    }

    /** TC4 addRandomArmor: the hammer and the full robe. */
    public void equip() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TCBosses.PONTIFEX_HAMMER.get()));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(TCBosses.PONTIFEX_ROBE_HEAD.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(TCBosses.PONTIFEX_ROBE_CHEST.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(TCBosses.PONTIFEX_ROBE_LEGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(TCBosses.PONTIFEX_ROBE_FEET.get()));
    }

    /** "Pontifex Ivius" and so on (TC4 added the champion modifier's name, which Thaumaturge does not expose). */
    @Override
    public void assignTitle() {
        setCustomName(Component.translatable("entity.thaumaturgicconcilium.crimson_pontifex.titled", TITLES.get(Math.floorMod(title, TITLES.size())))
                .withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean considersEntityAsAlly(Entity other) {
        return other instanceof EntityCultist || other instanceof EntityCultistLeader || other instanceof CrimsonPontifexEntity
                || other instanceof LesserPortalEntity || super.considersEntityAsAlly(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof EntityCultist || target instanceof EntityCultistLeader) && super.canAttack(target);
    }

    // ---------------------------------------------------------------- damage
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getDirectEntity() instanceof LivingEntity direct && direct != this) {
            targeted = direct;
            aggroCooldown = 200;
        }
        if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            targeted = attacker;
            aggroCooldown = 200;
        }
        return super.hurtServer(level, source, damage / (cultists + 3));
    }

    // ---------------------------------------------------------------- behaviour (TC4 updateAITasks)
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        List<EntityCultist> near = level.getEntitiesOfClass(EntityCultist.class, getBoundingBox().inflate(8.0));
        cultists = near.size();
        for (EntityCultist cultist : near) {
            if (!cultist.hasEffect(MobEffects.REGENERATION)) {
                cultist.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
            }
        }
        if (targeted != null && !targeted.isAlive()) {
            targeted = null;
        }
        --aggroCooldown;
        if (random.nextInt(10) > 7 && (targeted == null || aggroCooldown-- <= 0)) {
            Player player = level.getNearestPlayer(this, 32.0);
            targeted = player != null && !player.isCreative() && !player.isSpectator() ? player : null;
            if (targeted != null) {
                aggroCooldown = 50;
            }
        }
        if (targeted == null || targeted.distanceToSqr(this) >= ATTACK_RANGE * ATTACK_RANGE) {
            return;
        }
        if (!hasLineOfSight(targeted)) {
            if (attackCounter > 0) {
                attackCounter--;
            }
            return;
        }
        attackCounter++;
        if (attackCounter > 10 && continuousAttack) {
            if (targeted instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new EnslavePayload(false));
            }
            continuousAttack = false;
        }
        if (attackCounter == 20) {
            if (targeted instanceof ServerPlayer player) {
                cast(level, player);
            }
            attackCounter = -20 + random.nextInt(20);
        }
    }

    private void cast(ServerLevel level, ServerPlayer player) {
        switch (random.nextInt(4)) {
            case 0 -> {
                boolean found = !level.getEntitiesOfClass(ConcentratedWarpChargeEntity.class, player.getBoundingBox().inflate(7.0),
                        c -> c.isOwnedBy(player)).isEmpty();
                if (!found) {
                    ConcentratedWarpChargeEntity.byForce(level, player);
                }
            }
            case 1 -> {
                PacketDistributor.sendToPlayer(player, new EnslavePayload(true));
                continuousAttack = true;
            }
            case 2 -> openPortal(level);
            default -> drainLife(level);
        }
    }

    private void openPortal(ServerLevel level) {
        if (level.getEntitiesOfClass(LesserPortalEntity.class, getBoundingBox().inflate(64.0)).size() >= 3) {
            return;
        }
        int x = getBlockX() + random.nextInt(5) - random.nextInt(5);
        int z = getBlockZ() + random.nextInt(5) - random.nextInt(5);
        BlockPos at = new BlockPos(x, getBlockY(), z);
        if (x == getBlockX() || z == getBlockZ() || !level.getBlockState(at).isAir()) {
            return;
        }
        LesserPortalEntity portal = TCBosses.LESSER_PORTAL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (portal == null) {
            return;
        }
        portal.snapTo(x, getY(), z, random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(portal);
        Vec3 from = position().add(0.0, getBbHeight() * 0.75, 0.0);
        for (int i = 0; i < 12; i++) {
            Vec3 p = from.lerp(new Vec3(x + 0.5, getY() + 1.0, z + 0.5), i / 12.0);
            level.sendParticles(DRAIN, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, getX(), getY(), getZ(), TTSounds.WANDFAIL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    /** 25 damage to every living thing within 6 that is not of the cult (and not the Pontifex), +10 health per hit. */
    private void drainLife(ServerLevel level) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0),
                e -> e != this && e.isAlive() && !(e instanceof EntityCultist) && !(e instanceof EntityCultistLeader)
                        && !(e instanceof CrimsonPontifexEntity) && !(e instanceof LesserPortalEntity))) {
            if (e.hurtServer(level, damageSources().mobAttack(this), damage)) {
                heal(10.0F);
            }
            Vec3 to = position().add(0.0, getBbHeight() * 0.6, 0.0);
            Vec3 from = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            for (int i = 0; i < 8; i++) {
                Vec3 p = from.lerp(to, i / 8.0);
                level.sendParticles(DRAIN, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    // ---------------------------------------------------------------- drops, sounds, save
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (source.getEntity() instanceof ServerPlayer player) {
            ThaumaturgeTrade.get(level, LOST_RESEARCH).ifPresent(t -> revealLostResearch(player, t.hints()));
        }
    }

    /** One weighted lost research: started for the killer when its parents are known, otherwise a hint towards it. */
    private void revealLostResearch(ServerPlayer player, List<ThaumaturgeTrade.Hint> hints) {
        int total = hints.stream().mapToInt(ThaumaturgeTrade.Hint::weight).sum();
        if (total <= 0) {
            return;
        }
        int roll = random.nextInt(total);
        for (ThaumaturgeTrade.Hint hint : hints) {
            roll -= hint.weight();
            if (roll < 0) {
                if (ResearchManager.unlock(player, hint.research())) {
                    Component name = Component.translatable("research." + hint.research().getNamespace() + "." + hint.research().getPath() + ".title");
                    player.sendSystemMessage(Component.translatable("message.thaumaturgicconcilium.pontifex.research", name)
                            .withStyle(ChatFormatting.DARK_PURPLE));
                    MobUtil.play(level(), player, MobUtil.sound("thaumaturge:learn"), SoundSource.PLAYERS, 0.5F, 1.0F);
                } else {
                    MobUtil.giveHint(player, List.of(hint), random);
                }
                return;
            }
        }
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 450;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MobUtil.tcSound("chant").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobUtil.tcSound("growl").get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putByte("title", (byte) title);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        title = in.getByteOr("title", (byte) 0);
    }
}
