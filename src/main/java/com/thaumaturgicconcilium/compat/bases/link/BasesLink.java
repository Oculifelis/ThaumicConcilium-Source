package com.thaumaturgicconcilium.compat.bases.link;

import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Thaumic Bases link, with Thaumic Horizons present too (TC4 registered the Doped Squid only then): the Doped Squid,
 * which reads Bases' tobacco data. Reached only from {@code CompatB} behind both mod checks.
 */
public final class BasesLink {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThaumaturgicConcilium.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DopeSquidEntity>> DOPE_SQUID = ENTITIES.register("dope_squid",
            () -> EntityType.Builder.<DopeSquidEntity>of(DopeSquidEntity::new, MobCategory.MISC).sized(0.95F, 0.95F).eyeHeight(0.475F)
                    .clientTrackingRange(10).updateInterval(3)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id("dope_squid"))));
    public static final DeferredItem<SpawnEggItem> DOPE_SQUID_EGG = ITEMS.registerItem("dope_squid_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(DOPE_SQUID.get()));

    private BasesLink() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        TCRegistry.addToTab(DOPE_SQUID_EGG);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(DOPE_SQUID.get(), DopeSquidEntity.createAttributes().build()));
    }
}
