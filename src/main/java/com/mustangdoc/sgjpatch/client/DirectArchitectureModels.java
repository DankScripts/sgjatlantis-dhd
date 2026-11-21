package com.mustangdoc.sgjpatch.client;

import java.util.*;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;
import com.mustangdoc.sgjpatch.doors.*;
import com.mustangdoc.sgjpatch.transporter.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="sgjatlantis_dhd",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class DirectArchitectureModels {
 private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("sgjadditions_capacity_patch","block/"+path);}
 private static Block[] blocks(){return new Block[]{AtlantisDoorRegistry.BLOCK,TransporterRegistry.BLOCK,TransporterRegistry.PANEL};}
 private static String model(BlockState s){
  String name;
  if(s.getBlock()==AtlantisDoorRegistry.BLOCK){
   int col=s.getValue(AtlantisDoorBlock.COL),row=s.getValue(AtlantisDoorBlock.ROW);
   boolean aligned=s.getValue(AtlantisDoorBlock.ALIGNED);
   if(s.getValue(AtlantisDoorBlock.STYLE)==2)name="atlantis_door_wide_"+col+"_"+row;
   else if(aligned && col==4)return "architecture_empty";
   else name="atlantis_door_"+(aligned?"aligned_":"frame_")+col+"_"+row;
  }else{
   var part=s.getValue(TransporterBlock.PART);
   if(s.getBlock()==TransporterRegistry.PANEL && part!=TransporterBlock.Part.PANEL)return "architecture_empty";
   name=(part==TransporterBlock.Part.DOOR || part==TransporterBlock.Part.LINING)
    ?"transporter_lining_"+s.getValue(TransporterBlock.COLUMN)+"_"+s.getValue(TransporterBlock.ROW)
    :"transporter_"+part.getSerializedName();
  }
  AtlantisFinish finish=s.getValue(AtlantisFinish.PROPERTY);
  return name+(finish==AtlantisFinish.LEGACY?"":"_"+finish.getSerializedName());
 }
 @SubscribeEvent public static void register(ModelEvent.RegisterAdditional event){
  Set<String> models=new HashSet<>();
  for(Block block:blocks())for(BlockState state:block.getStateDefinition().getPossibleStates())models.add(model(state));
  for(String name:models)event.register(ModelResourceLocation.standalone(id(name)));
 }
 @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event){
  Map<String,BakedModel> cache=new HashMap<>();
  for(Block block:blocks())for(BlockState state:block.getStateDefinition().getPossibleStates()){
   String name=model(state);
   Direction facing=state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
   int turns=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
   String key=name+":"+turns;
   BakedModel baked=cache.computeIfAbsent(key,ignored->{
    BakedModel original=event.getModels().get(ModelResourceLocation.standalone(id(name)));
    if(original==null)throw new IllegalStateException("Missing Atlantis architecture model: "+name);
    return turns==0?original:new Rotated(original,turns);
   });
   event.getModels().put(BlockModelShaper.stateToModelLocation(state),baked);
  }
 }
 /** Rotate each static mesh once, then share it across animation and solid states. */
 private static final class Rotated extends BakedModelWrapper<BakedModel> {
  private final List<BakedQuad>[] faces;
  @SuppressWarnings("unchecked") Rotated(BakedModel original,int turns){
   super(original);faces=(List<BakedQuad>[])new List<?>[7];
   for(int i=0;i<7;i++){
    Direction source=i==6?null:Direction.values()[i];
    List<BakedQuad> quads=new ArrayList<>();
    for(BakedQuad q:original.getQuads(null,source,RandomSource.create(0))){
     int[] v=q.getVertices().clone();int stride=v.length/4;
     for(int vertex=0;vertex<4;vertex++){
      int offset=vertex*stride;
      float x=Float.intBitsToFloat(v[offset]),z=Float.intBitsToFloat(v[offset+2]);
      for(int t=0;t<turns;t++){float old=x;x=1-z;z=old;}
      v[offset]=Float.floatToRawIntBits(x);v[offset+2]=Float.floatToRawIntBits(z);
      int normal=v[offset+7],nx=(byte)normal,nz=(byte)(normal>>16);
      for(int t=0;t<turns;t++){int old=nx;nx=-nz;nz=old;}
      v[offset+7]=(normal&0xff00ff00)|(nx&255)|((nz&255)<<16);
     }
     quads.add(new BakedQuad(v,q.getTintIndex(),rotate(q.getDirection(),turns),q.getSprite(),q.isShade()));
    }
    faces[source==null?6:rotate(source,turns).ordinal()]=List.copyOf(quads);
   }
  }
  private static Direction rotate(Direction direction,int turns){if(direction.getAxis().isHorizontal())for(int i=0;i<turns;i++)direction=direction.getClockWise();return direction;}
  @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return faces[side==null?6:side.ordinal()];}
  @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType layer){return getQuads(state,side,random);}
 }
}

