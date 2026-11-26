package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.mojang.serialization.Codec;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

/**
 * Phase 3 bosses and the "ethereal" line (TC4 Concilium): the Brightest One, the Lesser Crimson Portal, the Crimson
 * Pontifex, the Peeler of Matter, the Pontifex's robe and hammer, the Crucible of Flesh, the Protobody and its six
 * Protolimbs, and Solid Void. Loot, research, recipes, tags and lang are data from {@code tools/gen_bosses.py}.
 *
 * <p>The rift package makes the three rift-summoned creatures by entity id and marks a boss portal with the entity tag
 * {@link LesserPortalEntity#RIFT_BOSS_TAG}; {@link #spawnFromRift(ServerLevel, Vec3, int)} (rift state 1, 2, 3 as in the
 * original) does the same for anyone who prefers a call.
 */
public final class TCBosses {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    // ---- creatures ----
    public static final DeferredHolder<EntityType<?>, EntityType<BrightestOneEntity>> BRIGHTEST_ONE = ENTITIES.register("brightest_one",
            () -> EntityType.Builder.<BrightestOneEntity>of(BrightestOneEntity::new, MobCategory.MONSTER).sized(3.0F, 3.0F).eyeHeight(1.5F).fireImmune()
                    .clientTrackingRange(10).build(key("brightest_one")));
    public static final DeferredHolder<EntityType<?>, EntityType<LesserPortalEntity>> LESSER_PORTAL = ENTITIES.register("lesser_crimson_portal",
            () -> EntityType.Builder.<LesserPortalEntity>of(LesserPortalEntity::new, MobCategory.MONSTER).sized(1.5F, 3.0F).eyeHeight(1.5F).fireImmune()
                    .clientTrackingRange(10).build(key("lesser_crimson_portal")));
    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonPontifexEntity>> CRIMSON_PONTIFEX = ENTITIES.register("crimson_pontifex",
            () -> EntityType.Builder.<CrimsonPontifexEntity>of(CrimsonPontifexEntity::new, MobCategory.MONSTER).sized(0.75F, 2.25F).eyeHeight(2.0F)
                    .clientTrackingRange(10).build(key("crimson_pontifex")));
    public static final DeferredHolder<EntityType<?>, EntityType<MaterialPeelerEntity>> MATERIAL_PEELER = ENTITIES.register("material_peeler",
            () -> EntityType.Builder.<MaterialPeelerEntity>of(MaterialPeelerEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).fireImmune()
                    .clientTrackingRange(10).updateInterval(20).build(key("material_peeler")));
    public static final DeferredHolder<EntityType<?>, EntityType<ProtolimbEntity>> PROTOLIMB = ENTITIES.register("protolimb",
            () -> EntityType.Builder.<ProtolimbEntity>of(ProtolimbEntity::new, MobCategory.MISC).sized(0.6F, 0.5F).eyeHeight(0.35F).fireImmune()
                    .clientTrackingRange(8).build(key("protolimb")));

    // ---- helpers ----
    public static final DeferredHolder<EntityType<?>, EntityType<BrightEmberEntity>> BRIGHT_EMBER = ENTITIES.register("bright_ember",
            () -> EntityType.Builder.<BrightEmberEntity>of(BrightEmberEntity::new, MobCategory.MISC).sized(0.3125F, 0.3125F).noSave()
                    .clientTrackingRange(4).updateInterval(2).build(key("bright_ember")));
    public static final DeferredHolder<EntityType<?>, EntityType<ProtobodyItemEntity>> PROTOBODY_ITEM = ENTITIES.register("protobody_item",
            () -> EntityType.Builder.<ProtobodyItemEntity>of(ProtobodyItemEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).eyeHeight(0.2125F)
                    .clientTrackingRange(6).updateInterval(20).build(key("protobody_item")));

