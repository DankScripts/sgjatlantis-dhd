package com.mustangdoc.sgjpatch.power;
import com.mustangdoc.sgjpatch.compat.ConcealedCableRouting;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchCableBootstrapEvents;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchTableBlockEntity;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.blocks.tech.CableBlock;

/** Separate cached virtual outputs; real block capabilities always take priority. */
public final class ConcealedFloorDhdPowerBridge {
 private record Routes(long tick,Map<Direction,WeakReference<BlockEntity>> receivers) {}
 private static final Map<BlockEntity,Routes> CACHE=new WeakHashMap<>();
 private ConcealedFloorDhdPowerBridge() {}
 public static void invalidate(BlockEntity cable){CACHE.remove(cable);}
 public static IEnergyStorage getFloorEnergyCapability(Level level,BlockPos queriedPos,BlockEntity blockEntity,Direction side){
  if(blockEntity!=null||side==null)return null;
  BlockPos source=queriedPos.relative(side);
  if(!level.hasChunkAt(source)||!(level.getBlockEntity(source) instanceof CableBlockEntity.LargeNaquadahCable))return null;
  BlockEntity receiver=outputRoutes(level,source).get(side.getOpposite());
  if(receiver instanceof AtlantisDHDEntity dhd)return dhd.getEnergyHandler(Direction.DOWN);
  if(receiver instanceof AtlantisHatchTableBlockEntity table)return table.getEnergyHandler(Direction.DOWN);
  return null;
 }
 public static Map<Direction,BlockEntity> outputRoutes(Level level,BlockPos cablePos){
  if(!level.hasChunkAt(cablePos))return Map.of();
  BlockEntity cable=level.getBlockEntity(cablePos);
  if(!(cable instanceof CableBlockEntity.LargeNaquadahCable))return Map.of();
  long now=level.getGameTime();Routes cached=CACHE.get(cable);
  if(cached==null||now<cached.tick()||now-cached.tick()>=20){
   List<BlockEntity> receivers=ConcealedCableRouting.findReceivers(cablePos,1024,p->isLargeCable(level,p),p->{
    List<BlockPos> adjacent=new ArrayList<>(6);for(Direction d:Direction.values())adjacent.add(p.relative(d));return adjacent;
   },p->receiversAbove(level,p));
   receivers.sort(Comparator.comparingInt(r->r instanceof AtlantisDHDEntity?0:1));
   List<Direction> free=new ArrayList<>(6);
   for(Direction d:Direction.values()){
    BlockPos next=cablePos.relative(d);if(!level.hasChunkAt(next))continue;
    BlockEntity direct=level.getBlockEntity(next);
    if(direct==null&&!isLargeCable(level,next))free.add(d);else receivers.remove(direct);
   }
   Map<Direction,WeakReference<BlockEntity>> refs=new EnumMap<>(Direction.class);
   ConcealedCableRouting.assign(free,receivers).forEach((d,r)->refs.put(d,new WeakReference<>(r)));
   cached=new Routes(now,refs);CACHE.put(cable,cached);
  }
  Map<Direction,BlockEntity> result=new EnumMap<>(Direction.class);
  cached.receivers().forEach((d,ref)->{BlockEntity r=ref.get();if(r!=null&&!r.isRemoved()&&level.hasChunkAt(r.getBlockPos())&&level.getBlockEntity(r.getBlockPos())==r)result.put(d,r);});
  return result;
 }
 private static List<BlockEntity> receiversAbove(Level level,BlockPos cablePos){
  List<BlockEntity> found=new ArrayList<>();
  for(int depth=AtlantisHatchCableBootstrapEvents.MIN_CABLE_DEPTH;depth<=AtlantisHatchCableBootstrapEvents.MAX_CABLE_DEPTH;depth++){
   for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
    BlockPos p=cablePos.offset(dx,depth,dz);if(!level.hasChunkAt(p))continue;
    if(level.getBlockEntity(p) instanceof AtlantisHatchTableBlockEntity table&&AtlantisHatchCableBootstrapEvents.isBelowFootprint(table,cablePos))found.add(table);
   }
  }
  BlockPos footprint=cablePos.above(2);
  if(level.hasChunkAt(footprint)&&level.getBlockEntity(footprint) instanceof AtlantisDHDEntity dhd)found.add(dhd);
  for(Direction d:Direction.Plane.HORIZONTAL){BlockPos p=footprint.relative(d);if(!level.hasChunkAt(p))continue;
   if(level.getBlockEntity(p) instanceof AtlantisDHDEntity dhd){Direction facing=dhd.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);if(dhd.getBlockPos().relative(facing.getClockWise()).equals(footprint))found.add(dhd);}
  }
  return found;
 }
 private static boolean isLargeCable(Level level,BlockPos p){return level.hasChunkAt(p)&&level.getBlockState(p).getBlock() instanceof CableBlock.LargeNaquadahCable;}
}
