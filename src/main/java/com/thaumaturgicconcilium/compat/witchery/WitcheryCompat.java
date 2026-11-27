package com.thaumaturgicconcilium.compat.witchery;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Witchery compat (TC4 {@code WitcheryIntegration} and the {@code TCWITCHERY} mobs): the Chort, Sloppy Alchemist and
 * Burned Witch with the witch's brew data, the crimson symbology spells, and a built-in data pack that is only added
 * with Witchery (resourcepacks_data/witchery): the Pontifex's kill hint for Crimson Spells and the mobs' scan aspects.
 * Spawns, loot, research and the Burning Site are ordinary data with a {@code neoforge:mod_loaded} condition.
 */
public final class WitcheryCompat {
    private WitcheryCompat() {
    }

    public static void register(IEventBus modBus) {
        WitcheryMobs.register(modBus);
        WitcherySpells.register(modBus);
        modBus.addListener((DataPackRegistryEvent.NewRegistry e) -> e.dataPackRegistry(BurnedWitchBrew.REGISTRY_KEY, BurnedWitchBrew.CODEC));
        modBus.addListener(WitcheryCompat::packs);
    }

    private static void packs(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addPackFinders(ThaumaturgicConcilium.id("resourcepacks_data/witchery"), PackType.SERVER_DATA,
                    Component.translatable("pack.thaumaturgicconcilium.witchery"), PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }
}
