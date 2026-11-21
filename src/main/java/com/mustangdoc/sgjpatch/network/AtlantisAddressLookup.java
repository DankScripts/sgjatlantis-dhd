package com.mustangdoc.sgjpatch.network;

import com.mustangdoc.sgjpatch.client.util.AtlantisAddressGuide;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

@EventBusSubscriber(modid = "sgjatlantis_dhd")
public final class AtlantisAddressLookup {
    private AtlantisAddressLookup() {}

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2.0.5-address-guide1");
        registrar.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof AbstractDHDMenu<?> menu
                    && menu.containerId == packet.container && menu.getDHD().getBlockPos().equals(packet.pos)
                    && player.distanceToSqr(packet.pos.getX()+.5,packet.pos.getY()+.5,packet.pos.getZ()+.5)<100) {
                String name = AtlantisAddressNames.resolve(player.level(), packet.address);
                PacketDistributor.sendToPlayer(player,new Response(packet.container,packet.pos,packet.address,name));
            }
        }));
        registrar.playToClient(Response.TYPE, Response.CODEC, (packet, context) -> context.enqueueWork(() -> Client.receive(packet)));
    }

    private static final class Client {
        static void receive(Response packet) {
            AtlantisAddressGuide.receive(packet.container,packet.pos,packet.address,packet.name);
        }
    }

    public static void request(int container, BlockPos pos, String address) {
        PacketDistributor.sendToServer(new Request(container,pos,address));
    }

    public record Request(int container, BlockPos pos, String address) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd","address_lookup_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC = StreamCodec.of((buffer,packet) -> {
            buffer.writeVarInt(packet.container); buffer.writeBlockPos(packet.pos); buffer.writeUtf(packet.address,80);
        },buffer -> new Request(buffer.readVarInt(),buffer.readBlockPos(),buffer.readUtf(80)));
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Response(int container, BlockPos pos, String address, String name) implements CustomPacketPayload {
        public static final Type<Response> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd","address_lookup_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC = StreamCodec.of((buffer,packet) -> {
            buffer.writeVarInt(packet.container); buffer.writeBlockPos(packet.pos);
            buffer.writeUtf(packet.address,80); buffer.writeUtf(packet.name,80);
        },buffer -> new Response(buffer.readVarInt(),buffer.readBlockPos(),buffer.readUtf(80),buffer.readUtf(80)));
        @Override public Type<Response> type() { return TYPE; }
    }
}
