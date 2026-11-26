package com.thaumaturgicconcilium.compat;

import com.thaumaturgicconcilium.compat.arsmagica.ArsMagicaCompat;
import com.thaumaturgicconcilium.compat.neovitae.NeoVitaeCompat;
import com.thaumaturgicconcilium.compat.witchery.WitcheryCompat;
import com.thaumaturgicconcilium.compat.witchery.client.WitcheryClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Phase 4 compat, part A: Witchery, Ars Magica Legacy and Neo Vitae. This class names no partner type: each partner's
 * classes sit in their own sub-package and are only reached behind a {@link ModList} check made here, at mod
 * construction.
 * <ul>
 * <li>{@code witchery}: the Chort (and its spit), the Sloppy Alchemist, the Burned Witch, the five crimson symbology
 * spells (learned with the Crimson Spells research), the Pontifex's Crimson Spells hint, and the Burning Site structure
 * (pure data). With {@code neovitae} too the Sloppy Alchemist spawns in the Nether (data condition).</li>
 * <li>{@code arsmagicalegacy}: the Crimson Raid spell component (learned with the Crimson Raid research) and the mana
 * glyph of the Astral Monitor and Lithographer.</li>
 * <li>{@code neovitae}: the blood glyph of the Astral Monitor and Lithographer.</li>
 * </ul>
 */
public final class CompatA {
    public static final String WITCHERY = "witchery";
    public static final String ARS_MAGICA = "arsmagicalegacy";
    public static final String NEO_VITAE = "neovitae";

    private CompatA() {
    }

    private static boolean loaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    public static void register(IEventBus modBus) {
        if (loaded(WITCHERY)) {
            WitcheryCompat.register(modBus);
        }
        if (loaded(ARS_MAGICA)) {
            ArsMagicaCompat.register(modBus);
        }
        if (loaded(NEO_VITAE)) {
            NeoVitaeCompat.register();
        }
    }

    public static void initClient(IEventBus modBus) {
        if (loaded(WITCHERY)) {
            WitcheryClient.init(modBus);
        }
    }
}
