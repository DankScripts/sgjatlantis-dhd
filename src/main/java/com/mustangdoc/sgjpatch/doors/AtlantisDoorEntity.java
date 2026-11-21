package com.mustangdoc.sgjpatch.doors;
import net.minecraft.core.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class AtlantisDoorEntity extends BlockEntity {
 private boolean open;private float from;private long started;
 private final ClientDoorAnimation clientAnimation=new ClientDoorAnimation(1800);
 public AtlantisDoorEntity(BlockPos p,BlockState s){super(AtlantisDoorRegistry.ENTITY,p,s);}
 public Direction facing(){return getBlockState().getValue(AtlantisDoorBlock.FACING);}
 public boolean solid(){return getBlockState().getValue(AtlantisDoorBlock.SOLID);}
 public float progress(float partial){
  if(level==null)return open?1:0;
  if(level.isClientSide)return clientAnimation.progress(open,net.minecraft.Util.getMillis());
  return net.minecraft.util.Mth.lerp(net.minecraft.util.Mth.clamp((level.getGameTime()+partial-started)/36f,0,1),from,open?1:0);
 }
 public void toggle(){if(!(level instanceof ServerLevel l))return;from=progress(0);started=l.getGameTime();open=!open;setChanged();for(var player:l.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(worldPosition),false))player.connection.send(getUpdatePacket());}
 public static void tick(ServerLevel l,AtlantisDoorEntity e){if(l.getGameTime()%2!=0)return;float t=e.progress(0);int stage=Math.round(t*t*(3-2*t)*8);for(int col=1;col<=3;col++)for(int row=0;row<4;row++){BlockPos p=AtlantisDoorBlock.at(e.worldPosition,e.facing(),col,row);BlockState s=l.getBlockState(p);if(s.is(AtlantisDoorRegistry.BLOCK)&&s.getValue(AtlantisDoorBlock.STAGE)!=stage)l.setBlock(p,s.setValue(AtlantisDoorBlock.STAGE,stage),2);}}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider registries){super.saveAdditional(t,registries);t.putBoolean("Open",open);t.putFloat("From",from);t.putLong("Started",started);}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider registries){super.loadAdditional(t,registries);open=t.getBoolean("Open");from=t.getFloat("From");started=t.getLong("Started");if(level!=null&&level.isClientSide)clientAnimation.setTarget(open,net.minecraft.Util.getMillis());}
}
