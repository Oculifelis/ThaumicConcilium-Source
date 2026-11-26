package com.thaumaturgicconcilium.compat.client;

import com.mojang.serialization.MapCodec;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.compat.horizons.DrainageSyringeItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import org.jspecify.annotations.Nullable;

/**
 * Client side of the compat part B base content: the Drainage Syringe's {@code thaumaturgicconcilium:syringe_filled} item-model
 * condition (true while it holds vis), which shows the purple fill layer TC4's renderer drew.
 */
public final class CompatBClient {
    private CompatBClient() {
    }

    public record SyringeFilled() implements ConditionalItemModelProperty {
        public static final MapCodec<SyringeFilled> MAP_CODEC = MapCodec.unit(new SyringeFilled());

        @Override
        public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
            return DrainageSyringeItem.data(stack).amount() > 0;
        }

        @Override
        public MapCodec<SyringeFilled> type() {
            return MAP_CODEC;
        }
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterConditionalItemModelPropertyEvent event) -> event.register(ThaumaturgicConcilium.id("syringe_filled"),
                SyringeFilled.MAP_CODEC));
    }
}
