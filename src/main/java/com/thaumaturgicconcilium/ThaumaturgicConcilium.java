package com.thaumaturgicconcilium;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Thaumic Concilium (Thaumcraft 4 addon for 1.7.10, by Ilya3000 and BreadX22), ported as a Thaumaturge addon for NeoForge 1.21.1. */
@Mod(ThaumaturgicConcilium.MODID)
public final class ThaumaturgicConcilium {
    public static final String MODID = "thaumaturgicconcilium";
    public static final Logger LOG = LogUtils.getLogger();

    public ThaumaturgicConcilium(IEventBus modBus, Dist dist) {
        TCArmor.register(modBus);
        TCRegistry.register(modBus);
        com.thaumaturgicconcilium.spell.TCSpells.register(modBus);
        com.thaumaturgicconcilium.mob.TCMobs.register(modBus);
        com.thaumaturgicconcilium.crimson.TCCrimson.register(modBus);
        com.thaumaturgicconcilium.rift.TCRifts.register(modBus);
        com.thaumaturgicconcilium.boss.TCBosses.register(modBus);
        com.thaumaturgicconcilium.compat.CompatA.register(modBus);
        com.thaumaturgicconcilium.compat.CompatB.register(modBus);
        TCEvents.register();
        SelfTest.register();
        if (dist == Dist.CLIENT) {
            com.thaumaturgicconcilium.client.TCClient.init(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

}
