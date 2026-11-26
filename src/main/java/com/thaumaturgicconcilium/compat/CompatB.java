package com.thaumaturgicconcilium.compat;

import com.thaumaturgicconcilium.compat.automagy.link.AutomagyLink;
import com.thaumaturgicconcilium.compat.bases.BasesContent;
import com.thaumaturgicconcilium.compat.bases.link.BasesLink;
import com.thaumaturgicconcilium.compat.bases.link.client.BasesLinkClient;
import com.thaumaturgicconcilium.compat.gadomancy.SinAuras;
import com.thaumaturgicconcilium.compat.gadomancy.link.GadomancyLink;
import com.thaumaturgicconcilium.compat.golem.TCSeals;
import com.thaumaturgicconcilium.compat.horizons.HorizonsContent;
import com.thaumaturgicconcilium.compat.horizons.client.HorizonsClient;
import com.thaumaturgicconcilium.compat.horizons.link.HorizonsLink;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Phase 4 compat, part B: Gadomancy, Thaumic Horizons, Thaumic Bases, Automagy, and the TC4 golem cores as Thaumaturge
 * seals. This class names no partner type: every partner-touching class sits in a {@code link} sub-package and is only
 * reached behind a {@link ModList} check made here, at mod construction.
 * <ul>
 * <li>Base content (always): the Valet and Assistant seals, the Drainage Syringe.</li>
 * <li>{@code thaumichorizons}: the Golem Bydlo and the creature infusions (Brain Littering, Golem Bydlo, Red-Powered Mind,
 * Doped Squid) and the Assistant's jar capture.</li>
 * <li>{@code thaumichorizons} + {@code automagy}: the Red-Powered Mind (creature, block); {@code automagy}: the Crystalline Eye
 * fits the Astral Monitor.</li>
 * <li>{@code thaumicbases}: the Casting Bracelets; with {@code thaumichorizons} too, the Doped Squid.</li>
 * <li>{@code gadomancy}: the sin-aspect aura effects of Aura Pylons.</li>
 * </ul>
 */
public final class CompatB {
    public static final String HORIZONS = "thaumichorizons";
    public static final String BASES = "thaumaturgicbases";
    public static final String GADOMANCY = "gadomancyawakened";
    public static final String AUTOMAGY = "automagia";
    public static final String TAINTED_MAGIC = "taintedmagic";

    private CompatB() {
    }

    private static boolean loaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    public static void register(IEventBus modBus) {
        boolean horizons = loaded(HORIZONS);
        boolean automagy = loaded(AUTOMAGY);
        boolean bases = loaded(BASES);
        CompatBContent.register(modBus);
        TCSeals.register(modBus);
        if (horizons) {
            HorizonsContent.register(modBus, automagy);
            HorizonsLink.register(modBus, automagy, bases);
        }
        if (bases) {
            BasesContent.register(modBus, loaded(TAINTED_MAGIC));
            if (horizons) {
                BasesLink.register(modBus);
            }
        }
        if (loaded(GADOMANCY)) {
            SinAuras.register(modBus);
            GadomancyLink.register();
        }
        if (automagy) {
            AutomagyLink.register();
        }
    }

    public static void initClient(IEventBus modBus) {
        com.thaumaturgicconcilium.compat.client.CompatBClient.init(modBus);
        if (loaded(HORIZONS)) {
            HorizonsClient.init(modBus, loaded(AUTOMAGY));
            if (loaded(BASES)) {
                BasesLinkClient.init(modBus);
            }
        }
    }
}
