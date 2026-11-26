package com.thaumaturgicconcilium.compat.golem;

import com.leclowndu93150.thaumaturge.api.golems.seals.SealFilterMode;
import com.leclowndu93150.thaumaturge.api.golems.seals.SealType;
import com.leclowndu93150.thaumaturge.content.golem.ItemSealPlacer;
import com.leclowndu93150.thaumaturge.content.golem.seals.SealPlacements;
import com.leclowndu93150.thaumaturge.content.golem.seals.behavior.UseBehavior;
import com.leclowndu93150.thaumaturge.registry.TTGolemTraits;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The TC4 Concilium golem cores as Thaumaturge seals (base content: Thaumaturge is required). Valet answers its owner's
 * spoken password with a click; Assistant hunts the creatures of its area and jars them with a killing blow. TC4's third
 * core, Burlak, was never registered and is not ported.
 */
public final class TCSeals {
    public static final DeferredRegister<SealType> SEALS = DeferredRegister.create(SealType.REGISTRY_KEY, ThaumaturgicConcilium.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThaumaturgicConcilium.MODID);

    public static final DeferredItem<ItemSealPlacer> VALET_SEAL = ITEMS.registerItem("valet_seal",
            p -> new ItemSealPlacer(ThaumaturgicConcilium.id("valet"), p.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ItemSealPlacer> ASSISTANT_SEAL = ITEMS.registerItem("assistant_seal",
            p -> new ItemSealPlacer(ThaumaturgicConcilium.id("assistant"), p.rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SealType, SealType> VALET = SEALS.register("valet", () -> SealType.builder(ValetBehavior::new)
            .filter(1, SealFilterMode.PLAIN)
            .settings(UseBehavior.LEFT_CLICK, UseBehavior.INTO_AIR, UseBehavior.BARE_HANDED, UseBehavior.SNEAKING).showSettings()
            .requires(TTGolemTraits.DEFT, TTGolemTraits.SMART).placement(SealPlacements.ANYWHERE).placer(VALET_SEAL)
            .icon(ThaumaturgicConcilium.id("item/valet_seal")).build());
    public static final DeferredHolder<SealType, SealType> ASSISTANT = SEALS.register("assistant", () -> SealType.builder(AssistantBehavior::new)
            .area().requires(TTGolemTraits.FIGHTER, TTGolemTraits.SMART).placer(ASSISTANT_SEAL)
            .icon(ThaumaturgicConcilium.id("item/assistant_seal")).build());

    private TCSeals() {
    }

    public static void register(IEventBus modBus) {
        SEALS.register(modBus);
        ITEMS.register(modBus);
        TCRegistry.addToTab(VALET_SEAL);
        TCRegistry.addToTab(ASSISTANT_SEAL);
        NeoForge.EVENT_BUS.addListener(GolemChat::onChat);
        NeoForge.EVENT_BUS.addListener(GolemChat::onLogout);
        NeoForge.EVENT_BUS.addListener(AssistantCapture::onIncomingDamage);
    }
}
