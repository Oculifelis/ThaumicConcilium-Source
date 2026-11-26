package com.thaumaturgicconcilium;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Armour materials (registry entries on 1.21.1). */
public final class TCArmor {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ThaumaturgicConcilium.MODID);

    /** Repairs the robe (data tag; written by gen_bosses as Thaumaturge's enchanted fabric). */
    public static final TagKey<Item> ROBE_REPAIR = TagKey.create(Registries.ITEM, ThaumaturgicConcilium.id("repairs_pontifex_robe"));

    /** Dump Jackboots: TC4's special armour material (iron-like). Durability multiplier 25. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JACKBOOTS = MATERIALS.register("dump_jackboots",
            () -> new ArmorMaterial(defense(2, 5, 6, 2), 25, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Tags.Items.LEATHERS),
                    List.of(new ArmorMaterial.Layer(ThaumaturgicConcilium.id("dump_jackboots"))), 0.0F, 0.0F));
    /** Runic Windings: durability 1, protection 1, enchantability 30; all the protection is runic. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WINDINGS = MATERIALS.register("runic_windings",
            () -> new ArmorMaterial(defense(1, 1, 1, 1), 30, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Tags.Items.STRINGS),
                    List.of(new ArmorMaterial.Layer(ThaumaturgicConcilium.id("runic_windings"))), 0.0F, 0.0F));
    /** TC4 PONTIFEXROBE: durability 30, protection {4, 8, 7, 4}, enchantability 25; dyeable, on Thaumaturge's robe textures. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PONTIFEX_ROBE = MATERIALS.register("pontifex_robe",
            () -> new ArmorMaterial(defense(4, 7, 8, 4), 25, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ROBE_REPAIR),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("thaumaturge", "robes"), "", true),
                            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("thaumaturge", "robes"), "_over", false)), 0.0F, 0.0F));

    public static final int JACKBOOTS_DURABILITY = 25;
    public static final int WINDINGS_DURABILITY = 1;
    public static final int ROBE_DURABILITY = 30;

    private TCArmor() {
    }

    private static Map<ArmorItem.Type, Integer> defense(int boots, int legs, int chest, int helmet) {
        Map<ArmorItem.Type, Integer> m = new EnumMap<>(ArmorItem.Type.class);
        m.put(ArmorItem.Type.BOOTS, boots);
        m.put(ArmorItem.Type.LEGGINGS, legs);
        m.put(ArmorItem.Type.CHESTPLATE, chest);
        m.put(ArmorItem.Type.HELMET, helmet);
        m.put(ArmorItem.Type.BODY, chest);
        return m;
    }

    public static void register(IEventBus modBus) {
        MATERIALS.register(modBus);
    }
}
