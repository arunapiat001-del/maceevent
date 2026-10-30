package com.deathdim.mace;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Sinyal dari plugin: mulai animasi selebrasi mace. ticks = total durasi (20 tick = 1 detik). */
public record MacePayload(int ticks) implements CustomPayload {
    public static final CustomPayload.Id<MacePayload> ID =
            new CustomPayload.Id<>(Identifier.of("maceevent", "celebrate"));

    public static final PacketCodec<PacketByteBuf, MacePayload> CODEC = PacketCodec.of(
            (v, buf) -> buf.writeInt(v.ticks()),
            buf -> new MacePayload(buf.readInt())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
