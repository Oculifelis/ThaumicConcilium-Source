package com.thaumaturgicconcilium.rift;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.item.ThaumaturgeDrumItem;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Phase 3, the rift set: the Flux Rift, the Hex of Predictability and Chained Rift recipes, the Vis Condenser, the
 * Lithographer and Astral Monitor (with Wand Xylography and Coagulating Eye Drops), the Gems of Rifts and Terra-Cast
 * Gems, the Causal Bouillon and its Void Reality, and the Warped Drum. Data and assets: {@code tools/gen_rifts.py}.
 */
public final class TCRifts {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    // ---- tags and data maps ----
    /** The block the hex is built around (TC4 config {@code hexCentralBlock}, default bedrock). */
    public static final TagKey<Block> HEX_CENTER = TagKey.create(Registries.BLOCK, ThaumaturgicConcilium.id("hex_center"));
    /** The ring of eight (TC4 cosmetic solid meta 11, Ancient Stone). */
    public static final TagKey<Block> HEX_RING = TagKey.create(Registries.BLOCK, ThaumaturgicConcilium.id("hex_ring"));
    /** Both looks of the formed hex (plain and thick-carved); the structure checks accept either. */
    public static final TagKey<Block> HEX_BLOCKS = TagKey.create(Registries.BLOCK, ThaumaturgicConcilium.id("hex"));

