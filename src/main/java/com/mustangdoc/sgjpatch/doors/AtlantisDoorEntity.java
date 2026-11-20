package com.mustangdoc.sgjpatch.doors;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public final class AtlantisDoorEntity extends BlockEntity {
 private boolean open,initialized,clientTarget;private float from,clientFrom;private long started,clientStarted;
 public AtlantisDoorEntity(BlockPos p,BlockState s){super(AtlantisDoorRegistry.ENTITY,p,s);}
 public Direction facing(){return getBlockState().getValue(AtlantisDoorBlock.FACING);}
 public boolean solid(){return getBlockState().getValue(AtlantisDoorBlock.SOLID);}
 public float progress(float partial){
  if(level==null)return open?1:0;
  if(level.isClientSide){long now=net.minecraft.Util.getMillis();if(!initialized){initialized=true;clientTarget=open;clientFrom=open?1:0;clientStarted=now;}float t=net.minecraft.util.Mth.clamp((now-clientStarted)/1800f,0,1);float current=net.minecraft.util.Mth.lerp(t,clientFrom,clientTarget?1:0);if(clientTarget!=open){clientFrom=current;clientTarget=open;clientStarted=now;}return current;}
  return net.minecraft.util.Mth.lerp(net.minecraft.util.Mth.clamp((level.getGameTime()+partial-started)/36f,0,1),from,open?1:0);
 }
 public void toggle(){if(!(level instanceof ServerLevel l))return;from=progress(0);started=l.getGameTime();open=!open;setChanged();for(var player:l.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(worldPosition),false))player.connection.send(getUpdatePacket());}
 public static void tick(ServerLevel l,AtlantisDoorEntity e){if(l.getGameTime()%2!=0)return;float t=e.progress(0);int stage=Math.round(t*t*(3-2*t)*8);for(int col=1;col<=3;col++)for(int row=0;row<4;row++){BlockPos p=AtlantisDoorBlock.at(e.worldPosition,e.facing(),col,row);BlockState s=l.getBlockState(p);if(s.is(AtlantisDoorRegistry.BLOCK)&&s.getValue(AtlantisDoorBlock.STAGE)!=stage)l.setBlock(p,s.setValue(AtlantisDoorBlock.STAGE,stage),2);}}
 @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(5);}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
 @Override public void onDataPacket(net.minecraft.network.Connection c,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket p){if(level!=null&&level.isClientSide)progress(0);if(p.getTag()!=null)load(p.getTag());}
 @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putBoolean("Open",open);t.putFloat("From",from);t.putLong("Started",started);}
 @Override public void load(CompoundTag t){super.load(t);open=t.getBoolean("Open");from=t.getFloat("From");started=t.getLong("Started");}
}
