package com.thaumaturgicconcilium.compat.horizons;

import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.thaumaturgicconcilium.spell.SpellUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Golem Bydlo (TC4 Concilium {@code GolemBydlo}): a 2 x 3.5 thaumium brute made by infusing a Mad Thaumaturge in a
 * Thaumic Horizons vat. It has no behaviour of its own; use it (not sneaking, not with a name tag) to ride it. The rider steers;
 * swinging a wand while riding makes it strike:
 * <ul>
 * <li>a focus that breaks or mines blocks: it smashes the 5 x 5 square of pickaxe blocks on the face the rider looks at
 * (reach 5), with the focus's fortune or silk touch;</li>
 * <li>a Wrath Attraction focus: it hits everything around it (2 blocks out, 1 up), the rider excepted.</li>
 * </ul>
 * A hit does 10..24 and throws the target up. Left alone it stays within 48 blocks of where it was last left.
 */
public class GolemBydloEntity extends AbstractGolem {
    private static final int HOME_RANGE = 48;
    private static final int ACTION_TICKS = 6;
    private static final byte EVENT_STRIKE = 4;
    private static final double REACH = 5.0;

    private int action;

    public GolemBydloEntity(EntityType<? extends GolemBydloEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractGolem.createMobAttributes().add(Attributes.MAX_HEALTH, 90.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** The strike timer the model swings both arms on (TC4 {@code getActionTimer}). */
    public int actionTimer() {
        return action;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    // ---------------------------------------------------------------- riding

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG) || isVehicle()) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide()) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 travel) {
        super.tickRidden(rider, travel);
        setRot(rider.getYRot(), rider.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    /** TC4: strafing at half speed, full speed forwards and back. */
    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travel) {
        return new Vec3(rider.xxa * 0.5F, 0.0, rider.zza);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /** TC4 {@code updateRiderPosition}: the rider sits half a block forward and 0.15 up on the shoulders. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0.0, 0.15, 0.5);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        setHomeTo(blockPosition(), HOME_RANGE);
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void aiStep() {
        super.aiStep();
        if (action > 0) {
            action--;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!hasHome()) {
            setHomeTo(blockPosition(), HOME_RANGE);
        }
        if (getControllingPassenger() instanceof Player rider) {
            if (rider.swinging && action == 0) {
                strike(level, rider);
            }
        } else if (distanceToSqr(Vec3.atBottomCenterOf(getHomePosition())) >= HOME_RANGE * HOME_RANGE || isInWall()) {
            returnHome(level);
        }
    }

    /**
     * TC4 teleported back to its home when 48 blocks away or stuck in a wall, but never set a home, so it was pulled to
     * 0,0,0, and it did so even with a rider. Here home is where it was last left (or where it appeared), and only an
     * unridden golem is pulled back.
     */
    private void returnHome(ServerLevel level) {
        BlockPos home = getHomePosition();
        for (int dy = 1; dy >= -1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos at = home.offset(dx, dy, dz);
                    if (level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP) && !level.getBlockState(at).isSolidRender()) {
                        snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, getYRot(), getXRot());
                        getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }

    private void strike(ServerLevel level, Player rider) {
        ItemStack wand = rider.getMainHandItem();
        if (!(wand.getItem() instanceof ItemWand)) {
            return;
        }
        Spell spell = focusSpell(wand);
        if (spell == null) {
            return;
        }
        if (has(spell, "thaumaturge", "break") || has(spell, "thaumaturge", "mine")) {
            HitResult hit = rider.pick(REACH, 1.0F, false);
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
                swingArms(level);
                excavate(level, rider, block, fortune(spell), has(spell, "thaumaturge", "silk_touch"));
            }
        } else if (has(spell, "thaumaturgicconcilium", "wrath_attraction")) {
            swingArms(level);
            List<LivingEntity> around = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.0, 1.0, 2.0),
                    e -> e != this && e != rider && e.isAlive());
            for (LivingEntity e : around) {
                doHurtTarget(level, e);
            }
        }
    }

    private void swingArms(ServerLevel level) {
        action = ACTION_TICKS;
        level.broadcastEntityEvent(this, EVENT_STRIKE);
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
    }

    /** TC4: every pickaxe block in the 5 x 5 square across the struck face, with the focus's fortune or silk touch. */
    private void excavate(ServerLevel level, Player rider, BlockHitResult hit, int fortune, boolean silk) {
        BlockPos centre = hit.getBlockPos();
        if (!level.getBlockState(centre).is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return;
        }
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (silk) {
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else if (fortune > 0) {
            tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
        }
        Direction.Axis axis = hit.getDirection().getAxis();
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                BlockPos pos = switch (axis) {
                    case Y -> centre.offset(a, 0, b);
                    case Z -> centre.offset(a, b, 0);
                    case X -> centre.offset(0, b, a);
                };
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || !state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        || !level.mayInteract(rider, pos)) {
                    continue;
                }
                BlockEntity be = level.getBlockEntity(pos);
                Block.dropResources(state, level, pos, be, rider, tool);
                level.destroyBlock(pos, false, rider);
            }
        }
    }

    private static @Nullable Spell focusSpell(ItemStack wand) {
        ItemStackTemplate socketed = wand.get(TTDataComponents.SOCKETED_FOCUS.get());
        return socketed == null ? null : Spells.spellOf(socketed.create());
    }

    private static boolean has(Spell spell, String ns, String path) {
        ResourceKey<SpellPart> part = ResourceKey.create(SpellPart.REGISTRY_KEY, Identifier.fromNamespaceAndPath(ns, path));
        boolean[] found = {false};
        spell.root().forEach((SpellNode node) -> found[0] |= node.part().equals(part));
        return found[0];
    }

    /** The focus's fortune level (Thaumaturge's {@code fortune} part, setting {@code level}). */
    private static int fortune(Spell spell) {
        ResourceKey<SpellPart> part = SpellUtil.part("fortune");
        ResourceKey<SpellPart> ttPart = ResourceKey.create(SpellPart.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", "fortune"));
        int[] level = {0};
        spell.root().forEach((SpellNode node) -> {
            if (node.part().equals(ttPart) || node.part().equals(part)) {
                level[0] = Math.max(level[0], node.settings().getOrDefault("level", 1));
            }
        });
        return level[0];
    }

    // ---------------------------------------------------------------- combat and sounds

    /** TC4: 10 + 0..14 damage, the target thrown 0.4 up, the iron golem's throw sound. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        DamageSource source = damageSources().mobAttack(this);
        boolean hurt = target.hurtServer(level, source, 10 + random.nextInt(15));
        if (hurt) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
        }
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_STRIKE) {
            action = ACTION_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }
}
