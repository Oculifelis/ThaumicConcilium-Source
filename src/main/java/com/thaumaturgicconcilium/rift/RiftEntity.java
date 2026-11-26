package com.thaumaturgicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.content.entity.EntityEldritchOrb;
import com.leclowndu93150.thaumaturge.content.entity.EntityMindSpider;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.TCUtil;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.MobUtil;
import com.thaumaturgicconcilium.spell.ConductorLock;
import com.thaumaturgicconcilium.spell.ConductorTarget;
import com.thaumaturgicconcilium.spell.SpellUtil;
import com.thaumaturgicconcilium.spell.entity.ChargedWispEntity;
import com.thaumaturgicconcilium.spell.entity.ChargedWispTarget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Flux Rift (TC4 {@code RiftEntity}): a tear in the world, a tube of points grown from its seed whose length follows its
 * size. Three kinds:
 * <ul>
 * <li>wild, opened by breaking an aura node: grows to half the node's vis (5..80), chews carriable blocks along its tube,
 * hurts what touches it, deletes items after 5 seconds, and every 15 seconds fires one of seven events; collapsing to
 * nothing taints the ground and leaves void seeds;</li>
 * <li>chained, built on a Hex of Predictability from a jarred node: it crafts (Chained Rift recipes) and turns into the
 * Brightest One, a Lesser Crimson Portal or a Peeler of Matter when the right offering is made;</li>
 * <li>interdimensional, summoned by a Gem of Rifts: touching it with the gem in hand moves the holder to and from the
 * Causal Bouillon.</li>
 * </ul>
 * The Polishment Device deletes any rift; the Vis Conductor's Dematerialization shrinks one with an Auram crystal.
 */
