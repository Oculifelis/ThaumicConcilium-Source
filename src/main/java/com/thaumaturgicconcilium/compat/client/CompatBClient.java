package com.thaumaturgicconcilium.compat.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.CompatBContent;
import com.thaumaturgicconcilium.compat.horizons.DrainageSyringeItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client side of the compat part B base content: the Drainage Syringe's {@code thaumaturgicconcilium:syringe_filled} item
 * property (1 while it holds vis), which switches its model to the purple fill layer TC4's renderer drew.
 */
public final class CompatBClient {
    private CompatBClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> ItemProperties.register(
                CompatBContent.DRAINAGE_SYRINGE.get(), ThaumaturgicConcilium.id("syringe_filled"),
                (stack, level, owner, seed) -> DrainageSyringeItem.data(stack).amount() > 0 ? 1.0F : 0.0F)));
    }
}
