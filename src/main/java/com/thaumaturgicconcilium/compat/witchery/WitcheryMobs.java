package com.thaumaturgicconcilium.compat.witchery;

import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Witchery mobs of Thaumic Concilium (TC4 {@code TCWITCHERY}): the Chort and its spit, the Sloppy Alchemist it
 * rides, and the Burned Witch. Registered only when Witchery is loaded (decided at mod construction).
 */
public final class WitcheryMobs {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ChortEntity>> CHORT = ENTITIES.register("chort",
            () -> EntityType.Builder.<ChortEntity>of(ChortEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).eyeHeight(1.62F).ridingOffset(-0.7F).fireImmune()
                    .clientTrackingRange(8).build(key("chort")));
    public static final DeferredHolder<EntityType<?>, EntityType<SloppyAlchemistEntity>> SLOPPY_ALCHEMIST = ENTITIES.register("sloppy_alchemist",
            () -> EntityType.Builder.<SloppyAlchemistEntity>of(SloppyAlchemistEntity::new, MobCategory.MONSTER).sized(0.5F, 1.8F).eyeHeight(1.62F)
                    .fireImmune().clientTrackingRange(8).build(key("sloppy_alchemist")));
    public static final DeferredHolder<EntityType<?>, EntityType<BurnedWitchEntity>> BURNED_WITCH = ENTITIES.register("burned_witch",
            () -> EntityType.Builder.<BurnedWitchEntity>of(BurnedWitchEntity::new, MobCategory.MONSTER).sized(1.0F, 2.0F).eyeHeight(1.62F)
                    .fireImmune().clientTrackingRange(10).build(key("burned_witch")));
    public static final DeferredHolder<EntityType<?>, EntityType<ChortSpitEntity>> CHORT_SPIT = ENTITIES.register("chort_spit",
            () -> EntityType.Builder.<ChortSpitEntity>of(ChortSpitEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build(key("chort_spit")));

    // Spawn eggs (stand-in vanilla egg models, see tools/gen_compat_a.py)
    public static final DeferredItem<SpawnEggItem> CHORT_SPAWN_EGG = egg("chort_spawn_egg", CHORT);
    public static final DeferredItem<SpawnEggItem> SLOPPY_ALCHEMIST_SPAWN_EGG = egg("sloppy_alchemist_spawn_egg", SLOPPY_ALCHEMIST);
    public static final DeferredItem<SpawnEggItem> BURNED_WITCH_SPAWN_EGG = egg("burned_witch_spawn_egg", BURNED_WITCH);

    private WitcheryMobs() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id(name));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, Supplier<? extends EntityType<?>> type) {
        DeferredItem<SpawnEggItem> item = ITEMS.registerItem(name, p -> new SpawnEggItem(p.spawnEgg(type.get())));
        TCRegistry.addToTab(item);
        return item;
    }

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(WitcheryMobs::attributes);
        modBus.addListener(WitcheryMobs::spawnPlacements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CHORT.get(), ChortEntity.createAttributes().build());
        event.put(SLOPPY_ALCHEMIST.get(), SloppyAlchemistEntity.createAttributes().build());
        event.put(BURNED_WITCH.get(), BurnedWitchEntity.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SLOPPY_ALCHEMIST.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CHORT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // The witch flies: any free spot (TC4 EntityFlying, like the Ghast), never in Peaceful.
        event.register(BURNED_WITCH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BurnedWitchEntity::checkWitchSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
