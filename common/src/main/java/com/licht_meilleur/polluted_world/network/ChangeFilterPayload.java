package com.licht_meilleur.polluted_world.network;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChangeFilterPayload() implements CustomPacketPayload {
    public static final Type<ChangeFilterPayload> TYPE =
            new Type<>(PollutedWorldMod.id("change_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeFilterPayload> STREAM_CODEC =
            StreamCodec.unit(new ChangeFilterPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}