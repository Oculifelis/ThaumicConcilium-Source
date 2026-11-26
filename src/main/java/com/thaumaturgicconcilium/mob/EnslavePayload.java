package com.thaumaturgicconcilium.mob;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the Vengeful Golem's mind control (TC4 {@code PacketEnslave}). {@code true} makes the client player
 * stumble about on its own until no Vengeful Golem is near; {@code false} frees it at once.
 */
public record EnslavePayload(boolean enslaved) implements CustomPacketPayload {
    public static final Type<EnslavePayload> TYPE = new Type<>(ThaumaturgicConcilium.id("enslave"));
    public static final StreamCodec<ByteBuf, EnslavePayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(EnslavePayload::new, EnslavePayload::enslaved);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
