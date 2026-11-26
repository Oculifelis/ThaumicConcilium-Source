package com.thaumaturgicconcilium.compat.bases.link;

import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.thaumaturgicbases.TBDataMaps;
import com.thaumaturgicbases.TBTobacco;
import com.thaumaturgicbases.item.TobaccoEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Doped Squid (TC4 Concilium {@code DopeSquid}): a squid infused in a Thaumic Horizons vat with Thaumic Bases tobacco. It
 * swims through the air after its owner (catching up when more than 15 blocks off) and, every 20 seconds, puffs smoke;
 * if the owner then carries 10 or more temporary warp, the squid smokes it off them, 10 at a time, with the tobacco
 * given to it (as from a greatwood pipe). Tame and heal it (5) with tobacco leaves; the owner gives it a tobacco by using
 * one on it, and otherwise sits or wakes it. TC4 began every squid on tobacco 9 (no such tobacco), so an untold squid
 * still took the warp and did nothing; a squid with no tobacco now leaves the warp alone.
 */
public class DopeSquidEntity extends TamableAnimal {
    private static final int SMOKE_PERIOD = 400;
    private static final int WARP_COST = 10;
    private static final double LEASH_SQ = 225.0;

    public float xBodyRot;
    public float xBodyRotO;
    public float zBodyRot;
    public float zBodyRotO;
    public float tentacleAngle;
    public float oldTentacleAngle;
    private float tentacleMovement;
    private float tentacleSpeed;
    private float speed;
    private float rotateSpeed;
    private int charge;
    private @Nullable Holder<Item> tobacco;

    public DopeSquidEntity(EntityType<? extends DopeSquidEntity> type, Level level) {
        super(type, level);
        tentacleSpeed = 1.0F / (random.nextFloat() + 1.0F) * 0.2F;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(TBTobacco.TOBACCO_LEAVES.get());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (isFood(stack)) {
            if (!isTame()) {
                tame(player);
                setOrderedToSit(true);
                level().broadcastEntityEvent(this, (byte) 7);
            } else if (isOwnedBy(player)) {
                heal(5.0F);
                level().broadcastEntityEvent(this, (byte) 7);
            } else {
                return InteractionResult.PASS;
            }
            stack.consume(1, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (isTame() && isOwnedBy(player)) {
            if (stack.typeHolder().getData(TBDataMaps.TOBACCO) != null) {
                tobacco = stack.typeHolder();
            } else {
                setOrderedToSit(!isOrderedToSit());
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        xBodyRotO = xBodyRot;
        zBodyRotO = zBodyRot;
        oldTentacleAngle = tentacleAngle;
        tentacleMovement += tentacleSpeed;
        if (tentacleMovement > Mth.TWO_PI) {
            tentacleMovement -= Mth.TWO_PI;
            if (random.nextInt(10) == 0) {
                tentacleSpeed = 1.0F / (random.nextFloat() + 1.0F) * 0.2F;
            }
        }
        if (isOrderedToSit()) {
            setNoGravity(false);
            tentacleAngle = Mth.abs(Mth.sin(tentacleMovement)) * Mth.PI * 0.25F;
            if (!level().isClientSide()) {
                Vec3 v = getDeltaMovement();
                setDeltaMovement(0.0, (v.y - 0.08) * 0.98, 0.0);
            }
            xBodyRot += (-90.0F - xBodyRot) * 0.02F;
            return;
        }
        setNoGravity(true);
        if (tentacleMovement < Mth.PI) {
            float f = tentacleMovement / Mth.PI;
            tentacleAngle = Mth.sin(f * f * Mth.PI) * Mth.PI * 0.25F;
            if (f > 0.75F) {
                speed = 0.9F;
                rotateSpeed = 1.0F;
            } else {
                rotateSpeed *= 0.8F;
            }
        } else {
            tentacleAngle = 0.0F;
            speed *= 0.9F;
            rotateSpeed *= 0.99F;
        }
        if (tickCount % SMOKE_PERIOD == 0) {
            charge = 10;
            playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.0F);
        }
        if (charge > 0) {
            Vec3 look = getLookAngle().scale(0.2);
            level().addParticle(ParticleTypes.POOF, getX() + look.x, getEyeY() + look.y, getZ() + look.z, look.x, look.y, look.z);
            charge--;
        }
        if (level() instanceof ServerLevel server && getOwner() instanceof ServerPlayer owner && owner.level() == server) {
            if (tickCount % SMOKE_PERIOD == 0) {
                smokeFor(owner);
            }
            followOwner(owner);
        }
        Vec3 v = getDeltaMovement();
        double horizontal = v.horizontalDistance();
        yBodyRot += (-((float) Mth.atan2(v.x, v.z)) * Mth.RAD_TO_DEG - yBodyRot) * 0.1F;
        setYRot(yBodyRot);
        zBodyRot += Mth.PI * rotateSpeed * 1.5F;
        xBodyRot += (-((float) Mth.atan2(horizontal, v.y)) * Mth.RAD_TO_DEG - xBodyRot) * 0.1F;
    }

    private void smokeFor(ServerPlayer owner) {
        if (tobacco == null || WarpHelper.getWarp(owner).get(WarpType.TEMPORARY) < WARP_COST) {
            return;
        }
        TobaccoEffect effect = tobacco.getData(TBDataMaps.TOBACCO);
        if (effect == null) {
            return;
        }
        effect.smoke(owner, TobaccoEffect.Pipe.GREATWOOD);
        WarpHelper.addWarp(owner, -WARP_COST, WarpType.TEMPORARY);
    }

    /** TC4: drift toward a random block next to the owner, rising while below them, at the stroke's speed. */
    private void followOwner(LivingEntity owner) {
        double dx = owner.getX() + (random.nextInt(3) - 1) - getX();
        double dy = owner.getY() + (random.nextInt(3) - 1) - getY();
        double dz = owner.getZ() + (random.nextInt(3) - 1) - getZ();
        double d = Math.max(1.0E-4, Math.sqrt(dx * dx + dy * dy + dz * dz));
        Vec3 v = getDeltaMovement();
        double vx = v.x + dx / d * 0.1;
        double vy = v.y + dy / d * 0.1 + (getY() < owner.getY() + 2.0 ? 0.1 : 0.0);
        double vz = v.z + dz / d * 0.1;
        setDeltaMovement(vx * speed, vy * speed, vz * speed);
        if (distanceToSqr(owner) > LEASH_SQ) {
            setPos(owner.getX() + (random.nextInt(3) - 1), owner.getY() + (random.nextInt(3) - 1), owner.getZ() + (random.nextInt(3) - 1));
        }
    }

    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int n = random.nextInt(3) + 1;
        for (int i = 0; i < n; i++) {
            spawnAtLocation(level, new ItemStack(Items.INK_SAC));
        }
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (tobacco != null) {
            tobacco.unwrapKey().ifPresent(k -> out.putString("Tobacco", k.identifier().toString()));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        tobacco = in.getString("Tobacco").map(Identifier::tryParse)
                .flatMap(id -> BuiltInRegistries.ITEM.get(ResourceKey.create(Registries.ITEM, id))).map(h -> (Holder<Item>) h).orElse(null);
    }
}
