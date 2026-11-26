package com.thaumaturgicconcilium.mob;

import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Phase 2 mobs (TC4 Concilium): the Thaumaturge and Mad Thaumaturge, Dissolved, Quicksilver Elemental (and the
 * Quicksilver Crucible it melts into), Vengeful Golem, Overanimated and its Alive Limbs, the Enslaved Taint Spider and
 * the Nether Explorer. Natural spawns are {@code neoforge:add_spawns} biome modifiers, drops are loot tables, scan
 * aspects are Thaumaturge's {@code entity_aspects} data map and the trade is a datapack registry
 * ({@link ThaumaturgeTrade}); all written by {@code tools/gen_mobs.py}.
 */
public final class TCMobs {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    // ---- mobs ----
    public static final DeferredHolder<EntityType<?>, EntityType<ThaumaturgeEntity>> THAUMATURGE = ENTITIES.register("thaumaturge",
            () -> EntityType.Builder.<ThaumaturgeEntity>of(ThaumaturgeEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .clientTrackingRange(8).updateInterval(3).build(key("thaumaturge")));
    public static final DeferredHolder<EntityType<?>, EntityType<MadThaumaturgeEntity>> MAD_THAUMATURGE = ENTITIES.register("mad_thaumaturge",
            () -> EntityType.Builder.<MadThaumaturgeEntity>of(MadThaumaturgeEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .clientTrackingRange(8).notInPeaceful().build(key("mad_thaumaturge")));
    public static final DeferredHolder<EntityType<?>, EntityType<DissolvedEntity>> DISSOLVED = ENTITIES.register("dissolved",
            () -> EntityType.Builder.<DissolvedEntity>of(DissolvedEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F).fireImmune()
                    .clientTrackingRange(8).notInPeaceful().build(key("dissolved")));
    public static final DeferredHolder<EntityType<?>, EntityType<QuicksilverElementalEntity>> QUICKSILVER_ELEMENTAL = ENTITIES.register(
            "quicksilver_elemental", () -> EntityType.Builder.<QuicksilverElementalEntity>of(QuicksilverElementalEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(8).notInPeaceful().build(key("quicksilver_elemental")));
    public static final DeferredHolder<EntityType<?>, EntityType<VengefulGolemEntity>> VENGEFUL_GOLEM = ENTITIES.register("vengeful_golem",
            () -> EntityType.Builder.<VengefulGolemEntity>of(VengefulGolemEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.6F)
                    .clientTrackingRange(8).notInPeaceful().build(key("vengeful_golem")));
    public static final DeferredHolder<EntityType<?>, EntityType<OveranimatedEntity>> OVERANIMATED = ENTITIES.register("overanimated",
            () -> EntityType.Builder.<OveranimatedEntity>of(OveranimatedEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .clientTrackingRange(8).notInPeaceful().build(key("overanimated")));
    public static final DeferredHolder<EntityType<?>, EntityType<AliveLimbEntity>> ALIVE_LIMB = ENTITIES.register("thaum_gib",
            () -> EntityType.Builder.<AliveLimbEntity>of(AliveLimbEntity::new, MobCategory.MONSTER).sized(0.6F, 0.5F).eyeHeight(0.35F)
                    .clientTrackingRange(8).notInPeaceful().build(key("thaum_gib")));
    public static final DeferredHolder<EntityType<?>, EntityType<TaintSpiderSmartEntity>> TAINT_SPIDER_SMART = ENTITIES.register("taint_spider_smart",
            () -> EntityType.Builder.<TaintSpiderSmartEntity>of(TaintSpiderSmartEntity::new, MobCategory.MONSTER).sized(0.4F, 0.3F).eyeHeight(0.13F)
                    .clientTrackingRange(8).build(key("taint_spider_smart")));
    public static final DeferredHolder<EntityType<?>, EntityType<NetherExplorerEntity>> NETHER_EXPLORER = ENTITIES.register("nether_explorer",
            () -> EntityType.Builder.<NetherExplorerEntity>of(NetherExplorerEntity::new, MobCategory.MISC).sized(2.0F, 0.3F).eyeHeight(0.2F).fireImmune()
                    .clientTrackingRange(8).build(key("nether_explorer")));

    // ---- helpers ----
    public static final DeferredHolder<EntityType<?>, EntityType<UpcomingHoleEntity>> UPCOMING_HOLE = ENTITIES.register("upcoming_hole",
            () -> EntityType.Builder.<UpcomingHoleEntity>of(UpcomingHoleEntity::new, MobCategory.MISC).sized(0.5F, 0.1F).noSave()
                    .clientTrackingRange(4).build(key("upcoming_hole")));
    public static final DeferredHolder<EntityType<?>, EntityType<EmberEntity>> EMBER = ENTITIES.register("thaumaturge_ember",
            () -> EntityType.Builder.<EmberEntity>of(EmberEntity::new, MobCategory.MISC).sized(0.3125F, 0.3125F).noSave()
                    .clientTrackingRange(4).updateInterval(2).build(key("thaumaturge_ember")));

    // ---- Quicksilver Crucible ----
    public static final DeferredBlock<QuicksilverCrucibleBlock> QUICKSILVER_CRUCIBLE = BLOCKS.registerBlock("quicksilver_crucible",
            QuicksilverCrucibleBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> QUICKSILVER_CRUCIBLE_ITEM = ITEMS.registerSimpleBlockItem(QUICKSILVER_CRUCIBLE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuicksilverCrucibleBlockEntity>> QUICKSILVER_CRUCIBLE_BE =
            BLOCK_ENTITIES.register("quicksilver_crucible", () -> new BlockEntityType<>(QuicksilverCrucibleBlockEntity::new, QUICKSILVER_CRUCIBLE.get()));

    // ---- trade menu ----
    public static final DeferredHolder<MenuType<?>, MenuType<ThaumaturgeMenu>> THAUMATURGE_MENU = MENUS.register("thaumaturge",
            () -> IMenuTypeExtension.create(ThaumaturgeMenu::new));

    // ---- spawn eggs ----
    public static final DeferredItem<SpawnEggItem> THAUMATURGE_EGG = egg("thaumaturge", THAUMATURGE);
    public static final DeferredItem<SpawnEggItem> MAD_THAUMATURGE_EGG = egg("mad_thaumaturge", MAD_THAUMATURGE);
    public static final DeferredItem<SpawnEggItem> DISSOLVED_EGG = egg("dissolved", DISSOLVED);
    public static final DeferredItem<SpawnEggItem> QUICKSILVER_ELEMENTAL_EGG = egg("quicksilver_elemental", QUICKSILVER_ELEMENTAL);
    public static final DeferredItem<SpawnEggItem> VENGEFUL_GOLEM_EGG = egg("vengeful_golem", VENGEFUL_GOLEM);
    public static final DeferredItem<SpawnEggItem> OVERANIMATED_EGG = egg("overanimated", OVERANIMATED);
    public static final DeferredItem<SpawnEggItem> ALIVE_LIMB_EGG = egg("thaum_gib", ALIVE_LIMB);
    public static final DeferredItem<SpawnEggItem> TAINT_SPIDER_SMART_EGG = egg("taint_spider_smart", TAINT_SPIDER_SMART);
    public static final DeferredItem<SpawnEggItem> NETHER_EXPLORER_EGG = egg("nether_explorer", NETHER_EXPLORER);

    // ---- Nether Explorer spawner (TC4 TCEntityEventHandler world tick, Nether only) ----
    private static final int EXPLORER_INTERVAL = 300;
    private static final int EXPLORER_MAX = 4;
    private static final int EXPLORER_CHUNK_RADIUS = 7;
    private static final double EXPLORER_PLAYER_CLEARANCE = 24.0;
    private static final double EXPLORER_SPAWN_CLEARANCE_SQR = 576.0;

    private TCMobs() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id(name));
    }

    private static <T extends Mob> DeferredItem<SpawnEggItem> egg(String name, Supplier<EntityType<T>> type) {
        DeferredItem<SpawnEggItem> item = ITEMS.registerItem(name + "_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(type.get()));
        TCRegistry.addToTab(item);
        return item;
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        TCRegistry.addToTab(QUICKSILVER_CRUCIBLE_ITEM);
        modBus.addListener(TCMobs::attributes);
        modBus.addListener(TCMobs::spawnPlacements);
        modBus.addListener(TCMobs::payloads);
        modBus.addListener(TCMobs::datapackRegistries);
        NeoForge.EVENT_BUS.addListener(TCMobs::netherExplorers);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(THAUMATURGE.get(), ThaumaturgeEntity.createAttributes().build());
        event.put(MAD_THAUMATURGE.get(), MadThaumaturgeEntity.createAttributes().build());
        event.put(DISSOLVED.get(), DissolvedEntity.createAttributes().build());
        event.put(QUICKSILVER_ELEMENTAL.get(), QuicksilverElementalEntity.createAttributes().build());
        event.put(VENGEFUL_GOLEM.get(), VengefulGolemEntity.createAttributes().build());
        event.put(OVERANIMATED.get(), OveranimatedEntity.createAttributes().build());
        event.put(ALIVE_LIMB.get(), AliveLimbEntity.createAttributes().build());
        event.put(TAINT_SPIDER_SMART.get(), TaintSpiderSmartEntity.createAttributes().build());
        event.put(NETHER_EXPLORER.get(), NetherExplorerEntity.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        var ground = SpawnPlacementTypes.ON_GROUND;
        var height = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        event.register(THAUMATURGE.get(), ground, height, Mob::checkMobSpawnRules, op);
        event.register(MAD_THAUMATURGE.get(), ground, height, Monster::checkMonsterSpawnRules, op);
        event.register(DISSOLVED.get(), ground, height, Monster::checkMonsterSpawnRules, op);
        event.register(QUICKSILVER_ELEMENTAL.get(), ground, height, Monster::checkMonsterSpawnRules, op);
        event.register(VENGEFUL_GOLEM.get(), ground, height, Monster::checkMonsterSpawnRules, op);
        event.register(OVERANIMATED.get(), ground, height, Monster::checkMonsterSpawnRules, op);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(EnslavePayload.TYPE, EnslavePayload.STREAM_CODEC);
    }

    private static void datapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ThaumaturgeTrade.REGISTRY_KEY, ThaumaturgeTrade.CODEC);
    }

    /**
     * Every 300 ticks in the Nether, with fewer than 4 Explorers about: a random column within 7 chunks of a random
     * player, then up to 4 tries within 6 blocks for a lava block with no player within 24 and at least 24 blocks from
     * the world spawn. One Explorer per run.
     */
    private static void netherExplorers(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.NETHER || level.getGameTime() % EXPLORER_INTERVAL != 0) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty() || level.getEntities(NETHER_EXPLORER.get(), e -> true).size() >= EXPLORER_MAX) {
            return;
        }
        var random = level.getRandom();
        ServerPlayer near = players.get(random.nextInt(players.size()));
        int cx = (near.blockPosition().getX() >> 4) + random.nextInt(EXPLORER_CHUNK_RADIUS * 2 + 1) - EXPLORER_CHUNK_RADIUS;
        int cz = (near.blockPosition().getZ() >> 4) + random.nextInt(EXPLORER_CHUNK_RADIUS * 2 + 1) - EXPLORER_CHUNK_RADIUS;
        int y = level.getMinY() + random.nextInt(Math.max(1, Math.min(level.getMaxY(), 128) - level.getMinY()));
        BlockPos base = new BlockPos((cx << 4) + random.nextInt(16), y, (cz << 4) + random.nextInt(16));
        BlockPos spawn = level.getRespawnData().pos();
        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos pos = base.offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6);
            if (!level.isLoaded(pos) || !level.getFluidState(pos).is(Fluids.LAVA) || pos.distSqr(spawn) < EXPLORER_SPAWN_CLEARANCE_SQR
                    || level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, EXPLORER_PLAYER_CLEARANCE, false) != null) {
                continue;
            }
            Entity made = NETHER_EXPLORER.get().create(level, EntitySpawnReason.EVENT);
            if (made == null) {
                return;
            }
            made.snapTo(pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(made);
            return;
        }
    }
}
