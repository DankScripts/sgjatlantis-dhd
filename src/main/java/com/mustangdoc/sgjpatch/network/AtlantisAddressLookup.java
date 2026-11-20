package com.mustangdoc.sgjpatch.network;

import com.mustangdoc.sgjpatch.client.util.AtlantisAddressGuide;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

public final class AtlantisAddressLookup {
    private AtlantisAddressLookup() {}

    public static void request(int container, BlockPos pos, String address) {
        SGJPatchNetwork.CHANNEL.sendToServer(new Request(container, pos, address));
    }

    public record Request(int container, BlockPos pos, String address) {
        public static void encode(Request packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.container); buffer.writeBlockPos(packet.pos); buffer.writeUtf(packet.address, 80);
        }
        public static Request decode(FriendlyByteBuf buffer) {
            return new Request(buffer.readVarInt(), buffer.readBlockPos(), buffer.readUtf(80));
        }
        public static void handle(Request packet, Supplier<NetworkEvent.Context> supplier) {
            ServerPlayer player = supplier.get().getSender();
            if (player != null && player.containerMenu instanceof AbstractDHDMenu<?> menu
                    && menu.containerId == packet.container && menu.getDHD().getBlockPos().equals(packet.pos)
                    && player.distanceToSqr(packet.pos.getX()+.5, packet.pos.getY()+.5, packet.pos.getZ()+.5) < 100) {
                String name = AtlantisAddressNames.resolve(player.level(), packet.address);
                SGJPatchNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new Response(packet.container, packet.pos, packet.address, name));
            }
            supplier.get().setPacketHandled(true);
        }
    }

    public record Response(int container, BlockPos pos, String address, String name) {
        public static void encode(Response packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.container); buffer.writeBlockPos(packet.pos);
            buffer.writeUtf(packet.address, 80); buffer.writeUtf(packet.name, 80);
        }
        public static Response decode(FriendlyByteBuf buffer) {
            return new Response(buffer.readVarInt(), buffer.readBlockPos(), buffer.readUtf(80), buffer.readUtf(80));
        }
        public static void handle(Response packet, Supplier<NetworkEvent.Context> supplier) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    AtlantisAddressGuide.receive(packet.container, packet.pos, packet.address, packet.name));
            supplier.get().setPacketHandled(true);
        }
    }
}
