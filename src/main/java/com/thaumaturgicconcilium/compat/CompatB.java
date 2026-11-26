package com.thaumaturgicconcilium.compat;

import com.thaumaturgicconcilium.compat.automagy.link.AutomagyLink;
import com.thaumaturgicconcilium.compat.golem.TCSeals;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Compat part B on 1.21.1: the Valet and Assistant seals and the Drainage Syringe (always), and the Crystalline Eye in the
 * Astral Monitor with {@code automagia}. Thaumic Horizons, Thaumic Bases and Gadomancy have no 1.21.1 build, so their
 * content is left out of this port.
 */
public final class CompatB {
    public static final String AUTOMAGY = "automagia";

    private CompatB() {
    }

    private static boolean loaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    public static void register(IEventBus modBus) {
        CompatBContent.register(modBus);
        TCSeals.register(modBus);
        if (loaded(AUTOMAGY)) {
            AutomagyLink.register();
        }
    }

    public static void initClient(IEventBus modBus) {
        com.thaumaturgicconcilium.compat.client.CompatBClient.init(modBus);
    }
}
