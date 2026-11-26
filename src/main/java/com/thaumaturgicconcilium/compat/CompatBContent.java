package com.thaumaturgicconcilium.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.horizons.DrainageSyringeItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Compat part B content that needs no partner mod: the Drainage Syringe (TC4 registered it only with Thaumic Horizons,
 * Botania and Blood Magic present because its class linked their APIs; its vis mode needs none of them, so it is base
 * content here, with a Horizons recipe and a plain one). The Valet and Assistant seals are in {@code compat.golem}.
 */
public final class CompatBContent {
    private static final String MODID = ThaumaturgicConcilium.MODID;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    /** The syringe's load: vis drawn (0..100) and whether the cap is off (open: it feeds wands instead of drawing). */
    public record Drainage(int amount, boolean open) {
        public static final Drainage EMPTY = new Drainage(0, false);
        public static final Codec<Drainage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, DrainageSyringeItem.MAX_VIS).optionalFieldOf("amount", 0).forGetter(Drainage::amount),
                Codec.BOOL.optionalFieldOf("open", false).forGetter(Drainage::open)
        ).apply(i, Drainage::new));
        public static final StreamCodec<ByteBuf, Drainage> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Drainage::amount, ByteBufCodecs.BOOL, Drainage::open, Drainage::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Drainage>> DRAINAGE = COMPONENTS.registerComponentType(
            "drainage", b -> b.persistent(Drainage.CODEC).networkSynchronized(Drainage.STREAM_CODEC));

    public static final DeferredItem<DrainageSyringeItem> DRAINAGE_SYRINGE = ITEMS.registerItem("drainage_syringe",
            p -> new DrainageSyringeItem(p.stacksTo(1).rarity(Rarity.UNCOMMON).component(DRAINAGE.get(), Drainage.EMPTY)));

    private CompatBContent() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        TCRegistry.addToTab(DRAINAGE_SYRINGE);
    }
}
