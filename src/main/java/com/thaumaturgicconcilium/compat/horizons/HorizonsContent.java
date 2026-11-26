package com.thaumaturgicconcilium.compat.horizons;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.util.DeferredSoundType;

/**
 * Thaumic Horizons content that names no Horizons type (TC4 registered it only with Horizons present, since it is only
 * made by creature infusion): the Golem Bydlo, and with Automagy too the Red-Powered Mind (creature, block, block entity).
 * Registered from {@code CompatB} only when {@code thaumichorizons} is loaded.
 */
public final class HorizonsContent {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static DeferredHolder<EntityType<?>, EntityType<GolemBydloEntity>> GOLEM_BYDLO;
    public static DeferredHolder<EntityType<?>, EntityType<RedPoweredMindEntity>> RED_POWERED_MIND;
    public static DeferredBlock<RedPoweredMindBlock> RED_POWERED_MIND_BLOCK;
    public static DeferredItem<BlockItem> RED_POWERED_MIND_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<RedPoweredMindBlockEntity>> RED_POWERED_MIND_BE;

    private static boolean mind;

    private HorizonsContent() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id(name));
    }

    private static <T extends Mob> void egg(String name, Supplier<EntityType<T>> type) {
        DeferredItem<SpawnEggItem> item = ITEMS.registerItem(name + "_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(type.get()));
        TCRegistry.addToTab(item);
    }

    public static boolean hasMind() {
        return mind;
    }

    public static void register(IEventBus modBus, boolean automagy) {
        GOLEM_BYDLO = ENTITIES.register("golem_bydlo", () -> EntityType.Builder.<GolemBydloEntity>of(GolemBydloEntity::new, MobCategory.MISC)
                .sized(2.0F, 3.5F).eyeHeight(3.0F).clientTrackingRange(10).updateInterval(3).build(key("golem_bydlo")));
        egg("golem_bydlo", () -> GOLEM_BYDLO.get());
        if (automagy) {
            mind = true;
            RED_POWERED_MIND = ENTITIES.register("red_powered_mind", () -> EntityType.Builder.<RedPoweredMindEntity>of(RedPoweredMindEntity::new,
                    MobCategory.MISC).sized(1.0F, 1.0F).eyeHeight(0.5F).clientTrackingRange(8).build(key("red_powered_mind")));
            egg("red_powered_mind", () -> RED_POWERED_MIND.get());
            // TC4: Material.cake, hardness 0.7, resistance 1, light 0.5 (level 8), the "gore" step sound.
            RED_POWERED_MIND_BLOCK = BLOCKS.registerBlock("red_powered_mind", RedPoweredMindBlock::new,
                    p -> p.mapColor(MapColor.COLOR_RED).strength(0.7F, 1.0F).lightLevel(s -> 8).noOcclusion()
                            .sound(new DeferredSoundType(0.5F, 0.8F, TTSounds.GORE, TTSounds.GORE, TTSounds.GORE, TTSounds.GORE, TTSounds.GORE))
                            .pushReaction(PushReaction.DESTROY));
            RED_POWERED_MIND_ITEM = ITEMS.registerSimpleBlockItem(RED_POWERED_MIND_BLOCK, p -> p.rarity(Rarity.UNCOMMON));
            TCRegistry.addToTab(RED_POWERED_MIND_ITEM);
            RED_POWERED_MIND_BE = BLOCK_ENTITIES.register("red_powered_mind",
                    () -> new BlockEntityType<>(RedPoweredMindBlockEntity::new, RED_POWERED_MIND_BLOCK.get()));
        }
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(HorizonsContent::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GOLEM_BYDLO.get(), GolemBydloEntity.createAttributes().build());
        if (mind) {
            event.put(RED_POWERED_MIND.get(), RedPoweredMindEntity.createAttributes().build());
        }
    }
}
