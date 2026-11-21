package com.mustangdoc.sgjpatch.transporter;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Container id and current server menu are both checked; clients cannot supply arbitrary coordinates. */
public final class TransporterPackets {
    private TransporterPackets() {}

    public static void init(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2.0.5-transporter1");
        registrar.playToServer(Select.TYPE, Select.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender
                    && sender.containerMenu instanceof TransporterMenu menu
                    && menu.containerId == packet.container()) menu.select(sender, packet.destination());
        }));
        registrar.playToClient(Directory.TYPE, Directory.CODEC,
                (packet, context) -> context.enqueueWork(() -> Client.receive(packet)));
    }

    public static void send(int container, BlockPos destination) {
        PacketDistributor.sendToServer(new Select(container, destination));
    }

    public static void update(ServerPlayer player, int container, List<TransporterNetwork.Station> stations) {
        PacketDistributor.sendToPlayer(player, new Directory(container, stations));
    }

    public record Select(int container, BlockPos destination) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "transporter_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Select> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeVarInt(value.container); buf.writeBlockPos(value.destination); },
                buf -> new Select(buf.readVarInt(), buf.readBlockPos()));
        @Override public Type<Select> type() { return TYPE; }
    }

    public record Directory(int container, List<TransporterNetwork.Station> stations) implements CustomPacketPayload {
        public static final Type<Directory> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "transporter_directory"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Directory> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeVarInt(value.container); TransporterMenu.writeStations(buf, value.stations); },
                buf -> new Directory(buf.readVarInt(), TransporterMenu.readStations(buf)));
        @Override public Type<Directory> type() { return TYPE; }
    }

    private static final class Client {
        static void receive(Directory packet) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.containerMenu instanceof TransporterMenu menu
                    && menu.containerId == packet.container()) {
                menu.destinations = List.copyOf(packet.stations());
                if (minecraft.screen instanceof TransporterScreen screen) screen.refreshDirectory();
            }
        }
    }
}