public class RiftEntity extends Entity implements ChargedWispTarget, ConductorTarget {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLLAPSE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INTERDIMENSIONAL = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHAINED = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);

    /** Chained rift outcomes (TC4 {@code STATE}). */
    public static final int STATE_NONE = 0;
    public static final int STATE_BRIGHTEST_ONE = 1;
    public static final int STATE_PONTIFEX = 2;
    public static final int STATE_PEELER = 3;

    /** The other phase 3 module's entities, looked up by id so the rift works while they are missing. */
    public static final Identifier BRIGHTEST_ONE = ThaumaturgicConcilium.id("brightest_one");
    public static final Identifier LESSER_PORTAL = ThaumaturgicConcilium.id("lesser_crimson_portal");
    public static final Identifier LESSER_PORTAL_FALLBACK = Identifier.fromNamespaceAndPath("thaumaturge", "cultist_portal_lesser");
    public static final Identifier PEELER = ThaumaturgicConcilium.id("material_peeler");
    /** Entity tag put on the portal a Pontifex offering opens (TC4 set {@code portal.boss = true}). */
    public static final String BOSS_PORTAL_TAG = "thaumaturgicconcilium.rift_boss";

    public static final Identifier RESEARCH_RIFTS = ThaumaturgicConcilium.id("rifts");
    public static final Identifier RESEARCH_BRIGHTEST_ONE = ThaumaturgicConcilium.id("brightest_one");
    public static final Identifier RESEARCH_PONTIFEX = ThaumaturgicConcilium.id("crimson_pontifex");
    public static final Identifier RESEARCH_PEELER = ThaumaturgicConcilium.id("material_peeler");
    private static final Identifier CRIMSON_RITES = Identifier.fromNamespaceAndPath("thaumaturge", "curio_rites");

    private static final int EVENT_INTERVAL = 300;
    private static final double EVENT_RANGE = 7.0;
    private static final double TOUCH = 0.5;

    private int maxSize;
    private int lastSize = -1;
    private Optional<BlockPos> hex = Optional.empty();
    /** Tube points relative to the entity position, and their girths; rebuilt whenever the size changes. */
    public final List<Vec3> points = new ArrayList<>();
    public final List<Float> widths = new ArrayList<>();
    private double yOffset;
    private AABB shape = new AABB(-1, 0, -1, 1, 2, 1);

    public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // ------------------------------------------------------------------ spawning (TC4 createRift / summonRift / constructRift)

    /** A node broke: a wild rift growing to half the node's vis, 5..80 (TC4 {@code createRift}); nothing below 5. */
    public static void openWild(ServerLevel level, Vec3 at, int nodeVis) {
        int amount = nodeVis / 2;
        if (amount < 5) {
            return;
        }
        RiftEntity rift = new RiftEntity(TCRifts.FLUX_RIFT.get(), level);
        rift.snapTo(at.x, at.y, at.z, level.getRandom().nextInt(360), 0.0F);
        rift.setSeed(level.getRandom().nextInt());
        rift.setRiftSize(5);
        rift.maxSize = Mth.clamp(amount, 5, 80);
        rift.setGrowing();
        level.addFreshEntity(rift);
    }

    /** A Gem of Rifts summoned an interdimensional rift growing to 50 (TC4 {@code summonRift}). */
    public static void summon(ServerLevel level, Vec3 at) {
        RiftEntity rift = new RiftEntity(TCRifts.FLUX_RIFT.get(), level);
        rift.snapTo(at.x, at.y, at.z, level.getRandom().nextInt(360), 0.0F);
        rift.setSeed(level.getRandom().nextInt());
        rift.entityData.set(INTERDIMENSIONAL, true);
        rift.maxSize = 50;
        rift.setGrowing();
        level.addFreshEntity(rift);
    }

    /** A jarred node went onto a Hex: a chained rift above it growing to {@code size} (TC4 {@code constructRift}). */
    public static void construct(ServerLevel level, BlockPos above, BlockPos hexMaster, int size) {
        RiftEntity rift = new RiftEntity(TCRifts.FLUX_RIFT.get(), level);
        rift.snapTo(above.getX() + 0.5, above.getY(), above.getZ() + 0.5, level.getRandom().nextInt(360), 0.0F);
        rift.setSeed(level.getRandom().nextInt());
        rift.entityData.set(CHAINED, true);
        rift.hex = Optional.of(hexMaster);
        rift.setRiftSize(5);
        rift.maxSize = Mth.clamp(size, 5, 150);
        rift.setGrowing();
        level.addFreshEntity(rift);
    }

    // ------------------------------------------------------------------ data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 5);
        builder.define(SEED, 0);
        builder.define(COLLAPSE, 0);
        builder.define(STATE, STATE_NONE);
        builder.define(INTERDIMENSIONAL, false);
        builder.define(CHAINED, false);
    }

    public int riftSize() {
        return entityData.get(SIZE);
    }

    public void setRiftSize(int size) {
        entityData.set(SIZE, Math.max(0, size));
    }

    public int seed() {
        return entityData.get(SEED);
    }

    private void setSeed(int seed) {
        entityData.set(SEED, seed);
    }

    public int state() {
        return entityData.get(STATE);
    }

    public boolean interdimensional() {
        return entityData.get(INTERDIMENSIONAL);
    }

    public boolean chained() {
        return entityData.get(CHAINED);
    }

    public boolean isCollapsing() {
        return entityData.get(COLLAPSE) == 1;
    }

    public boolean isGrowing() {
        return entityData.get(COLLAPSE) == -1;
    }

    /** Shrinks one per tick down to {@code target}. */
    public void setCollapsing(int target) {
        maxSize = target;
        entityData.set(COLLAPSE, 1);
    }

    private void setGrowing() {
        entityData.set(COLLAPSE, -1);
    }

    private void setStable() {
        entityData.set(COLLAPSE, 0);
    }

    public Optional<BlockPos> hex() {
        return hex;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("MaxSize", maxSize);
        out.putInt("RiftSize", riftSize());
        out.putInt("RiftSeed", seed());
        out.putInt("Collapse", entityData.get(COLLAPSE));
        out.putInt("State", state());
        out.putBoolean("Interdimensional", interdimensional());
        out.putBoolean("Chained", chained());
        hex.ifPresent(p -> out.store("Hex", BlockPos.CODEC, p));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        maxSize = in.getIntOr("MaxSize", 0);
        setRiftSize(in.getIntOr("RiftSize", 5));
        setSeed(in.getIntOr("RiftSeed", 0));
        entityData.set(COLLAPSE, in.getIntOr("Collapse", 0));
        entityData.set(STATE, in.getIntOr("State", STATE_NONE));
        entityData.set(INTERDIMENSIONAL, in.getBooleanOr("Interdimensional", false));
        entityData.set(CHAINED, in.getBooleanOr("Chained", false));
        hex = in.read("Hex", BlockPos.CODEC);
    }

    // ------------------------------------------------------------------ shape

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (SIZE.equals(accessor) || SEED.equals(accessor)) {
            rebuild();
        }
    }

    /** TC4 {@code calcSteps}: two arms grown out from the centre, each step bent a little at random. */
    private void rebuild() {
        if (points == null) {
            return;
        }
        lastSize = riftSize();
        points.clear();
        widths.clear();
        Random rr = new Random(seed());
        Vec3 right = new Vec3(rr.nextGaussian(), rr.nextGaussian(), rr.nextGaussian()).normalize();
        Vec3 left = right.scale(-1);
        Vec3 lr = Vec3.ZERO;
        Vec3 ll = Vec3.ZERO;
        int steps = Mth.ceil(riftSize() / 3.0F);
        float girth = riftSize() / 300.0F;
        double angle = 0.33;
        float dec = steps == 0 ? 0 : girth / steps;
        for (int a = 0; a < steps; a++) {
            girth -= dec;
            right = yaw(pitch(right, (float) (rr.nextGaussian() * angle)), (float) (rr.nextGaussian() * angle));
            lr = lr.add(right.scale(0.2));
            points.add(lr);
            widths.add(girth);
            left = yaw(pitch(left, (float) (rr.nextGaussian() * angle)), (float) (rr.nextGaussian() * angle));
            ll = ll.add(left.scale(0.2));
            points.add(0, ll);
            widths.add(0, girth);
        }
        lr = lr.add(right.scale(0.1));
        points.add(lr);
        widths.add(0.0F);
        ll = ll.add(left.scale(0.1));
        points.add(0, ll);
        widths.add(0, 0.0F);

        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE;
        double x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
        for (Vec3 v : points) {
            x0 = Math.min(x0, v.x);
            y0 = Math.min(y0, v.y);
            z0 = Math.min(z0, v.z);
            x1 = Math.max(x1, v.x);
            y1 = Math.max(y1, v.y);
            z1 = Math.max(z1, v.z);
        }
        // TC4 lifted the tube by half its height when touching things but not in its box; both use the lift here
        yOffset = (y1 - y0) / 2.0;
        shape = new AABB(x0, y0 + yOffset, z0, x1, y1 + yOffset, z1).inflate(0.1);
        setBoundingBox(makeBoundingBox(position()));
    }

    private static Vec3 pitch(Vec3 v, float pitch) {
        float c = Mth.cos(pitch);
        float s = Mth.sin(pitch);
        return new Vec3(v.x, v.y * c + v.z * s, v.z * c - v.y * s);
    }

    private static Vec3 yaw(Vec3 v, float yaw) {
        float c = Mth.cos(yaw);
        float s = Mth.sin(yaw);
        return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        if (shape == null) {
            return super.makeBoundingBox(pos);
        }
        return shape.move(pos);
    }

    /** Height the tube is lifted by above the entity position. */
    public double yOffset() {
        return yOffset;
    }

    /** A tube point in world space. */
    public Vec3 point(int index) {
        return position().add(points.get(index)).add(0, yOffset, 0);
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    public void move(MoverType type, Vec3 delta) {
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(TCRegistry.POLISHMENT_DEVICE.get())) {
            if (!level().isClientSide()) {
                discard();
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public void tick() {
        super.tick();
        if (lastSize != riftSize() || points.isEmpty()) {
            rebuild();
        }
        if (!(level() instanceof ServerLevel server)) {
            clientTick();
            return;
        }
        if (seed() == 0) {
            setSeed(random.nextInt() | 1);
        }
        if (points.size() < 2) {
            return;
        }
        if (tickCount % EVENT_INTERVAL == 0) {
            playSound(TTSounds.EVILPORTAL.get(), (float) (0.15 + random.nextGaussian() * 0.066), (float) (0.75 + random.nextGaussian() * 0.1));
        }
        if (tickCount % 100 == 0) {
            for (ServerPlayer p : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(EVENT_RANGE))) {
                MobUtil.unlockResearch(p, RESEARCH_RIFTS);
            }
        }
        int pi = random.nextInt(points.size() - 1);
        Vec3 v1 = point(pi);
        if (chained()) {
            chainedTick(server, v1);
        }
        if (!interdimensional() && server.getGameRules().get(GameRules.MOB_GRIEFING)) {
            chew(server, v1, point(pi + 1));
        }
        touch(server, v1);
        if (isRemoved()) {
            return;
        }
        if (!interdimensional() && !chained() && tickCount % EVENT_INTERVAL == 0) {
            fireEvent(server);
        }
        sizeTick(server);
    }

    private void clientTick() {
        if (interdimensional() && points.size() > 1 && random.nextInt(3) == 0) {
            Vec3 v1 = point(random.nextInt(points.size() - 1));
            double fx = getX() + 0.5 + (random.nextFloat() - 0.5) * 5.0;
            double fy = getY() + 0.5 + (random.nextFloat() - 0.5) * 5.0;
            double fz = getZ() + 0.5 + (random.nextFloat() - 0.5) * 5.0;
            level().addParticle(new BoltParticleOptions(v1.x, v1.y, v1.z, 30 / 255.0F, 0.0F, 30 / 255.0F, 0.3F), fx, fy, fz, 0, 0, 0);
        }
    }

    /** A chained rift dies with its Hex; while the Hex is hot it arcs into nearby items (TC4: up to 6 within 1.4). */
    private void chainedTick(ServerLevel server, Vec3 v1) {
        HexBlockEntity master = hex.map(server::getBlockEntity).filter(HexBlockEntity.class::isInstance).map(HexBlockEntity.class::cast)
                .orElse(null);
        if (master == null) {
            setCollapsing(0);
            return;
        }
        if (master.heat() <= 0 || master.essentia().isEmpty()) {
            return;
        }
        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class, new AABB(v1, v1).inflate(1.4),
                e -> e.position().distanceToSqr(v1) <= 1.4 * 1.4);
        if (items.isEmpty()) {
            return;
        }
        playSound(TTSounds.JACOBS.get(), 0.5F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F);
        List<ResourceKey<IAspect>> aspects = new ArrayList<>(master.essentia().keySet());
        float width = master.heat() / 2400.0F * 2.0F;
        for (ItemEntity item : items.subList(0, Math.min(6, items.size()))) {
            int rgb = RiftUtil.aspectColor(server, aspects.get(random.nextInt(aspects.size())));
            RiftUtil.bolt(server, v1, item.position(), rgb, width);
        }
    }

    /** TC4: the segment between two tube points removes the first carriable, breakable, solid block it crosses. */
    private void chew(ServerLevel server, Vec3 from, Vec3 to) {
        BlockHitResult hit = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = server.getBlockState(pos);
        if (!state.isAir() && state.is(BlockTags.ENDERMAN_HOLDABLE) && state.getDestroySpeed(server, pos) >= 0.0F && state.blocksMotion()) {
            server.removeBlock(pos, false);
        }
    }

    private void touch(ServerLevel server, Vec3 v1) {
        List<Entity> near = server.getEntities(this, new AABB(v1, v1).inflate(TOUCH), e -> e.position().distanceToSqr(v1) <= TOUCH * TOUCH
                || e.getBoundingBox().contains(v1));
        for (Entity e : near) {
            if (!e.isAlive()) {
                continue;
            }
            if (e instanceof Player player) {
                if (interdimensional() && player instanceof ServerPlayer sp) {
                    ItemStack gem = RiftGemItem.heldActive(sp);
                    if (!gem.isEmpty() && !sp.isPassenger() && !sp.isVehicle()) {
                        CausalBouillon.travel(sp, gem);
                        continue;
                    }
                }
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
            }
            if (e instanceof ChargedWispEntity wisp && chained()) {
                if (onChargedWisp(wisp)) {
                    wisp.discard();
                }
                continue;
            }
            if (e instanceof ItemEntity item && chained()) {
                offering(server, item);
                continue;
            }
            if (!(e instanceof ItemEntity) && !chained()) {
                e.hurtServer(server, damageSources().fellOutOfWorld(), 2.0F);
                continue;
            }
            if (!chained() && tickCount > 100) {
                e.discard();
            }
        }
    }

    /** Crimson Rites from a Pontifex researcher opens a boss portal; Primordial Life from a Peeler researcher, the Peeler. */
    private void offering(ServerLevel server, ItemEntity item) {
        if (!(item.getOwner() instanceof Player thrower)) {
            return;
        }
        ItemStack stack = item.getItem();
        int outcome = STATE_NONE;
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(CRIMSON_RITES).map(stack::is).orElse(false)
                && researched(thrower, RESEARCH_PONTIFEX)) {
            outcome = STATE_PONTIFEX;
        } else if (stack.is(TCRegistry.PRIMORDIAL_LIFE.get()) && researched(thrower, RESEARCH_PEELER)) {
            outcome = STATE_PEELER;
        }
        if (outcome != STATE_NONE) {
            item.discard();
            entityData.set(INTERDIMENSIONAL, true);
            entityData.set(STATE, outcome);
            setCollapsing(-1);
        }
    }

    private static boolean researched(Player player, Identifier research) {
        return KnowledgeAccess.of(player).isResearchComplete(research);
    }

    /** TC4: a Charged Wisp from a Brightest One researcher turns a chained rift toward the Brightest One. */
    @Override
    public boolean onChargedWisp(ChargedWispEntity wisp) {
        if (!chained() || !(level() instanceof ServerLevel server)) {
            return false;
        }
        Optional<UUID> caster = wisp.caster();
        Player player = caster.map(server::getPlayerByUUID).orElse(null);
        if (player == null || !researched(player, RESEARCH_BRIGHTEST_ONE)) {
            return false;
        }
        entityData.set(INTERDIMENSIONAL, true);
        entityData.set(STATE, STATE_BRIGHTEST_ONE);
        setCollapsing(-1);
        return true;
    }

    /** TC4 Vis Conductor: an Auram crystal's charge shrinks the rift by {@code amount * (1 + potency)}. */
    @Override
    public boolean onDematerialize(Player caster, ConductorLock.State lock, float potency) {
        if (!lock.aspect().equals(TTAspects.AURAM)) {
            return false;
        }
        setCollapsing(Math.max(0, riftSize() - Math.round(lock.amount() * (1.0F + potency))));
        return true;
    }

    private void sizeTick(ServerLevel server) {
        if (isCollapsing()) {
            if (riftSize() > maxSize) {
                setRiftSize(riftSize() - 1);
                if (maxSize == 0 && random.nextInt(3) == 0) {
                    BlockPos at = BlockPos.containing(getX() + random.nextGaussian() * 1.5, getY(), getZ() + random.nextGaussian() * 1.5);
                    TCUtil.taintArea(server, at, 5);
                }
            } else {
                setStable();
            }
        }
        if (isGrowing()) {
            // TC4 compared with != and kept growing past a target the warp event had raised the size over
            if (riftSize() < maxSize) {
                if (random.nextInt(3) == 0) {
                    setRiftSize(riftSize() + 1);
                }
            } else {
                setStable();
            }
        }
        if (interdimensional() && (riftSize() >= maxSize || tickCount > 200)) {
            setCollapsing(-1);
        }
        if (riftSize() > 300) {
            setCollapsing(riftSize() / 2);
        }
        if (riftSize() == 0) {
            close(server);
        }
    }

    private void close(ServerLevel server) {
        Vec3 at = position();
        if (chained()) {
            switch (state()) {
                case STATE_BRIGHTEST_ONE -> SpellUtil.create(server, BRIGHTEST_ONE, at).ifPresent(e -> spawn(server, e));
                case STATE_PONTIFEX -> {
                    Optional<Entity> portal = SpellUtil.create(server, LESSER_PORTAL, at).or(() -> SpellUtil.create(server, LESSER_PORTAL_FALLBACK, at));
                    portal.ifPresent(p -> {
                        p.addTag(BOSS_PORTAL_TAG);
                        spawn(server, p);
                    });
                    playSound(TTSounds.CRAFTFAIL.get(), 1.0F, 1.0F);
                }
                case STATE_PEELER -> {
                    SpellUtil.create(server, PEELER, at).ifPresent(e -> spawn(server, e));
                    server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                    playSound(TTSounds.CRAFTFAIL.get(), 1.0F, 1.0F);
                }
                default -> {
                }
            }
        } else if (!interdimensional()) {
            int seeds = random.nextInt(12);
            if (seeds > 0) {
                spawnAtLocation(server, new ItemStack(TTItems.VOID_SEED.get(), seeds), 1.5F);
            }
        }
        discard();
    }

    private static void spawn(ServerLevel server, Entity e) {
        if (e instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.EVENT, null);
        }
        server.addFreshEntity(e);
    }

    // ------------------------------------------------------------------ wild rift events (TC4 fireEvent)

    private void fireEvent(ServerLevel server) {
        AABB area = getBoundingBox().inflate(EVENT_RANGE);
        switch (random.nextInt(10)) {
            case 0 -> {
                for (Player p : server.getEntitiesOfClass(Player.class, area)) {
                    maxSize += Mth.clamp(WarpHelper.getActualWarp(p), 2, 10);
                    setGrowing();
                }
            }
            case 1 -> {
                List<LivingEntity> list = new ArrayList<>(server.getEntitiesOfClass(LivingEntity.class, area));
                if (!list.isEmpty()) {
                    int amount = Mth.clamp(riftSize() % 11, 3, 10);
                    for (int i = 0; i < amount; i++) {
                        Collections.shuffle(list);
                        LivingEntity target = list.get(0);
                        EntityEldritchOrb orb = new EntityEldritchOrb(TTEntities.ELDRITCH_ORB.get(), server);
                        orb.snapTo(getX() + random.nextInt(6) - 3, getY() + random.nextInt(6) - 3, getZ() + random.nextInt(6) - 3,
                                (float) random.nextGaussian(), (float) random.nextGaussian());
                        orb.shoot(target.getX() - getX(), target.getY() - getY(), target.getZ() - getZ(), 0.66F, 0.1F);
                        server.addFreshEntity(orb);
                    }
                }
            }
            case 2 -> {
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, area)) {
                    zap(server, e);
                }
            }
            case 3 -> {
                for (Player p : server.getEntitiesOfClass(Player.class, area)) {
                    setRiftSize(riftSize() + Mth.clamp(WarpHelper.getActualWarp(p), 2, 10));
                    zap(server, p);
                }
            }
            case 4 -> spiders(server, area);
            case 5 -> wisps(server, area);
            case 6 -> {
                if (random.nextInt(100) > 60) {
                    setCollapsing(0);
                }
            }
            default -> {
            }
        }
    }

    private void zap(ServerLevel server, LivingEntity e) {
        Vec3 from = position().add(0.5, 0.5, 0.5);
        RiftUtil.zapBolt(server, from, e.position().add(0, e.getBbHeight() / 2.0, 0));
        e.hurtServer(server, damageSources().magic(), 4 + random.nextInt(Mth.clamp(riftSize(), 5, 30)));
    }

    private void spiders(ServerLevel server, AABB area) {
        int spawns = Math.min(50, riftSize());
        List<Player> players = server.getEntitiesOfClass(Player.class, area);
        BlockPos base = blockPosition();
        for (int a = 0; a < spawns; a++) {
            EntityMindSpider spider = TTEntities.MIND_SPIDER.get().create(server, EntitySpawnReason.MOB_SUMMONED);
            if (spider == null) {
                return;
            }
            boolean placed = false;
            for (int tries = 0; tries < 50 && !placed; tries++) {
                int x = base.getX() + Mth.randomBetweenInclusive(random, 7, 24) * Mth.randomBetweenInclusive(random, -1, 1);
                int y = base.getY() + Mth.randomBetweenInclusive(random, 7, 24) * Mth.randomBetweenInclusive(random, -1, 1);
                int z = base.getZ() + Mth.randomBetweenInclusive(random, 7, 24) * Mth.randomBetweenInclusive(random, -1, 1);
                BlockPos below = new BlockPos(x, y - 1, z);
                if (server.getBlockState(below).isFaceSturdy(server, below, Direction.UP)) {
                    spider.setPos(x + 0.5, y, z + 0.5);
                    placed = server.noCollision(spider) && !server.containsAnyLiquid(spider.getBoundingBox());
                }
            }
            if (!placed) {
                continue;
            }
            if (!players.isEmpty()) {
                Player target = players.get(random.nextInt(players.size()));
                spider.setTarget(target);
                spider.bindIllusion(target.getName().getString());
            }
            server.addFreshEntity(spider);
        }
    }

    /** TC4 event 5 built its charged wisps but never spawned them (fixed): a wisp of a random aspect at each target. */
    private void wisps(ServerLevel server, AABB area) {
        List<LivingEntity> list = new ArrayList<>(server.getEntitiesOfClass(LivingEntity.class, area));
        if (list.isEmpty()) {
            return;
        }
        List<ResourceKey<IAspect>> aspects = RiftUtil.allAspects(server);
        int amount = Mth.clamp(riftSize() % 11, 3, 10);
        for (int i = 0; i < amount; i++) {
            Collections.shuffle(list);
            LivingEntity e = list.get(0);
            Vec3 aim = new Vec3(e.getX() + e.getDeltaMovement().x - getX(), e.getY() + e.getBbHeight() / 2.0 - getY() - yOffset,
                    e.getZ() + e.getDeltaMovement().z - getZ());
            if (aim.lengthSqr() < 1.0E-4) {
                aim = new Vec3(0, 1, 0);
            }
            ResourceKey<IAspect> aspect = aspects.isEmpty() ? TTAspects.PERDITIO : aspects.get(random.nextInt(aspects.size()));
            ChargedWispEntity.spawn(server, position().add(0, yOffset, 0), aim.normalize(), aspect, null, 0.0F, 20 + random.nextInt(20),
                    ChargedWispEntity.Zap.DEFAULT);
        }
    }
}
