package com.thaumaturgicconcilium.boss;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.MobUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the robe key (TC4 {@code PacketTogglePontifexRobe}, key L). With the full robe on, flips the
 * robe's own ethereal state. A player made ethereal by a Peeler of Matter cannot use it to become solid again.
 */
public record RobeTogglePayload() implements CustomPacketPayload {
    public static final Type<RobeTogglePayload> TYPE = new Type<>(ThaumaturgicConcilium.id("pontifex_robe_toggle"));
    public static final RobeTogglePayload INSTANCE = new RobeTogglePayload();
    public static final StreamCodec<ByteBuf, RobeTogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(RobeTogglePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!Ethereal.isFullSet(player)) {
            return;
        }
        boolean toggled = Ethereal.robeToggled(player);
        if (!toggled && Ethereal.isEthereal(player)) {
            return;
        }
        Ethereal.setRobeToggled(player, !toggled);
        Ethereal.setEthereal(player, !toggled);
        if (!toggled) {
            MobUtil.play(player.level(), player, MobUtil.tcSound("shackles").get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
