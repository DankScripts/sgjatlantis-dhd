package com.mustangdoc.sgjpatch.transporter;

import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Container id and current server menu are both checked; clients cannot supply arbitrary coordinates. */
public final class TransporterPackets {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(TransporterRegistry.id("transporter"),()->"1","1"::equals,"1"::equals);
    private record Select(int container,net.minecraft.core.BlockPos destination){}
    private record Directory(int container,java.util.List<TransporterNetwork.Station> stations){}
    static {
        CHANNEL.messageBuilder(Select.class,0,NetworkDirection.PLAY_TO_SERVER)
            .encoder((p,b)->{b.writeVarInt(p.container());b.writeBlockPos(p.destination());})
            .decoder(b->new Select(b.readVarInt(),b.readBlockPos()))
            .consumerMainThread((p,c)->{
                var sender=c.get().getSender();
                if(sender!=null&&sender.containerMenu instanceof TransporterMenu menu&&menu.containerId==p.container())
                    menu.select(sender,p.destination());
            }).add();
        CHANNEL.messageBuilder(Directory.class,1,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b)->{b.writeVarInt(p.container());TransporterMenu.writeStations(b,p.stations());})
            .decoder(b->new Directory(b.readVarInt(),TransporterMenu.readStations(b)))
            .consumerMainThread((p,c)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->Client.receive(p))).add();
    }
    public static void init(){}
    public static void send(int container,net.minecraft.core.BlockPos destination){CHANNEL.sendToServer(new Select(container,destination));}
    public static void update(net.minecraft.server.level.ServerPlayer p,int id,java.util.List<TransporterNetwork.Station> list){
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Directory(id,list));
    }
    private static final class Client {
        static void receive(Directory p){
            var mc=net.minecraft.client.Minecraft.getInstance();
            if(mc.player!=null&&mc.player.containerMenu instanceof TransporterMenu menu&&menu.containerId==p.container()){
                menu.destinations=java.util.List.copyOf(p.stations());
                if(mc.screen instanceof TransporterScreen screen)screen.refreshDirectory();
            }
        }
    }
}
