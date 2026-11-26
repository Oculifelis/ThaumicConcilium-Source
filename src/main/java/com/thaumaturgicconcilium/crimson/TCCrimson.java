package com.thaumaturgicconcilium.crimson;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.entity.boss.EntityCultistLeader;
import com.mojang.serialization.Codec;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The crimson half of Thaumic Concilium's mobs: the Paranoid Warrior (TC4 "Samurai", Tainted Magic in the original),
 * the Crimson Paladin, Archer and Ranger, the Paladin's Ethereal Shackles and Crimson Orb, and the "chained" state the
 * shackles put on a player. The Crimson Avanpost tower is pure data (jigsaw structure written by tools/gen_crimson.py);
 * its extra archers come from {@link CrimsonArcher}'s escort hook.
 */
public final class TCCrimson {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    // ---- mobs ----
    public static final DeferredHolder<EntityType<?>, EntityType<ParanoidWarrior>> PARANOID_WARRIOR = ENTITIES.register("paranoid_warrior",
            () -> EntityType.Builder.<ParanoidWarrior>of(ParanoidWarrior::new, MobCategory.MONSTER).sized(1.0F, 1.8F).eyeHeight(1.62F)
                    .clientTrackingRange(8).build(key("paranoid_warrior")));
    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonPaladin>> CRIMSON_PALADIN = ENTITIES.register("crimson_paladin",
            () -> EntityType.Builder.<CrimsonPaladin>of(CrimsonPaladin::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .clientTrackingRange(8).build(key("crimson_paladin")));
    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonArcher>> CRIMSON_ARCHER = ENTITIES.register("crimson_archer",
            () -> EntityType.Builder.<CrimsonArcher>of(CrimsonArcher::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .clientTrackingRange(8).build(key("crimson_archer")));
    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonRanger>> CRIMSON_RANGER = ENTITIES.register("crimson_ranger",
            () -> EntityType.Builder.<CrimsonRanger>of(CrimsonRanger::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .clientTrackingRange(8).build(key("crimson_ranger")));

    // ---- projectiles ----
    public static final DeferredHolder<EntityType<?>, EntityType<EtherealShacklesEntity>> ETHEREAL_SHACKLES = ENTITIES.register("ethereal_shackles",
            () -> EntityType.Builder.<EtherealShacklesEntity>of(EtherealShacklesEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build(key("ethereal_shackles")));
    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonOrbEntity>> CRIMSON_ORB = ENTITIES.register("crimson_orb",
            () -> EntityType.Builder.<CrimsonOrbEntity>of(CrimsonOrbEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(8).updateInterval(2).build(key("crimson_orb")));

    // ---- spawn eggs (stand-in vanilla egg models, see tools/gen_crimson.py) ----
    public static final DeferredItem<SpawnEggItem> PARANOID_WARRIOR_SPAWN_EGG = egg("paranoid_warrior_spawn_egg", PARANOID_WARRIOR);
    public static final DeferredItem<SpawnEggItem> CRIMSON_PALADIN_SPAWN_EGG = egg("crimson_paladin_spawn_egg", CRIMSON_PALADIN);
    public static final DeferredItem<SpawnEggItem> CRIMSON_ARCHER_SPAWN_EGG = egg("crimson_archer_spawn_egg", CRIMSON_ARCHER);
    public static final DeferredItem<SpawnEggItem> CRIMSON_RANGER_SPAWN_EGG = egg("crimson_ranger_spawn_egg", CRIMSON_RANGER);

    // ---- attachments ----
    /**
     * Game time until which a player is chained (the original's {@code chainedTime}, a 100-tick countdown). Stored as an
     * end time so it never needs ticking; synced so the client also blocks the hands and hides them.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> CHAINED_UNTIL = ATTACHMENTS.register("chained_until",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("until")).sync(ByteBufCodecs.VAR_LONG).build());

    private TCCrimson() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id(name));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, Supplier<? extends EntityType<?>> type) {
        DeferredItem<SpawnEggItem> item = ITEMS.registerItem(name, p -> new SpawnEggItem(p.spawnEgg(type.get())));
        TCRegistry.addToTab(item);
        return item;
    }

    /** One of the mod's sounds (registered in {@link TCRegistry}) by name. */
    static SoundEvent sound(String name) {
        Identifier id = ThaumaturgicConcilium.id(name);
        SoundEvent s = BuiltInRegistries.SOUND_EVENT.getValue(id);
        return s != null ? s : SoundEvent.createVariableRangeEvent(id);
    }

    /** The Thaumaturge trader mob (made by the other half of phase 2), matched by id so this package does not depend on it. */
    static boolean isThaumaturge(Entity e) {
        return EntityType.getKey(e.getType()).equals(ThaumaturgicConcilium.id("thaumaturge"));
    }

    /** Crimson cult members (Thaumaturge's cultists and Praetor, and the Concilium's own cultists and Pontifex). */
    static boolean isCultist(Entity e) {
        if (e instanceof EntityCultist || e instanceof EntityCultistLeader) {
            return true;
        }
        Identifier id = EntityType.getKey(e.getType());
        return id.getNamespace().equals(MODID) && id.getPath().equals("crimson_pontifex");
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(TCCrimson::attributes);
        modBus.addListener(TCCrimson::spawnPlacements);
        modBus.addListener((DataPackRegistryEvent.NewRegistry e) -> e.dataPackRegistry(ParanoidWarriorVariant.REGISTRY_KEY,
                ParanoidWarriorVariant.CODEC));
        Chains.register(NeoForge.EVENT_BUS);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(PARANOID_WARRIOR.get(), ParanoidWarrior.createAttributes().build());
        event.put(CRIMSON_PALADIN.get(), CrimsonPaladin.createAttributes().build());
        event.put(CRIMSON_ARCHER.get(), CrimsonArcher.createAttributes().build());
        event.put(CRIMSON_RANGER.get(), CrimsonRanger.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        place(event, PARANOID_WARRIOR.get());
        place(event, CRIMSON_PALADIN.get());
        place(event, CRIMSON_ARCHER.get());
        place(event, CRIMSON_RANGER.get());
    }

    private static <T extends Monster> void place(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
