package com.mustangdoc.sgjpatch.power;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.capabilities.ZeroPointEnergy;

/** Full solid floor; energy is forwarded, never stored, generated or converted here. */
public final class FloorConduitBlockEntity extends BlockEntity {
 public final SGJourneyEnergy energy=new SGJourneyEnergy(Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE) {
  public int receiveEnergy(int amount,boolean simulate){return (int)route(amount,simulate,hasLanteaHubAbove(),0);}
  public long receiveLongEnergy(long amount,boolean simulate){return route(amount,simulate,false,0);}
  public long receiveZeroPointEnergy(long amount,boolean simulate){return route(amount,simulate,true,0);}
  public long extractLongEnergy(long amount,boolean simulate){return 0;}
  public int extractEnergy(int amount,boolean simulate){return 0;}
  public boolean canExtract(){return false;}
  public long getTrueEnergyStored(){return 0;}
  public boolean hasEnergy(){return false;}
  public boolean hasEnergy(long amount){return false;}
  public void onEnergyChanged(long amount,boolean simulate){}
 };
 public FloorConduitBlockEntity(BlockPos pos,BlockState state){super(FloorConduitRegistration.TYPE,pos,state);}
 private static final Map<net.minecraft.world.level.Level,Long> TOPOLOGY_VERSIONS=new WeakHashMap<>();
 private static long topologyVersion(net.minecraft.world.level.Level level){return TOPOLOGY_VERSIONS.getOrDefault(level,0L);}
 private static void topologyChanged(net.minecraft.world.level.Level level){if(level!=null && !level.isClientSide)TOPOLOGY_VERSIONS.put(level,topologyVersion(level)+1);}
 static void refreshTopology(net.minecraft.world.level.Level level){topologyChanged(level);}
 private Network network;
 private static final class Network {
  final Set<FloorConduitBlockEntity> members=Collections.newSetFromMap(new IdentityHashMap<>());
  final List<Endpoint> endpoints=new ArrayList<>();
  boolean routing;
  long generation;
  long targetsTick=Long.MIN_VALUE;
  final List<IEnergyStorage> targets=new ArrayList<>();
 }
 private Network getNetwork(){
  if(network!=null && network.generation==topologyVersion(level))return network;
  Network built=new Network();
  built.generation=topologyVersion(level);
  Set<BlockPos> visited=new HashSet<>();
  ArrayDeque<BlockPos> queue=new ArrayDeque<>();
  visited.add(worldPosition);queue.add(worldPosition);
  while(!queue.isEmpty()){
   BlockPos at=queue.remove();
   if(!level.hasChunkAt(at) || !(level.getBlockEntity(at) instanceof FloorConduitBlockEntity member))continue;
   built.members.add(member);
   for(Direction side:Direction.values()){
    BlockPos next=at.relative(side);
    if(!level.hasChunkAt(next))continue;
    if(level.getBlockEntity(next) instanceof FloorConduitBlockEntity){if(visited.add(next))queue.add(next);}
    else built.endpoints.add(new Endpoint(next,side.getOpposite()));
   }
  }
  for(FloorConduitBlockEntity member:built.members)member.network=built;
  return built;
 }
 public Set<BlockPos> connectedLargeCablePositions(){
  Set<BlockPos> cables=new LinkedHashSet<>();
  if(level==null || level.isClientSide)return cables;
  for(Endpoint endpoint:getNetwork().endpoints){
   BlockPos pos=endpoint.pos();
   if(level.hasChunkAt(pos) && level.getBlockState(pos).getBlock()
           instanceof net.povstalec.sgjourney.common.blocks.tech.CableBlock.LargeNaquadahCable)cables.add(pos);
  }
  return cables;
 }
 private boolean hasLanteaHubAbove(){
  if(level==null)return false;
  BlockEntity above=level.getBlockEntity(worldPosition.above());
  if(above==null)return false;
  var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(above.getBlockState().getBlock());
  return id.getNamespace().equals("lantea") && id.getPath().equals("zpm_hub_base");
 }
 /** Returns the charge of connected ZPM hubs on this floor network in FE. */
 public long[] connectedHubCharge(){
  if(level==null)return new long[]{0,0};
  Set<BlockEntity> lanteaHubs=Collections.newSetFromMap(new IdentityHashMap<>());
  Set<BlockEntity> zpmHubs=Collections.newSetFromMap(new IdentityHashMap<>());
  for(Endpoint endpoint:getNetwork().endpoints){
    BlockPos next=endpoint.pos();
    BlockEntity neighbor=level.getBlockEntity(next);
    if(neighbor!=null){
     var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(neighbor.getBlockState().getBlock());
     if(id.getNamespace().equals("lantea") && id.getPath().equals("zpm_hub_base")){
      try{
       Object hub=neighbor.getClass().getMethod("hub").invoke(neighbor);
       if(hub instanceof BlockEntity entity)lanteaHubs.add(entity);
      }catch(ReflectiveOperationException ignored){}
     }else if(id.getPath().contains("zpm_hub"))zpmHubs.add(neighbor);

     // Follow an attached SGJourney Large Naquadah Cable network all the way
     // to its hubs. This makes conduit -> cable -> conduit combinations just
     // as inspectable as a hub placed directly on an Atlantis conduit.
     if(neighbor instanceof net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity cable
             && cable.canTransferZeroPointEnergy()){
      for(var networkCable:net.povstalec.sgjourney.common.data.ConduitNetworks.findConnectedCables(level,next)){
       if(!networkCable.canTransferZeroPointEnergy())continue;
       BlockEntity hub=level.getBlockEntity(networkCable.getBlockPos().above());
       if(hub==null)continue;
       var hubId=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(hub.getBlockState().getBlock());
       if(hubId.getPath().contains("zpm_hub"))zpmHubs.add(hub);
      }
     }
    }
  }
  int fePerEu=4;
  try{fePerEu=((Number)Class.forName("io.github.aqueoxx.lantea.energy.GateCableInput").getMethod("fePerEu").invoke(null)).intValue();}
  catch(ReflectiveOperationException ignored){}
  long stored=0,capacity=0;
  for(BlockEntity hub:lanteaHubs)try{
   long charge=((Number)hub.getClass().getMethod("stored").invoke(hub)).longValue();
   long maximum=((Number)hub.getClass().getMethod("capacity").invoke(hub)).longValue();
   stored=saturatedAdd(stored,saturatedMultiply(Math.max(0,charge),Math.max(1,fePerEu)));
   capacity=saturatedAdd(capacity,saturatedMultiply(Math.max(0,maximum),Math.max(1,fePerEu)));
  }catch(ReflectiveOperationException ignored){}
  for(BlockEntity hub:zpmHubs){
   if(hub instanceof net.povstalec.sgjourney.common.block_entities.zpm.ZPMHubEntity original){
    long[] charge=zpmCharge(original.zpmEnergy);
    stored=saturatedAdd(stored,charge[0]);capacity=saturatedAdd(capacity,charge[1]);
    continue;
   }
   net.minecraftforge.items.IItemHandler inventory=hub.getCapability(
           net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElse(null);
   if(inventory==null)try{
    Object exposed=hub.getClass().getField("inventory").get(hub);
    if(exposed instanceof net.minecraftforge.items.IItemHandler handler)inventory=handler;
   }catch(ReflectiveOperationException ignored){}
   if(inventory!=null){
    for(int slot=0;slot<inventory.getSlots();slot++){
     var stack=inventory.getStackInSlot(slot);
     if(!stack.is(net.povstalec.sgjourney.common.init.ItemInit.ZPM.get()))continue;
     ZeroPointEnergy zpm=new ZeroPointEnergy(stack,
             net.povstalec.sgjourney.common.config.CommonZPMConfig.zpm_hub_max_transfer.get()){
      @Override public void onEnergyChanged(long amount,boolean simulate){}
     };
     long[] charge=zpmCharge(zpm);
     stored=saturatedAdd(stored,charge[0]);capacity=saturatedAdd(capacity,charge[1]);
    }
   }else{
    var power=hub.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY,Direction.DOWN).orElse(null);
    if(power instanceof SGJourneyEnergy energy){
     stored=saturatedAdd(stored,Math.max(0,energy.getTrueEnergyStored()));
     capacity=saturatedAdd(capacity,Math.max(0,energy.getTrueMaxEnergyStored()));
    }
   }
  }
  return new long[]{stored,capacity};
 }
 private static long[] zpmCharge(ZeroPointEnergy zpm){
  long segment=Math.max(0,zpm.getTrueEnergyStored());
  long perLevel=Math.max(0,zpm.getTrueMaxEnergyStored());
  long remaining=saturatedAdd(segment,saturatedMultiply(perLevel,Math.max(0,999-zpm.getEntropy())));
  return new long[]{remaining,saturatedMultiply(perLevel,1000)};
 }
 private static long saturatedAdd(long a,long b){return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
 private static long saturatedMultiply(long a,long b){if(a<=0||b<=0)return 0;return a>Long.MAX_VALUE/b?Long.MAX_VALUE:a*b;}
 @Override public void onLoad(){
  super.onLoad();
  topologyChanged(level);
  CableNetworkRefresh.register(this);
 }
 @Override public void setRemoved(){topologyChanged(level);super.setRemoved();}
 private long route(long amount,boolean simulate,boolean zeroPoint,int operation){
  if(amount<=0 || level==null || level.isClientSide)return 0;
  Network network=getNetwork();
  return MixedPowerTransfer.run(network,amount,simulate,zeroPoint,
    ()->routeNetwork(network,amount,simulate,zeroPoint));
 }
 private long routeNetwork(Network network,long amount,boolean simulate,boolean zeroPoint){
  if(network.routing)return 0;
  network.routing=true;
  
  try{
   long left=amount;
   // Resolve perimeter capabilities once per network/tick, rather than once
   // for every source, face, simulation and committed transfer.
   if(network.targetsTick!=level.getGameTime()){
    network.targets.clear();
    Set<IEnergyStorage> offered=Collections.newSetFromMap(new IdentityHashMap<>());
    for(Endpoint endpoint:network.endpoints){
     IEnergyStorage target=lookup(endpoint);
     if(target!=null && offered.add(target))network.targets.add(target);
    }
    network.targetsTick=level.getGameTime();
   }
   for(IEnergyStorage target:network.targets){
    if(!target.canReceive())continue;
    long accepted;
    if(target instanceof SGJourneyEnergy sg)accepted=zeroPoint?sg.receiveZeroPointEnergy(left,simulate):sg.receiveLongEnergy(left,simulate);
    else accepted=target.receiveEnergy((int)Math.min(Integer.MAX_VALUE,left),simulate);
    left-=Math.max(0,Math.min(left,accepted));
    if(left==0)break;
   }
   return amount-left;
  }finally{network.routing=false;}
 }
 private record Endpoint(BlockPos pos,Direction side){}
  private IEnergyStorage lookup(Endpoint endpoint){if(!level.hasChunkAt(endpoint.pos()))return null;BlockEntity be=level.getBlockEntity(endpoint.pos());return be==null?null:be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY,endpoint.side()).orElse(null);}
 private net.minecraftforge.common.util.LazyOptional<IEnergyStorage> energyCap=net.minecraftforge.common.util.LazyOptional.of(()->energy);
 @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,Direction side){return cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY?energyCap.cast():super.getCapability(cap,side);}
 @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();}
 @Override public void reviveCaps(){super.reviveCaps();energyCap=net.minecraftforge.common.util.LazyOptional.of(()->energy);}
}