    /** Chance in percent that breaking this node block opens a rift (TC4 config {@code chanceOfRiftOpening}, 100). */
    public record RiftOpening(int chance) {
        public static final Codec<RiftOpening> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 100).fieldOf("chance").forGetter(RiftOpening::chance)
        ).apply(i, RiftOpening::new));
    }

    public static final DataMapType<Block, RiftOpening> RIFT_OPENING = DataMapType.builder(ThaumaturgicConcilium.id("rift_opening"), Registries.BLOCK,
            RiftOpening.CODEC).build();

    // ---- data components ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> XYLOGRAPHY = COMPONENTS.registerComponentType(
            "xylography", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RiftData.Readings>> ASTRAL_READINGS = COMPONENTS.registerComponentType(
            "astral_readings", b -> b.persistent(RiftData.Readings.CODEC).networkSynchronized(RiftData.Readings.STREAM_CODEC));
    /** A watched place (phase 4: Crystal Eye on a Red-Powered Mind); an item carrying it fits the Astral Monitor. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> ASTRAL_TARGET = COMPONENTS.registerComponentType(
            "astral_target", b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RiftData.Gem>> RIFT_GEM_DATA = COMPONENTS.registerComponentType(
            "rift_gem", b -> b.persistent(RiftData.Gem.CODEC).networkSynchronized(RiftData.Gem.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RiftData.Cast>> TERRA_CAST = COMPONENTS.registerComponentType(
            "terra_cast", b -> b.persistent(RiftData.Cast.CODEC).networkSynchronized(RiftData.Cast.STREAM_CODEC));

    // ---- attachments ----
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RiftData.Lithograph>> LITHOGRAPH = ATTACHMENTS.register("lithograph",
            () -> AttachmentType.builder(() -> RiftData.Lithograph.NONE).serialize(RiftData.Lithograph.CODEC.fieldOf("lithograph")).copyOnDeath()
                    .sync((holder, player) -> holder == player, RiftData.Lithograph.STREAM_CODEC).build());

    // ---- blocks ----
    public static final DeferredBlock<HexBlock> HEX = TCRegistry.block("hex_of_predictability", HexBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(0.7F, 100.0F).sound(SoundType.STONE).lightLevel(s -> 8).noLootTable()
                    .pushReaction(PushReaction.BLOCK), Rarity.RARE);
    /** The same hex, carved deep: a wand struck while sneaking forms this one. */
    public static final DeferredBlock<HexBlock> HEX_THICK = TCRegistry.block("hex_of_predictability_thick", HexBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(0.7F, 100.0F).sound(SoundType.STONE).lightLevel(s -> 8).noLootTable()
                    .pushReaction(PushReaction.BLOCK), Rarity.RARE);
    public static final DeferredBlock<VisCondenserBlock> VIS_CONDENSER = TCRegistry.block("vis_condenser", VisCondenserBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(0.7F).sound(SoundType.METAL).lightLevel(s -> 8).noOcclusion(), Rarity.UNCOMMON);
    public static final DeferredBlock<LithographerBlock> LITHOGRAPHER = TCRegistry.block("lithographer", LithographerBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.0F, 17.0F).sound(SoundType.METAL).noOcclusion(), Rarity.RARE);
    /** TC4 {@code VoidRealityBlock}: the floor of the Causal Bouillon; unbreakable. */
    public static final DeferredBlock<Block> VOID_REALITY = TCRegistry.block("void_reality", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 6000.0F).sound(SoundType.WOOL).noLootTable().isValidSpawn((s, l, pos, t) -> false),
            Rarity.EPIC);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexBlockEntity>> HEX_BE = BLOCK_ENTITIES.register("hex_of_predictability",
            () -> new BlockEntityType<>(HexBlockEntity::new, HEX.get(), HEX_THICK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VisCondenserBlockEntity>> VIS_CONDENSER_BE = BLOCK_ENTITIES.register(
            "vis_condenser", () -> new BlockEntityType<>(VisCondenserBlockEntity::new, VIS_CONDENSER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LithographerBlockEntity>> LITHOGRAPHER_BE = BLOCK_ENTITIES.register(
            "lithographer", () -> new BlockEntityType<>(LithographerBlockEntity::new, LITHOGRAPHER.get()));

    // ---- items ----
    public static final DeferredItem<Item> RIFT_GEM_INERT = TCRegistry.item("rift_gem_inert", p -> new Item(p.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<RiftGemItem> RIFT_GEM = TCRegistry.item("rift_gem", p -> new RiftGemItem(p.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TERRA_CAST_GEM_INERT = TCRegistry.item("terra_cast_gem_inert",
            p -> new Item(p.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<TerraCastGemItem> TERRA_CAST_GEM = TCRegistry.item("terra_cast_gem",
            p -> new TerraCastGemItem(p.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<AstralMonitorItem> ASTRAL_MONITOR = TCRegistry.item("astral_monitor",
            p -> new AstralMonitorItem(p.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<CoagulatingEyeDropsItem> COAGULATING_EYE_DROPS = TCRegistry.item("coagulating_eye_drops",
            p -> new CoagulatingEyeDropsItem(p.durability(100).rarity(Rarity.UNCOMMON)));
    /** TC4 drum meta 1: the Chained Rift turns a Thaumaturge's Drum into this. */
    public static final DeferredItem<ThaumaturgeDrumItem> WARPED_DRUM = TCRegistry.item("warped_thaumaturge_drum",
            p -> new ThaumaturgeDrumItem(p.stacksTo(1).rarity(Rarity.EPIC), true));

    // ---- entity ----
    public static final DeferredHolder<EntityType<?>, EntityType<RiftEntity>> FLUX_RIFT = ENTITIES.register("flux_rift",
            () -> EntityType.Builder.<RiftEntity>of(RiftEntity::new, MobCategory.MISC).sized(2.0F, 2.0F).fireImmune()
                    .clientTrackingRange(10).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id("flux_rift"))));

    // ---- menus ----
    public static final DeferredHolder<MenuType<?>, MenuType<LithographerMenu>> LITHOGRAPHER_MENU = MENUS.register("lithographer",
            () -> IMenuTypeExtension.create(LithographerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AstralMonitorMenu>> ASTRAL_MONITOR_MENU = MENUS.register("astral_monitor",
            () -> IMenuTypeExtension.create(AstralMonitorMenu::new));

    // ---- recipes ----
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TerraGemRecipe>> TERRA_GEM_RECIPE = SERIALIZERS.register("terra_gem",
            () -> TerraGemRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<XylographyRecipe>> XYLOGRAPHY_RECIPE = SERIALIZERS.register(
            "xylography", () -> XylographyRecipe.SERIALIZER);

    private TCRifts() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        MENUS.register(modBus);
        SERIALIZERS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(RIFT_OPENING));
        modBus.addListener((DataPackRegistryEvent.NewRegistry e) -> e.dataPackRegistry(ChainedRiftRecipe.REGISTRY_KEY, ChainedRiftRecipe.CODEC));
        modBus.addListener(TCRifts::payloads);
        RiftEvents.register(NeoForge.EVENT_BUS);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(PartyStatusPayload.TYPE, PartyStatusPayload.STREAM_CODEC);
    }
}
