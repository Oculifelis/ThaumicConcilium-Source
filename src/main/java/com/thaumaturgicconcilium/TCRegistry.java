package com.thaumaturgicconcilium;

import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlock;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import com.thaumaturgicconcilium.entity.ShardPowderEntity;
import com.thaumaturgicconcilium.entity.ThrownConciliumBottle;
import com.thaumaturgicconcilium.item.ConciliumBottleItem;
import com.thaumaturgicconcilium.item.CrimsonAnnalesItem;
import com.thaumaturgicconcilium.item.DumpJackbootsItem;
import com.thaumaturgicconcilium.item.NetherMembraneRecipe;
import com.thaumaturgicconcilium.item.RunicWindingsItem;
import com.thaumaturgicconcilium.item.ShardMillItem;
import com.thaumaturgicconcilium.item.ThaumaturgeDrumItem;
import com.thaumaturgicconcilium.item.TrinketItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TCRegistry {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<com.thaumaturgicconcilium.item.MillCharge>> MILL_CHARGE =
            COMPONENTS.registerComponentType("mill_charge", b -> b.persistent(com.thaumaturgicconcilium.item.MillCharge.CODEC)
                    .networkSynchronized(com.thaumaturgicconcilium.item.MillCharge.STREAM_CODEC));

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    // ---- Sounds (the original's, CC0) ----
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_MILL = sound("mill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_CLACK = sound("clack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_SHARD_DROP = sound("shard_drop");

    static {
        for (String s : List.of("thump", "stab", "pull", "ira", "whispers", "hum", "bubbles", "gnaw", "melted", "sad", "uagh", "spell",
                "speech", "think", "growl", "chant", "gargle_many", "gargle_one", "gargle_pain", "shackles", "shackles_throw", "breath")) {
            sound(s);
        }
    }

    // ---- Materials ----
    public static final DeferredItem<Item> PRIMORDIAL_LIFE = item("primordial_life", p -> new Item(p.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> VOID_SLAG = item("void_slag", Item::new);
    public static final DeferredItem<Item> NETHER_MEMBRANE = item("nether_membrane", p -> new Item(p.fireResistant()));
    public static final DeferredItem<CrimsonAnnalesItem> CRIMSON_ANNALES = item("crimson_annales",
            p -> new CrimsonAnnalesItem(p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> POLISHMENT_DEVICE = item("polishment_device", p -> new Item(p.stacksTo(1).rarity(Rarity.RARE)));

    // ---- Throwables ----
    public static final DeferredItem<ConciliumBottleItem> BOTTLE_OF_CLEAR_WATER = item("bottle_of_clear_water",
            p -> new ConciliumBottleItem(p.stacksTo(16), ConciliumBottleItem.Kind.CLEAR_WATER));
    public static final DeferredItem<ConciliumBottleItem> BOTTLE_OF_THICK_TAINT = item("bottle_of_thick_taint",
            p -> new ConciliumBottleItem(p.stacksTo(16).rarity(Rarity.UNCOMMON), ConciliumBottleItem.Kind.THICK_TAINT));

    // ---- Trinkets (Curios) ----
    public static final DeferredItem<TrinketItem> RING_OF_BLUSTERING_LIGHT = item("ring_of_blustering_light",
            p -> new TrinketItem(p.stacksTo(1).rarity(Rarity.EPIC).component(TTDataComponents.RUNIC_CHARGE.get(), 50)));
    public static final DeferredItem<TrinketItem> BURDENING_AMULET = item("burdening_amulet",
            p -> new TrinketItem(p.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<TrinketItem> TIGHT_BELT = item("tight_belt", p -> new TrinketItem(p.stacksTo(1).rarity(Rarity.RARE)));

    // ---- Armour ----
    public static final DeferredItem<DumpJackbootsItem> DUMP_JACKBOOTS = item("dump_jackboots",
            p -> new DumpJackbootsItem(TCArmor.JACKBOOTS, ArmorItem.Type.BOOTS, p.durability(ArmorItem.Type.BOOTS.getDurability(TCArmor.JACKBOOTS_DURABILITY)).rarity(Rarity.RARE)));
    public static final DeferredItem<RunicWindingsItem> RUNIC_BODY_WINDINGS = item("runic_body_windings",
            p -> new RunicWindingsItem(TCArmor.WINDINGS, ArmorItem.Type.CHESTPLATE, p.durability(ArmorItem.Type.CHESTPLATE.getDurability(TCArmor.WINDINGS_DURABILITY)).rarity(Rarity.UNCOMMON)
                    .component(TTDataComponents.RUNIC_CHARGE.get(), 200)));
    public static final DeferredItem<RunicWindingsItem> RUNIC_LEGS_WINDINGS = item("runic_legs_windings",
            p -> new RunicWindingsItem(TCArmor.WINDINGS, ArmorItem.Type.LEGGINGS, p.durability(ArmorItem.Type.LEGGINGS.getDurability(TCArmor.WINDINGS_DURABILITY)).rarity(Rarity.UNCOMMON)
                    .component(TTDataComponents.RUNIC_CHARGE.get(), 200)));

    // ---- Tools ----
    public static final DeferredItem<ThaumaturgeDrumItem> THAUMATURGE_DRUM = item("thaumaturge_drum",
            p -> new ThaumaturgeDrumItem(p.stacksTo(1).rarity(Rarity.RARE), false));
    public static final DeferredItem<ShardMillItem> GREATWOOD_SHARD_MILL = item("greatwood_shard_mill",
            p -> new ShardMillItem(p.stacksTo(1).rarity(Rarity.UNCOMMON), false));
    public static final DeferredItem<ShardMillItem> SILVERWOOD_SHARD_MILL = item("silverwood_shard_mill",
            p -> new ShardMillItem(p.stacksTo(1).rarity(Rarity.RARE), true));

    // ---- Blocks ----
    public static final DeferredBlock<DestabilizedCrystalBlock> DESTABILIZED_CRYSTAL = block("destabilized_crystal", DestabilizedCrystalBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.7F).sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 8).noOcclusion()
                    .pushReaction(PushReaction.DESTROY), Rarity.UNCOMMON);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestabilizedCrystalBlockEntity>> DESTABILIZED_CRYSTAL_BE =
            BLOCK_ENTITIES.register("destabilized_crystal", () -> BlockEntityType.Builder.of(DestabilizedCrystalBlockEntity::new, DESTABILIZED_CRYSTAL.get()).build(null));

    // ---- Entities ----
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownConciliumBottle>> THROWN_BOTTLE = ENTITIES.register("thrown_bottle",
            () -> EntityType.Builder.<ThrownConciliumBottle>of(ThrownConciliumBottle::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build(key("thrown_bottle")));
    public static final DeferredHolder<EntityType<?>, EntityType<ShardPowderEntity>> SHARD_POWDER = ENTITIES.register("shard_powder",
            () -> EntityType.Builder.<ShardPowderEntity>of(ShardPowderEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(6).updateInterval(2).build(key("shard_powder")));

    // ---- Recipes ----
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<NetherMembraneRecipe>> NETHER_MEMBRANE_RECIPE =
            SERIALIZERS.register("nether_membrane", () -> NetherMembraneRecipe.SERIALIZER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumaturgicconcilium"))
            .icon(() -> new ItemStack(PRIMORDIAL_LIFE.get()))
            .displayItems((params, out) -> TAB_ITEMS.forEach(i -> out.accept(i.get())))
            .build());

    private TCRegistry() {
    }

    private static String key(String name) {
        return ThaumaturgicConcilium.id(name).toString();
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ThaumaturgicConcilium.id(name)));
    }

    /** Registers a block and its item, shown in the Thaumic Concilium tab. */
    public static <B extends Block> DeferredBlock<B> block(String name, Function<Block.Properties, B> factory,
                                                           UnaryOperator<Block.Properties> props, Rarity rarity) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, props.apply(Block.Properties.of()));
        TAB_ITEMS.add(ITEMS.registerSimpleBlockItem(block, new Item.Properties().rarity(rarity)));
        return block;
    }

    /** Registers an item that shows in the Thaumic Concilium tab. */
    public static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory) {
        DeferredItem<I> item = ITEMS.registerItem(name, factory);
        TAB_ITEMS.add(item);
        return item;
    }

    /** Lets other parts of the mod (spells, compat) put their items in the tab. */
    public static void addToTab(Supplier<? extends Item> item) {
        TAB_ITEMS.add(item);
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        SERIALIZERS.register(bus);
        TABS.register(bus);
        COMPONENTS.register(bus);
        TCDataMaps.register(bus);
    }
}
