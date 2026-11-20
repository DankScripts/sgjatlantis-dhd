package com.mustangdoc.sgjpatch.compat;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LanteaJumperNetwork {
    private static final String VERSION = "10";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("sgjadditions_capacity_patch", "lantea_jumper"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private LanteaJumperNetwork() {}

    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        CHANNEL.messageBuilder(State.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(State::encode).decoder(State::decode).consumerMainThread(State::handle).add();
        CHANNEL.messageBuilder(Command.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Command::encode).decoder(Command::decode).consumerMainThread(Command::handle).add();
        CHANNEL.messageBuilder(TableLights.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TableLights::encode).decoder(TableLights::decode).consumerMainThread(TableLights::handle).add();
        CHANNEL.messageBuilder(DialGuide.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialGuide::encode).decoder(DialGuide::decode).consumerMainThread(DialGuide::handle).add();
    }
    public static void send(ServerPlayer player, State state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
    }
    public static void sendTableLights(ServerPlayer player, ResourceLocation dimension, BlockPos pos,
                                       String glyphSet, int[] symbols, boolean connected) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TableLights(dimension, pos, glyphSet, symbols, connected));
    }
    public static void command(UUID token, int action, String address) {
        CHANNEL.sendToServer(new Command(token, action, address));
    }
    public static void sendGuide(ServerPlayer player, UUID token, String address, int[] keys, String error) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DialGuide(token, address, keys, error));
    }
    /** Lantea-style translated button sequence for a pasted stationary-table address. */
    public record DialGuide(UUID token, String address, int[] keys, String error) {
        void encode(FriendlyByteBuf b) {
            b.writeUUID(token); b.writeUtf(address, 80); b.writeVarIntArray(keys); b.writeUtf(error, 160);
        }
        static DialGuide decode(FriendlyByteBuf b) {
            return new DialGuide(b.readUUID(), b.readUtf(80), b.readVarIntArray(9), b.readUtf(160));
        }
        static void handle(DialGuide guide, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LanteaJumperScreen.receiveGuide(guide));
            context.get().setPacketHandled(true);
        }
    }
    public record State(UUID token, UUID jumper, boolean open, boolean valid, int symbols, Component message, String status,
                        String backend, String glyphSet, int[] encodedSymbols, int[] lockedPositions, int movingPosition, int[] typedSymbols, String[] memory) {
        public State(UUID token, UUID jumper, boolean open, boolean valid, int symbols, Component message, String status,
                     String backend, String glyphSet, int[] encodedSymbols, int[] lockedPositions, int movingPosition, int[] typedSymbols) {
            this(token,jumper,open,valid,symbols,message,status,backend,glyphSet,encodedSymbols,lockedPositions,movingPosition,typedSymbols,new String[0]);
        }
        public State(UUID token, UUID jumper, boolean open, boolean valid, int symbols, Component message, String status,
                     String backend, String glyphSet, int[] encodedSymbols, int[] lockedPositions, int movingPosition) {
            this(token,jumper,open,valid,symbols,message,status,backend,glyphSet,encodedSymbols,lockedPositions,movingPosition,new int[0]);
        }
        void encode(FriendlyByteBuf b) {
            b.writeUUID(token); b.writeUUID(jumper); b.writeBoolean(open); b.writeBoolean(valid); b.writeVarInt(symbols);
            b.writeComponent(message); b.writeUtf(status, 40);
            b.writeUtf(backend, 16); b.writeUtf(glyphSet, 80); b.writeVarIntArray(encodedSymbols); b.writeVarIntArray(lockedPositions); b.writeInt(movingPosition);
            b.writeVarIntArray(typedSymbols);
            b.writeVarInt(memory.length);
            for(String address:memory)b.writeUtf(address,80);
        }
        static State decode(FriendlyByteBuf b) {
            return new State(b.readUUID(), b.readUUID(), b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readComponent(), b.readUtf(40),
                    b.readUtf(16), b.readUtf(80), b.readVarIntArray(9), b.readVarIntArray(9), b.readInt(), b.readVarIntArray(9), readMemory(b));
        }
        static void handle(State state, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LanteaJumperScreen.receive(state));
            context.get().setPacketHandled(true);
        }
    }
    private static String[] readMemory(FriendlyByteBuf b) {
        int count=b.readVarInt();
        if(count<0 || count>8)throw new IllegalArgumentException("Invalid DHD memory size");
        String[] entries=new String[count];
        for(int i=0;i<count;i++)entries[i]=b.readUtf(80);
        return entries;
    }
    public record Command(UUID token, int action, String address) {
        void encode(FriendlyByteBuf b) { b.writeUUID(token); b.writeByte(action); b.writeUtf(address, 80); }
        static Command decode(FriendlyByteBuf b) { return new Command(b.readUUID(), b.readUnsignedByte(), b.readUtf(80)); }
        static void handle(Command command, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && !AtlantisNativeDhd.command(player, command.token, command.action, command.address))
                LanteaJumperBridge.command(player, command.token, command.action, command.address);
            context.get().setPacketHandled(true);
        }
    }
    public record TableLights(ResourceLocation dimension, BlockPos pos, String glyphSet,
                              int[] symbols, boolean connected) {
        void encode(FriendlyByteBuf b) {
            b.writeResourceLocation(dimension); b.writeBlockPos(pos); b.writeUtf(glyphSet, 8);
            b.writeVarIntArray(symbols); b.writeBoolean(connected);
        }
        static TableLights decode(FriendlyByteBuf b) {
            return new TableLights(b.readResourceLocation(), b.readBlockPos(), b.readUtf(8),
                    b.readVarIntArray(9), b.readBoolean());
        }
        static void handle(TableLights state, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    LanteaDhdTableLights.receive(state.dimension, state.pos, state.glyphSet,
                            state.symbols, state.connected));
            context.get().setPacketHandled(true);
        }
    }
}


