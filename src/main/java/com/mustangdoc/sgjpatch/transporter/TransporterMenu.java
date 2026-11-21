package com.mustangdoc.sgjpatch.transporter;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TransporterMenu extends AbstractContainerMenu {
    public final BlockPos panel;
    public List<TransporterNetwork.Station> destinations;
    private final Player owner;
    public TransporterMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,buf.readBlockPos(),readStations(buf));}
    public TransporterMenu(int id,Inventory inv,BlockPos panel,List<TransporterNetwork.Station> stations){
        super(TransporterRegistry.MENU,id);this.panel=panel;destinations=List.copyOf(stations);owner=inv.player;
    }
    public static void writeStations(FriendlyByteBuf b,List<TransporterNetwork.Station> list){
        b.writeVarInt(Math.min(1024,list.size()));
        for(var s:list.stream().limit(1024).toList()){b.writeBlockPos(s.pos());b.writeEnum(s.facing());b.writeUtf(s.name(),48);}
    }
    public static List<TransporterNetwork.Station> readStations(FriendlyByteBuf b){
        int n=b.readVarInt();if(n<0||n>1024)throw new IllegalArgumentException("Invalid transporter directory size");
        List<TransporterNetwork.Station> result=new ArrayList<>();
        for(int i=0;i<n;i++)result.add(new TransporterNetwork.Station(b.readBlockPos(),b.readEnum(Direction.class),b.readUtf(48)));
        return result;
    }
    @Override public boolean stillValid(Player p){
        return p.level().getBlockEntity(panel) instanceof TransporterEntity be&&!be.reserved
            &&TransporterRoom.interior(be.anchor(),be.facing()).contains(p.position());
    }
    @Override public boolean clickMenuButton(Player p,int id){
        if(!(p instanceof ServerPlayer sp)||id<0||id>=destinations.size()||!stillValid(p))return false;
        if(p.level().getBlockEntity(panel) instanceof TransporterEntity be)return TransporterService.start(sp,be,destinations.get(id).pos());
        return false;
    }
    public void select(ServerPlayer player,BlockPos destination){
        for(int i=0;i<destinations.size();i++)if(destinations.get(i).pos().equals(destination)){clickMenuButton(player,i);return;}
    }
    @Override public void broadcastChanges(){
        super.broadcastChanges();
        if(owner instanceof ServerPlayer sp&&sp.serverLevel().getGameTime()%20==0){
            var latest=TransporterNetwork.get(sp.serverLevel()).stations().stream().limit(1024).toList();
            if(!latest.equals(destinations)){destinations=latest;TransporterPackets.update(sp,containerId,latest);}
        }
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
