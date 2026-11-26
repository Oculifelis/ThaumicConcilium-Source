package com.thaumaturgicconcilium.spell.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.spell.TCSpells;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.thaumaturgicconcilium.util.ValueInput;
import com.thaumaturgicconcilium.util.ValueOutput;

/**
 * The Signal and Impulse foci's projectile: a puff of compressed air that flies straight along its launch velocity,
 * thumps every 5 ticks and goes off at the end of its life. With a firework it bursts into that firework (and, for
 * Flashbang, blinds everything near); without one it explodes, or (Pneumatic Strike) throws everything near outward.
 * Vacuum pulls non-player creatures toward it while it flies.
 */
public class CompressedBlastEntity extends Entity implements GlowingOrb {
    private static final EntityDataAccessor<ItemStack> FIREWORK = SynchedEntityData.defineId(CompressedBlastEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final byte BURST_EVENT = 17;

    /**
     * What the blast does.
     *
     * @param life            ticks before it goes off
     * @param explosion       explosion strength at the end, 0 for none (fireworks never explode)
     * @param knockRadius     above 0: Pneumatic Strike, no explosion; creatures this near are thrown outward
     * @param knockSpeed      horizontal and upward speed of that throw
     * @param vacuumRadius    above 0: non-player creatures this near are pulled toward the blast every tick
     * @param vacuumStrength  speed of that pull
     * @param blindRadius     above 0 with a firework: Flashbang radius
     * @param blindTicks      Flashbang blindness duration
     * @param blindAmplifier  Flashbang blindness amplifier
     * @param thump           sound every 5 ticks
     */
    public record Blast(int life, float explosion, double knockRadius, double knockSpeed, double vacuumRadius, double vacuumStrength,
            double blindRadius, int blindTicks, int blindAmplifier, Optional<ResourceLocation> thump) {
        public static final MapCodec<Blast> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("life").forGetter(Blast::life),
                Codec.FLOAT.optionalFieldOf("explosion", 0.0F).forGetter(Blast::explosion),
                Codec.DOUBLE.optionalFieldOf("knock_radius", 0.0).forGetter(Blast::knockRadius),
                Codec.DOUBLE.optionalFieldOf("knock_speed", 0.0).forGetter(Blast::knockSpeed),
                Codec.DOUBLE.optionalFieldOf("vacuum_radius", 0.0).forGetter(Blast::vacuumRadius),
                Codec.DOUBLE.optionalFieldOf("vacuum_strength", 0.0).forGetter(Blast::vacuumStrength),
                Codec.DOUBLE.optionalFieldOf("blind_radius", 0.0).forGetter(Blast::blindRadius),
                Codec.INT.optionalFieldOf("blind_ticks", 0).forGetter(Blast::blindTicks),
                Codec.INT.optionalFieldOf("blind_amplifier", 0).forGetter(Blast::blindAmplifier),
                ResourceLocation.CODEC.optionalFieldOf("thump").forGetter(Blast::thump)).apply(i, Blast::new));
    }

    private Blast blast = new Blast(20, 0.0F, 0, 0, 0, 0, 0, 0, 0, Optional.empty());
    private int age;
    private Optional<UUID> owner = Optional.empty();

    public CompressedBlastEntity(EntityType<? extends CompressedBlastEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A blast at {@code from} flying at {@code velocity}; {@code firework} may be empty. */
    public static CompressedBlastEntity launch(ServerLevel level, Vec3 from, Vec3 velocity, Blast blast, ItemStack firework, Entity owner) {
        CompressedBlastEntity entity = new CompressedBlastEntity(TCSpells.COMPRESSED_BLAST.get(), level);
        entity.setPos(from.x, from.y, from.z);
        entity.setDeltaMovement(velocity);
        entity.blast = blast;
        entity.owner = owner == null ? Optional.empty() : Optional.of(owner.getUUID());
        entity.entityData.set(FIREWORK, firework.copy());
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FIREWORK, ItemStack.EMPTY);
    }

    private ItemStack firework() {
        return entityData.get(FIREWORK);
    }

    @Override
    public int glowColor() {
        Fireworks fireworks = firework().get(DataComponents.FIREWORKS);
        if (fireworks != null && !fireworks.explosions().isEmpty() && !fireworks.explosions().getFirst().colors().isEmpty()) {
            return fireworks.explosions().getFirst().colors().getInt(0);
        }
        return 0xFF3333;
    }

    @Override
    public float glowScale() {
        return 0.6F;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        age++;
        Level level = level();
        if (level.isClientSide()) {
            level.addParticle(ParticleTypes.FIREWORK, getX(), getY(), getZ(), random.nextGaussian() * 0.05, -v.y * 0.5, random.nextGaussian() * 0.05);
            if (age % 5 == 0) {
                level.addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        ServerLevel server = (ServerLevel) level;
        if (blast.vacuumRadius() > 0) {
            for (LivingEntity near : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(blast.vacuumRadius()),
                    e -> e.isAlive() && !(e instanceof Player))) {
                Vec3 to = position().subtract(near.position());
                if (to.length() > 1.0) {
                    near.setDeltaMovement(to.normalize().scale(blast.vacuumStrength()));
                    near.hurtMarked = true;
                }
            }
        }
        if (age % 5 == 0 && blast.thump().isPresent()) {
            Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getHolder(blast.thump().get()).map(h -> (Holder<SoundEvent>) h)
                    .orElseGet(() -> Holder.direct(SoundEvent.createVariableRangeEvent(blast.thump().get())));
            server.playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, 0.9F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
        }
        if (age > blast.life()) {
            goOff(server);
        }
    }

    private void goOff(ServerLevel level) {
        ItemStack firework = firework();
        Entity source = owner.map(level::getEntity).orElse(null);
        if (firework.isEmpty()) {
            if (blast.knockRadius() > 0) {
                for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(blast.knockRadius()), Entity::isAlive)) {
                    Vec3 out = new Vec3(near.getX() - getX(), 0, near.getZ() - getZ()).normalize();
                    near.setDeltaMovement(out.x * blast.knockSpeed(), blast.knockSpeed(), out.z * blast.knockSpeed());
                    near.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            } else if (blast.explosion() > 0) {
                level.explode(source, getX(), getY(), getZ(), blast.explosion(), Level.ExplosionInteraction.MOB);
            }
        } else {
            level.broadcastEntityEvent(this, BURST_EVENT);
            if (blast.blindRadius() > 0) {
                for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(blast.blindRadius()), Entity::isAlive)) {
                    near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blast.blindTicks(), blast.blindAmplifier()));
                }
            }
        }
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == BURST_EVENT) {
            Fireworks fireworks = firework().get(DataComponents.FIREWORKS);
            List<FireworkExplosion> explosions = fireworks == null ? List.of() : fireworks.explosions();
            if (!explosions.isEmpty()) {
                Vec3 v = getDeltaMovement();
                level().createFireworks(getX(), getY(), getZ(), v.x, v.y, v.z, explosions);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    /** A firework rocket stack carrying one explosion, for {@link #launch}. */
    public static ItemStack firework(FireworkExplosion explosion) {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));
        return stack;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueInput in = new ValueInput(tag$, registryAccess());
        blast = in.read(Blast.CODEC).orElse(blast);
        age = in.getIntOr("age", 0);
        owner = in.read("owner", UUIDUtil.CODEC);
        entityData.set(FIREWORK, in.read("firework", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag$) {
        ValueOutput out = new ValueOutput(tag$, registryAccess());
        out.store(Blast.CODEC, blast);
        out.putInt("age", age);
        owner.ifPresent(id -> out.store("owner", UUIDUtil.CODEC, id));
        out.store("firework", ItemStack.OPTIONAL_CODEC, firework());
    }
}
