package com.mustangdoc.sgjpatch.transporter;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TransporterEntity extends BlockEntity {
    public static final int DOOR_TICKS=36;
    private boolean clientInitialized,clientTarget;
    private float clientFrom;
    private long clientStarted;
    private float clientProgress(){
        long now=net.minecraft.Util.getMillis();
        if(!clientInitialized){clientInitialized=true;clientTarget=doorOpen;clientFrom=doorOpen?1:0;clientStarted=now;}
        float t=net.minecraft.util.Mth.clamp((now-clientStarted)/(DOOR_TICKS*50f),0,1);
        float current=net.minecraft.util.Mth.lerp(t,clientFrom,clientTarget?1:0);
        if(clientTarget!=doorOpen){clientFrom=current;clientTarget=doorOpen;clientStarted=now;}
        return current;
    }
    private String name="Transporter";
    // 0 closed, 1 boarding until a destination is selected, 2 unloading until empty.
    private int doorMode;
    public boolean reserved;
    private boolean doorOpen;
    private float animationFrom;
    private long animationStart;
    private int layoutRevision;
    public float progress(float partial){
        if(level==null)return 0;
        if(level.isClientSide)return clientProgress();
        float t=net.minecraft.util.Mth.clamp((level.getGameTime()+partial-animationStart)/DOOR_TICKS,0,1);
        return net.minecraft.util.Mth.lerp(t,animationFrom,doorOpen?1:0);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet){
        // Prime the old target before receiving its replacement. Visual timing is local and
        // independent of world-time corrections or repeated block-entity NBT updates.
        if(level!=null&&level.isClientSide)clientProgress();
        if(packet.getTag()!=null)load(packet.getTag());
    }
    private void updateDoorTarget(){
        if(!(level instanceof ServerLevel l))return;
        boolean open=!reserved&&doorMode!=0;
        if(open==doorOpen)return;
        animationFrom=progress(0);animationStart=l.getGameTime();doorOpen=open;setChanged();
        // Send immediately instead of waiting for the chunk's batched block updates.
        var packet=getUpdatePacket();
        for(var player:l.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(worldPosition),false))player.connection.send(packet);
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).inflate(6);}
    public TransporterEntity(BlockPos p,BlockState s){super(TransporterRegistry.ENTITY,p,s);}
    public BlockPos anchor(){return worldPosition.below();}
    public Direction facing(){return getBlockState().getValue(TransporterBlock.FACING);}
    public String stationName(){return name;}
    public void rename(String value){
        name=value.strip();if(name.isEmpty())name="Transporter";if(name.length()>48)name=name.substring(0,48);
        setChanged();if(level instanceof ServerLevel sl)TransporterNetwork.get(sl).register(new TransporterNetwork.Station(anchor(),facing(),name));
    }
    public void toggleDoor(){
        if(reserved)return;
        // Keep the arrival exit open until its passengers have cleared it.
        if(doorMode==2&&level instanceof ServerLevel l&&occupied(l))return;
        doorMode=doorMode==0?1:0;setChanged();updateDoorTarget();
    }
    public void openForEntry(){doorMode=1;setChanged();updateDoorTarget();}
    public void openForArrival(){doorMode=2;setChanged();updateDoorTarget();}
    private boolean occupied(ServerLevel l){
        var area=TransporterRoom.interior(anchor(),facing()).minmax(new net.minecraft.world.phys.AABB(TransporterRoom.at(anchor(),facing(),0,0,3)).inflate(1,1,1));
        return !l.getEntities((net.minecraft.world.entity.Entity)null,area,e->e.isAlive()&&!e.isSpectator()).isEmpty();
    }
    public static void tick(ServerLevel l,TransporterEntity be){
        long time=l.getGameTime();
        if(be.layoutRevision<6){
            if(time%100==0)TransporterRoom.shorten(l,be);
            return;
        }
        if(Math.floorMod(time+be.worldPosition.asLong(),100)==0){
            if(TransporterRoom.complete(l,be.anchor(),be.facing()))TransporterNetwork.get(l).register(new TransporterNetwork.Station(be.anchor(),be.facing(),be.name));
            else TransporterNetwork.get(l).remove(be.anchor());
        }
        if(time%2!=0)return;
        if(!be.reserved&&be.doorMode==2&&be.doorOpen&&be.progress(0)>=1&&!be.occupied(l)){be.doorMode=0;be.setChanged();}
        be.updateDoorTarget();
        for(int x=-1;x<=1;x++)for(int y=0;y<4;y++){
            BlockPos p=TransporterRoom.at(be.anchor(),be.facing(),x,y,3);BlockState s=l.getBlockState(p);
            if(!s.is(TransporterRegistry.BLOCK)||(s.getValue(TransporterBlock.PART)!=TransporterBlock.Part.DOOR&&s.getValue(TransporterBlock.PART)!=TransporterBlock.Part.CROWN))continue;
            float p0=be.progress(0);
            int stage=s.getValue(TransporterBlock.STAGE),next=Math.round(p0*p0*(3-2*p0)*8);
            if(next!=stage)l.setBlock(p,s.setValue(TransporterBlock.STAGE,next),2);
        }
    }
    public void currentLayout(){layoutRevision=6;setChanged();}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putString("StationName",name);t.putInt("LayoutRevision",layoutRevision);t.putBoolean("DoorOpen",doorOpen);t.putInt("DoorMode",doorMode);t.putFloat("AnimationFrom",animationFrom);t.putLong("AnimationStart",animationStart);}
    @Override public void load(CompoundTag t){super.load(t);name=t.contains("StationName")?t.getString("StationName"):"Transporter";reserved=false;layoutRevision=t.getInt("LayoutRevision");doorOpen=t.getBoolean("DoorOpen");doorMode=t.contains("DoorMode")?t.getInt("DoorMode"):(doorOpen?1:0);animationFrom=t.getFloat("AnimationFrom");animationStart=t.getLong("AnimationStart");}
}

