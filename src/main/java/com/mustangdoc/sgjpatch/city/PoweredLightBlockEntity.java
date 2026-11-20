package com.mustangdoc.sgjpatch.city;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
@Mod.EventBusSubscriber(modid="sgjadditions_capacity_patch",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class PoweredLightBlockEntity extends BlockEntity {
 private static final int FIXTURES_PER_POWER_UNIT=256;
 public static BlockEntityType<PoweredLightBlockEntity> TYPE;
 private int charge;
 private boolean refreshLightOnTick;
 private long lastTick=Long.MIN_VALUE;
 private static final Map<Level,Long> TOPOLOGY=new WeakHashMap<>();
 private LightNetwork cachedNetwork;
 private static final class LightNetwork {
  final List<PoweredLightBlockEntity> members=new ArrayList<>();
  long generation,stored;
  int cursor;
 }
 private static void topologyChanged(Level level){if(level!=null)TOPOLOGY.put(level,TOPOLOGY.getOrDefault(level,0L)+1);}
 public PoweredLightBlockEntity(BlockPos p,BlockState s){super(TYPE,p,s);}
 // Two seconds per fixture: a short buffer, never a permanent powered state.
 public final SGJourneyEnergy energy=new SGJourneyEnergy(2,2,0){
  @Override public long receiveLongEnergy(long amount,boolean simulate){
   if(amount<=0 || level==null)return 0;
   LightNetwork group=network();
   long accepted=Math.min(amount,Math.max(0L,networkCapacity(group.members.size())-group.stored));
   if(simulate || accepted<=0)return accepted;
   long left=accepted;
   while(left>0){
    PoweredLightBlockEntity light=group.members.get(group.cursor);
    int fill=(int)Math.min(left,2-light.charge);
    if(fill>0){light.charge+=fill;light.setChanged();left-=fill;}
    group.cursor=(group.cursor+1)%group.members.size();
   }
   group.stored+=accepted;
   return accepted;
  }
  @Override public long receiveZeroPointEnergy(long amount,boolean simulate){return receiveLongEnergy(amount,simulate);}
  @Override public long getTrueEnergyStored(){return network().stored;}
  @Override public void onEnergyChanged(long amount,boolean simulate){}
 };
 private static long billingUnits(int fixtures){return Math.max(1L,(fixtures+FIXTURES_PER_POWER_UNIT-1L)/FIXTURES_PER_POWER_UNIT);}
 private static long networkCapacity(int fixtures){return 2L*billingUnits(fixtures);}
 private LightNetwork network(){
  long generation=TOPOLOGY.getOrDefault(level,0L);
  if(cachedNetwork!=null && cachedNetwork.generation==generation)return cachedNetwork;
  LightNetwork found=new LightNetwork();found.generation=generation;
  if(level==null||isRemoved())return found;
  Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(worldPosition);seen.add(worldPosition);
  while(!queue.isEmpty()){
   BlockPos p=queue.remove();if(!level.hasChunkAt(p))continue;
   if(!(level.getBlockEntity(p) instanceof PoweredLightBlockEntity light)||light.isRemoved())continue;
   found.members.add(light);found.stored+=light.charge;
   for(Direction d:Direction.values()){BlockPos next=p.relative(d);if(seen.add(next))queue.add(next);}
  }
  for(var light:found.members)light.cachedNetwork=found;
  return found;
 }
 public static void tick(Level world,BlockPos pos,BlockState state,PoweredLightBlockEntity self){
  // Reconcile after chunk attachment, before the once-per-second power check.
  if(self.refreshLightOnTick){
   self.refreshLightOnTick=false;
   BlockState current=world.getBlockState(pos);
   BlockState aligned=SeamPanelBlock.aligned(current,pos);
   if(aligned!=current)world.setBlock(pos,aligned,3);
   world.getChunkSource().getLightEngine().checkBlock(pos);
   world.updateNeighborsAt(pos,aligned.getBlock());
  }
  long now=world.getGameTime();if(now%20!=0||self.lastTick==now)return;
  LightNetwork network=self.network();
  List<PoweredLightBlockEntity> group=network.members;long available=0;
  for(var light:group){light.lastTick=now;available+=light.charge;}
  // Pull from passive FE batteries as well as accepting pushes from cables/hubs.
  Set<Object> sources=Collections.newSetFromMap(new IdentityHashMap<>());
  for(var light:group)for(Direction side:Direction.values()){
   BlockPos next=light.worldPosition.relative(side);
   if(!world.hasChunkAt(next)||world.getBlockEntity(next) instanceof PoweredLightBlockEntity
      ||world.getBlockEntity(next) instanceof com.mustangdoc.sgjpatch.power.FloorConduitBlockEntity)continue;
   var source=(world.getBlockEntity(next)==null?null:world.getBlockEntity(next).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY,side.getOpposite()).orElse(null));
   if(source==null||!source.canExtract()||!sources.add(source))continue;
   int needed=(int)Math.min(Integer.MAX_VALUE,Math.max(0,networkCapacity(group.size())-available));
   int offer=source.extractEnergy(needed,true);
   int accepted=(int)self.energy.receiveLongEnergy(offer,true);
   if(accepted>0)available+=self.energy.receiveLongEnergy(source.extractEnergy(accepted,false),false);
  }
  int upkeep=(int)Math.min(Integer.MAX_VALUE,billingUnits(group.size()));
  boolean lit=available>=upkeep;int cost=lit?upkeep:0;
  for(var light:group){int used=Math.min(cost,light.charge);light.charge-=used;cost-=used;
   network.stored-=used;
   BlockState current=world.getBlockState(light.worldPosition);
   if(current.hasProperty(BlockStateProperties.LIT)&&current.getValue(BlockStateProperties.LIT)!=lit)
    world.setBlock(light.worldPosition,current.setValue(BlockStateProperties.LIT,lit),3);
  }
 }
 @Override public void setRemoved(){topologyChanged(level);cachedNetwork=null;super.setRemoved();}
 @Override public void onLoad(){
  super.onLoad();topologyChanged(level);cachedNetwork=null;
  if(level==null)return;
  // Keep the saved LIT state until the normal power check. Mutating it while
  // attaching a chunk can race the chunk's initial lighting data.
  if(level.isClientSide)level.getChunkSource().getLightEngine().checkBlock(worldPosition);
  else refreshLightOnTick=true;
 }
 @SubscribeEvent public static void register(RegisterEvent event){event.register(Registries.BLOCK_ENTITY_TYPE,h->{
  java.util.List<Block> blocks=new ArrayList<>();
  for(Block block:net.minecraft.core.registries.BuiltInRegistries.BLOCK)if(block instanceof PoweredLightBlocks.Powered)blocks.add(block);
  TYPE=BlockEntityType.Builder.of(PoweredLightBlockEntity::new,blocks.toArray(Block[]::new)).build(null);
  h.register(new ResourceLocation("sgjadditions_capacity_patch","powered_light"),TYPE);
 });}
 private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.energy.IEnergyStorage> cap=net.minecraftforge.common.util.LazyOptional.of(()->energy);
 @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,Direction side){if(capability==net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY)return cap.cast();return super.getCapability(capability,side);}
 @Override public void invalidateCaps(){super.invalidateCaps();cap.invalidate();}
 @Override public void reviveCaps(){super.reviveCaps();cap=net.minecraftforge.common.util.LazyOptional.of(()->energy);}
}


