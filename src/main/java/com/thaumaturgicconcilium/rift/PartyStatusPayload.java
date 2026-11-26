package com.thaumaturgicconcilium.rift;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, every 10 ticks to a lithographed player (TC4 {@code PacketUpdatePartyStatus}): for each of the four
 * imprinted targets its health 0..10, blood, mana and runic shielding 0..9, and the brain value of a watched place;
 * -1 means nothing to show.
 */
public record PartyStatusPayload(List<Integer> health, List<Integer> blood, List<Integer> mana, List<Integer> runes, List<Integer> brains)
        implements CustomPacketPayload {
    public static final Type<PartyStatusPayload> TYPE = new Type<>(ThaumaturgicConcilium.id("party_status"));
    private static final StreamCodec<ByteBuf, List<Integer>> FOUR = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(4));
    public static final StreamCodec<ByteBuf, PartyStatusPayload> STREAM_CODEC = StreamCodec.composite(
            FOUR, PartyStatusPayload::health,
            FOUR, PartyStatusPayload::blood,
            FOUR, PartyStatusPayload::mana,
            FOUR, PartyStatusPayload::runes,
            FOUR, PartyStatusPayload::brains,
            PartyStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
