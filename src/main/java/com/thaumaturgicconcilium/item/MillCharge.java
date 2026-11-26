package com.thaumaturgicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

/** What a Shard Mill holds: the aspect of the vis crystal it ground, how many ticks of powder are left, and whether it is open. */
public record MillCharge(ResourceKey<IAspect> aspect, int ticks, boolean open) {
    public static final Codec<MillCharge> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(MillCharge::aspect),
            Codec.INT.fieldOf("ticks").forGetter(MillCharge::ticks),
            Codec.BOOL.optionalFieldOf("open", false).forGetter(MillCharge::open)).apply(i, MillCharge::new));
    public static final StreamCodec<ByteBuf, MillCharge> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(IAspect.REGISTRY_KEY), MillCharge::aspect,
            ByteBufCodecs.VAR_INT, MillCharge::ticks,
            ByteBufCodecs.BOOL, MillCharge::open, MillCharge::new);

    public MillCharge withTicks(int t) {
        return new MillCharge(aspect, t, open);
    }

    public MillCharge withOpen(boolean o) {
        return new MillCharge(aspect, ticks, o);
    }
}
