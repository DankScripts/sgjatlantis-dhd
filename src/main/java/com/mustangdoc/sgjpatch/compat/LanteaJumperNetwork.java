package com.mustangdoc.sgjpatch.compat;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "sgjatlantis_dhd")
public final class LanteaJumperNetwork {
    private LanteaJumperNetwork() {}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2.0.5-lantea-live1");
        registrar.playToClient(State.TYPE, State.CODEC, (p,c) -> c.enqueueWork(() -> Client.receive(p)));
        registrar.playToClient(DialGuide.TYPE, DialGuide.CODEC, (p,c) -> c.enqueueWork(() -> Client.receive(p)));
        registrar.playToClient(TableLights.TYPE, TableLights.CODEC, (p,c) -> c.enqueueWork(() -> Client.receive(p)));
        registrar.playToServer(Command.TYPE, Command.CODEC, (p,c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && !AtlantisNativeDhd.command(player,p.token,p.action,p.address))
                LanteaJumperBridge.command(player,p.token,p.action,p.address);
        }));
    }
    private static final class Client {
        static void receive(State p) { LanteaJumperScreen.receive(p); }
        static void receive(DialGuide p) { LanteaJumperScreen.receiveGuide(p); }
        static void receive(TableLights p) { LanteaDhdTableLights.receive(p.dimension,p.pos,p.glyphSet,p.symbols,p.connected); }
    }
    public static void send(ServerPlayer player, State state) { PacketDistributor.sendToPlayer(player,state); }
    public static void sendTableLights(ServerPlayer player, ResourceLocation dimension, BlockPos pos, String glyphSet, int[] symbols, boolean connected) {
        PacketDistributor.sendToPlayer(player,new TableLights(dimension,pos,glyphSet,symbols,connected));
    }
    public static void command(UUID token,int action,String address) { PacketDistributor.sendToServer(new Command(token,action,address)); }
    public static void sendGuide(ServerPlayer player,UUID token,String address,int[] keys,String error) {
        PacketDistributor.sendToPlayer(player,new DialGuide(token,address,keys,error));
    }
    /** Lantea-style translated button sequence for a pasted stationary-table address. */
    public record DialGuide(UUID token, String address, int[] keys, String error) implements CustomPacketPayload {
        public static final Type<DialGuide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "dialguide"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DialGuide> CODEC = StreamCodec.of((b,p) -> p.encode(b), DialGuide::decode);
        @Override public Type<DialGuide> type() { return TYPE; }
        void encode(RegistryFriendlyByteBuf b) {
            b.writeUUID(token); b.writeUtf(address, 80); b.writeVarIntArray(keys); b.writeUtf(error, 160);
        }
        static DialGuide decode(RegistryFriendlyByteBuf b) {
            return new DialGuide(b.readUUID(), b.readUtf(80), b.readVarIntArray(9), b.readUtf(160));
        }
    }
    public record State(UUID token, UUID jumper, boolean open, boolean valid, int symbols, Component message, String status,
                        String backend, String glyphSet, int[] encodedSymbols, int[] lockedPositions, int movingPosition,
                        int[] typedSymbols, String[] memory, BlockPos nativeGatePos) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((b,p) -> p.encode(b), State::decode);
        @Override public Type<State> type() { return TYPE; }
        public State(UUID token, UUID jumper, boolean open, boolean valid, int symbols, Component message, String status,
                     String backend, String glyphSet, int[] encodedSymbols, int[] lockedPositions, int movingPosition, int[] typedSymbols) {
            this(token,jumper,open,valid,symbols,message,status,backend,glyphSet,encodedSymbols,lockedPositions,movingPosition,typedSymbols,new String[0],null);
        }
        public State(UUID token, UUID jumper, boolean open, boolean valid, int symbols, Component message, String status,
                     String backend, String glyphSet, int[] encodedSymbols, int[] lockedPositions, int movingPosition,
                     int[] typedSymbols, String[] memory) {
            this(token,jumper,open,valid,symbols,message,status,backend,glyphSet,encodedSymbols,lockedPositions,
                    movingPosition,typedSymbols,memory,null);
        }
        public State(UUID token, UUID jumper, boolean open, boolean valid, int symbols, Component message, String status,
                     String backend, String glyphSet, int[] encodedSymbols, int[] lockedPositions, int movingPosition) {
            this(token,jumper,open,valid,symbols,message,status,backend,glyphSet,encodedSymbols,lockedPositions,movingPosition,new int[0]);
        }
        void encode(RegistryFriendlyByteBuf b) {
            b.writeUUID(token); b.writeUUID(jumper); b.writeBoolean(open); b.writeBoolean(valid); b.writeVarInt(symbols);
            ComponentSerialization.STREAM_CODEC.encode(b, message); b.writeUtf(status, 40);
            b.writeUtf(backend, 16); b.writeUtf(glyphSet, 80); b.writeVarIntArray(encodedSymbols); b.writeVarIntArray(lockedPositions); b.writeInt(movingPosition);
            b.writeVarIntArray(typedSymbols);
            b.writeVarInt(memory.length);
            for(String address:memory)b.writeUtf(address,80);
            b.writeNullable(nativeGatePos, RegistryFriendlyByteBuf::writeBlockPos);
        }
        static State decode(RegistryFriendlyByteBuf b) {
            return new State(b.readUUID(), b.readUUID(), b.readBoolean(), b.readBoolean(), b.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(b), b.readUtf(40),
                    b.readUtf(16), b.readUtf(80), b.readVarIntArray(9), b.readVarIntArray(9), b.readInt(),
                    b.readVarIntArray(9), readMemory(b), b.readNullable(RegistryFriendlyByteBuf::readBlockPos));
        }
    }
    private static String[] readMemory(RegistryFriendlyByteBuf b) {
        int count=b.readVarInt();
        if(count<0 || count>8)throw new IllegalArgumentException("Invalid DHD memory size");
        String[] entries=new String[count];
        for(int i=0;i<count;i++)entries[i]=b.readUtf(80);
        return entries;
    }
    public record Command(UUID token, int action, String address) implements CustomPacketPayload {
        public static final Type<Command> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Command> CODEC = StreamCodec.of((b,p) -> p.encode(b), Command::decode);
        @Override public Type<Command> type() { return TYPE; }
        void encode(RegistryFriendlyByteBuf b) { b.writeUUID(token); b.writeByte(action); b.writeUtf(address, 80); }
        static Command decode(RegistryFriendlyByteBuf b) { return new Command(b.readUUID(), b.readUnsignedByte(), b.readUtf(80)); }
    }
    public record TableLights(ResourceLocation dimension, BlockPos pos, String glyphSet,
                              int[] symbols, boolean connected) implements CustomPacketPayload {
        public static final Type<TableLights> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "tablelights"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TableLights> CODEC = StreamCodec.of((b,p) -> p.encode(b), TableLights::decode);
        @Override public Type<TableLights> type() { return TYPE; }
        void encode(RegistryFriendlyByteBuf b) {
            b.writeResourceLocation(dimension); b.writeBlockPos(pos); b.writeUtf(glyphSet, 8);
            b.writeVarIntArray(symbols); b.writeBoolean(connected);
        }
        static TableLights decode(RegistryFriendlyByteBuf b) {
            return new TableLights(b.readResourceLocation(), b.readBlockPos(), b.readUtf(8),
                    b.readVarIntArray(9), b.readBoolean());
        }
    }
}


