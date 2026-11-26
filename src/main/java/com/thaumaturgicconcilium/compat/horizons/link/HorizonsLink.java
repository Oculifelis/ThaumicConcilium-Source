package com.thaumaturgicconcilium.compat.horizons.link;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.thaumichorizons.infusion.CreatureInfusions;
import com.thaumichorizons.infusion.CreatureInfusions.Kind;
import com.thaumichorizons.infusion.CreatureInfusions.Recipe;
import com.thaumichorizons.item.StasisJarItem;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.golem.AssistantCapture;
import com.thaumaturgicconcilium.compat.horizons.HorizonsContent;
import com.thaumaturgicconcilium.mob.TCMobs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Thaumic Horizons link (TC4 Concilium {@code HorizonsIntegration}): the four creature infusions of the vat, added to
 * Horizons' list, and the Stasis Jar capture the Assistant seal uses. Essentia amounts are TC4's divided by 4 (the scale
 * every Concilium recipe uses); research keys resolve in Horizons' namespace, so their entries are created under
 * {@code data/thaumichorizons/thaumaturge/research_entry}. TC4 also put the Nether Explorer, Sloppy Alchemist, Withered
 * Botanist and Protolimb on Horizons' class ban list; the Horizons port has a fixed ban rule and no list to add to.
 */
public final class HorizonsLink {
    private static final int INSTABILITY = 4;

    private HorizonsLink() {
    }

    public static void register(IEventBus modBus, boolean automagy, boolean bases) {
        AssistantCapture.setCapturer(StasisJarItem::capture);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> addRecipes(automagy, bases)));
    }

    private static void addRecipes(boolean automagy, boolean bases) {
        List<Recipe> all = CreatureInfusions.ALL;
        all.add(species("brain_littering", 941, "brain_littering", e -> e.getType() == TCMobs.THAUMATURGE.get(),
                List.of("thaumaturge:curio_knowledge", "forsakenmagic:taint_shard", "thaumaturge:brain", "forsakenmagic:taint_shard"),
                a("perditio", 16, "alienis", 16, "cognitio", 16), () -> TCMobs.MAD_THAUMATURGE.get()));
        all.add(species("golem_bydlo", 942, "golem_bydlo", HorizonsLink::mad,
                List.of("thaumaturge:golem", "thaumichorizons:golem_powder", "thaumaturge:metal_thaumium", "thaumaturge:tallow_block",
                        "thaumaturge:metal_thaumium", "thaumaturge:golem", "thaumaturge:golem_fetter", "thaumaturge:golem",
                        "thaumaturge:metal_thaumium", "thaumaturge:tallow_block", "thaumaturge:metal_thaumium", "thaumichorizons:golem_powder"),
                a("potentia", 16, "metallum", 32, "forsakenmagic:superbia", 8, "perditio", 32), () -> HorizonsContent.GOLEM_BYDLO.get()));
        if (automagy && HorizonsContent.hasMind()) {
            all.add(species("red_powered_mind", 943, "red_powered_mind", HorizonsLink::mad,
                    List.of("thaumaturge:liquid_death_bucket", "minecraft:repeater", "minecraft:redstone_block", "minecraft:redstone_block",
                            "minecraft:redstone_block", "thaumaturge:liquid_death_bucket", "thaumaturge:void_seed",
                            "thaumaturge:liquid_death_bucket", "minecraft:redstone_block", "minecraft:redstone_block",
                            "minecraft:redstone_block", "minecraft:comparator"),
                    a("vitreus", 16, "cognitio", 16, "sensus", 8, "machina", 8), () -> HorizonsContent.RED_POWERED_MIND.get()));
        }
        if (bases) {
            // Looked up by id: the squid is registered by the Thaumic Bases link, which this class does not name.
            Identifier squid = ThaumaturgicConcilium.id("dope_squid");
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(squid)) {
                all.add(species("dope_squid", 944, "dope_squid", e -> e.getType() == EntityType.SQUID,
                        List.of("thaumaturge:essentia_crystal", "thaumaturgicbases:tobacco_eldritch", "thaumaturgicbases:tobacco_fighting",
                                "thaumaturgicbases:tobacco_hunger", "thaumaturgicbases:tobacco_knowledge", "thaumaturge:essentia_crystal",
                                "thaumaturgicbases:tobacco_mining", "thaumaturgicbases:tobacco_sanity", "thaumaturgicbases:tobacco_tainted",
                                "thaumaturgicbases:tobacco_wispy"),
                        a("herba", 16, "praecantatio", 16, "humanus", 8, "volatus", 16),
                        () -> BuiltInRegistries.ENTITY_TYPE.getValue(squid)));
            }
        }
    }

    private static boolean mad(LivingEntity e) {
        return e.getType() == TCMobs.MAD_THAUMATURGE.get();
    }

    private static Recipe species(String key, int id, String research, Predicate<LivingEntity> subject, List<String> components,
            Map<ResourceKey<IAspect>, Integer> aspects, Supplier<? extends EntityType<?>> result) {
        return new Recipe(key, Kind.SPECIES, id, research, subject, Collections.unmodifiableList(new ArrayList<>(components)),
                aspects, INSTABILITY, result);
    }

    private static Map<ResourceKey<IAspect>, Integer> a(Object... pairs) {
        Map<ResourceKey<IAspect>, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            String name = (String) pairs[i];
            Identifier id = name.contains(":") ? Identifier.parse(name) : Identifier.fromNamespaceAndPath("thaumaturge", name);
            out.put(ResourceKey.create(IAspect.REGISTRY_KEY, id), (Integer) pairs[i + 1]);
        }
        return out;
    }
}