    // ---- blocks ----
    public static final DeferredBlock<FleshCrucibleBlock> FLESH_CRUCIBLE = BLOCKS.registerBlock("flesh_crucible", FleshCrucibleBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.0F, 17.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> FLESH_CRUCIBLE_ITEM = ITEMS.registerSimpleBlockItem(FLESH_CRUCIBLE, p -> p.rarity(Rarity.RARE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FleshCrucibleBlockEntity>> FLESH_CRUCIBLE_BE =
            BLOCK_ENTITIES.register("flesh_crucible", () -> new BlockEntityType<>(FleshCrucibleBlockEntity::new, FLESH_CRUCIBLE.get()));

    public static final DeferredBlock<SolidVoidBlock> SOLID_VOID = BLOCKS.registerBlock("solid_void", SolidVoidBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(0.0F, 6000.0F).sound(SoundType.STONE).noLootTable().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> SOLID_VOID_ITEM = ITEMS.registerSimpleBlockItem(SOLID_VOID, p -> p.rarity(Rarity.EPIC));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidVoidBlockEntity>> SOLID_VOID_BE =
            BLOCK_ENTITIES.register("solid_void", () -> new BlockEntityType<>(SolidVoidBlockEntity::new, SOLID_VOID.get()));

    // ---- Pontifex gear ----
    /** Repairs the robe (data tag; written by gen_bosses as Thaumaturge's enchanted fabric). */
    public static final TagKey<Item> ROBE_REPAIR = TagKey.create(Registries.ITEM, ThaumaturgicConcilium.id("repairs_pontifex_robe"));
    /** Repairs the hammer (data tag; gold blocks, as in its recipe). */
    public static final TagKey<Item> HAMMER_REPAIR = TagKey.create(Registries.ITEM, ThaumaturgicConcilium.id("repairs_pontifex_hammer"));

    /** TC4 PONTIFEXROBE material: durability 30, protection {4, 8, 7, 4}, enchantability 25. */
    public static final ArmorMaterial PONTIFEX_ROBE = new ArmorMaterial(30, Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 8,
            ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4), 25, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ROBE_REPAIR, asset("pontifex_robe"));
    /** TC4 PHAMMER tool material: harvest 4, durability 1000, efficiency 8, damage 14, enchantability 20. */
    public static final ToolMaterial PONTIFEX_HAMMER_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1000, 8.0F, 14.0F, 20,
            HAMMER_REPAIR);

    public static final DeferredItem<PontifexRobeItem> PONTIFEX_ROBE_HEAD = robe("pontifex_robe_head", ArmorType.HELMET);
    public static final DeferredItem<PontifexRobeItem> PONTIFEX_ROBE_CHEST = robe("pontifex_robe_chest", ArmorType.CHESTPLATE);
    public static final DeferredItem<PontifexRobeItem> PONTIFEX_ROBE_LEGS = robe("pontifex_robe_legs", ArmorType.LEGGINGS);
    public static final DeferredItem<PontifexRobeItem> PONTIFEX_ROBE_FEET = robe("pontifex_robe_feet", ArmorType.BOOTS);
    /** 1 (hand) + 14 (material) + 3 = TC4's 18 sword damage (4 + 14); sword speed. */
    public static final DeferredItem<PontifexHammerItem> PONTIFEX_HAMMER = ITEMS.registerItem("pontifex_hammer",
            p -> new PontifexHammerItem(p.sword(PONTIFEX_HAMMER_MATERIAL, 3.0F, -2.4F).rarity(Rarity.EPIC)
                    .component(TTDataComponents.STACK_WARP.get(), 3)));
    public static final DeferredItem<ProtobodyItem> PROTOBODY = ITEMS.registerItem("protobody",
            p -> new ProtobodyItem(p.stacksTo(1).rarity(Rarity.RARE)));

    // ---- spawn eggs ----
    public static final DeferredItem<SpawnEggItem> BRIGHTEST_ONE_EGG = egg("brightest_one", BRIGHTEST_ONE);
    public static final DeferredItem<SpawnEggItem> LESSER_PORTAL_EGG = egg("lesser_crimson_portal", LESSER_PORTAL);
    public static final DeferredItem<SpawnEggItem> CRIMSON_PONTIFEX_EGG = egg("crimson_pontifex", CRIMSON_PONTIFEX);
    public static final DeferredItem<SpawnEggItem> PROTOLIMB_EGG = egg("protolimb", PROTOLIMB);

    // ---- player state (TC4 TCPlayerCapabilities) ----
    /** Intangible: no collision, no damage, no healing (synced, the client needs it for noclip). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> ETHEREAL = ATTACHMENTS.register("ethereal",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("v")).sync(ByteBufCodecs.BOOL).build());
    /** Ethereal by the robe's own toggle (dropped when the set comes off). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> ROBE_TOGGLE = ATTACHMENTS.register("pontifex_robe_toggle",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("v")).sync(ByteBufCodecs.BOOL).build());
    /** Flesh lost while ethereal: each point takes one from the player's health cap. Kept through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> FLESH_LOST = ATTACHMENTS.register("flesh_lost",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("v")).sync(ByteBufCodecs.VAR_INT).copyOnDeath().build());
    /** Protolimbs absorbed (6 = whole again: no more flesh loss, Solid Void from the wand). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PROTOLIMBS = ATTACHMENTS.register("protolimbs",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("v")).sync(ByteBufCodecs.VAR_INT).build());

    private TCBosses() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id(name));
    }

    private static ResourceKey<EquipmentAsset> asset(String path) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, ThaumaturgicConcilium.id(path));
    }

    private static DeferredItem<PontifexRobeItem> robe(String name, ArmorType type) {
        return ITEMS.registerItem(name, p -> {
            Item.Properties props = p.humanoidArmor(PONTIFEX_ROBE, type).rarity(Rarity.EPIC)
                    .component(TTDataComponents.RUNIC_CHARGE.get(), PontifexRobeItem.RUNIC_CHARGE)
                    .component(TTDataComponents.STACK_WARP.get(), PontifexRobeItem.WARP);
            if (type == ArmorType.HELMET) {
                props = props.component(TTDataComponents.GOGGLES_UPGRADE.get(), net.minecraft.util.Unit.INSTANCE);
            }
            return new PontifexRobeItem(props, type);
        });
    }

    private static <T extends Mob> DeferredItem<SpawnEggItem> egg(String name, Supplier<EntityType<T>> type) {
        return ITEMS.registerItem(name + "_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ATTACHMENTS.register(modBus);
        for (Supplier<? extends Item> item : java.util.List.<Supplier<? extends Item>>of(PONTIFEX_ROBE_HEAD, PONTIFEX_ROBE_CHEST, PONTIFEX_ROBE_LEGS,
                PONTIFEX_ROBE_FEET, PONTIFEX_HAMMER, PROTOBODY, FLESH_CRUCIBLE_ITEM, SOLID_VOID_ITEM, BRIGHTEST_ONE_EGG, LESSER_PORTAL_EGG,
                CRIMSON_PONTIFEX_EGG, PROTOLIMB_EGG)) {
            TCRegistry.addToTab(item);
        }
        modBus.addListener(TCBosses::attributes);
        modBus.addListener(TCBosses::payloads);
        Ethereal.register(NeoForge.EVENT_BUS);
        CrimsonInitiation.register(NeoForge.EVENT_BUS);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(BRIGHTEST_ONE.get(), BrightestOneEntity.createAttributes().build());
        event.put(LESSER_PORTAL.get(), LesserPortalEntity.createAttributes().build());
        event.put(CRIMSON_PONTIFEX.get(), CrimsonPontifexEntity.createAttributes().build());
        event.put(PROTOLIMB.get(), ProtolimbEntity.createAttributes().build());
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(RobeTogglePayload.TYPE, RobeTogglePayload.STREAM_CODEC, RobeTogglePayload::handle);
    }

    /**
     * What a chained rift lets through once its research is known (TC4 {@code RiftEntity} state 1-3): 1 the Brightest
     * One (a charged wisp), 2 a Lesser Crimson Portal that opens on the Crimson Pontifex (Crimson Annales), 3 a Peeler
     * of Matter (Primordial Life). Returns the new entity, or null for any other state.
     */
    public static @Nullable Entity spawnFromRift(ServerLevel level, Vec3 pos, int state) {
        Entity made = switch (state) {
            case 1 -> BRIGHTEST_ONE.get().create(level, EntitySpawnReason.EVENT);
            case 2 -> {
                LesserPortalEntity portal = LESSER_PORTAL.get().create(level, EntitySpawnReason.EVENT);
                if (portal != null) {
                    portal.setBossPortal(true);
                }
                yield portal;
            }
            case 3 -> MATERIAL_PEELER.get().create(level, EntitySpawnReason.EVENT);
            default -> null;
        };
        if (made == null) {
            return null;
        }
        made.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (made instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.EVENT, null);
        }
        level.addFreshEntity(made);
        return made;
    }

    /** Whether an item stack is a Pontifex robe piece. */
    static boolean isRobe(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof PontifexRobeItem;
    }
}
