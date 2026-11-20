package com.mustangdoc.sgjpatch.doors;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import com.mustangdoc.sgjpatch.transporter.TransporterRoom;
public final class AtlantisDoorBlock extends HorizontalDirectionalBlock implements EntityBlock {
 public static final IntegerProperty COL=IntegerProperty.create("column",0,4),ROW=IntegerProperty.create("row",0,3),STAGE=IntegerProperty.create("stage",0,8);
 public static final IntegerProperty STYLE=IntegerProperty.create("style",0,2);
 public static final BooleanProperty SOLID=BooleanProperty.create("solid"),ALIGNED=BooleanProperty.create("aligned");
 private static final ThreadLocal<Boolean> REMOVING=ThreadLocal.withInitial(()->false);
 public AtlantisDoorBlock(){super(Properties.of().strength(4,20).sound(SoundType.METAL).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(COL,2).setValue(ROW,0).setValue(STAGE,0).setValue(SOLID,false).setValue(ALIGNED,false).setValue(STYLE,0).setValue(AtlantisFinish.PROPERTY,AtlantisFinish.LEGACY));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,COL,ROW,STAGE,SOLID,ALIGNED,STYLE,AtlantisFinish.PROPERTY);}
 public static BlockPos at(BlockPos root,Direction f,int col,int row){return root.relative(f.getClockWise(),col-2).above(row);}
 public static BlockPos root(BlockPos p,BlockState s){return p.relative(s.getValue(FACING).getClockWise(),2-s.getValue(COL)).below(s.getValue(ROW));}
 public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(COL)==2&&s.getValue(ROW)==0?new AtlantisDoorEntity(p,s):null;}
 public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide&&t==AtlantisDoorRegistry.ENTITY?(world,p,state,e)->AtlantisDoorEntity.tick((ServerLevel)world,(AtlantisDoorEntity)e):null;}
 public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
  int col=s.getValue(COL),row=s.getValue(ROW);VoxelShape shape=Shapes.empty();
  if(s.getValue(STYLE)==2){
   if(col==0||col==4||row==3)shape=Shapes.block();
   else for(double[] r:AtlantisDoorGeometry.wideRanges(s.getValue(STAGE),col))shape=Shapes.or(shape,Shapes.box(r[0],0,.35,r[1],1,.65));
  }
  else if(s.getValue(ALIGNED)){
   if(col==0||col>=3)shape=Shapes.block();
   else {
    if(row==3){shape=Shapes.box(0,.5,0,1,1,1);shape=Shapes.or(shape,col==1?Shapes.box(0,0,0,.5,.5,1):Shapes.box(.5,0,0,1,.5,1));}
    for(double[] r:AtlantisDoorGeometry.ranges(s.getValue(STAGE),col,row==3))shape=Shapes.or(shape,Shapes.box(r[0],0,.35,r[1],row==3?.5:1,.65));
   }
  }
  else if(col==0||col==4||row==3&&col!=2)shape=Shapes.block();
  else if(row==3){shape=Shapes.box(0,.5,0,1,1,1);for(double[] r:TransporterRoom.doorRanges(s.getValue(STAGE),1))shape=Shapes.or(shape,Shapes.box(r[0],0,.35,r[1],.5,.65));}
  else {if(col==1)shape=Shapes.box(0,0,0,.5,1,1);if(col==3)shape=Shapes.box(.5,0,0,1,1,1);for(double[] r:TransporterRoom.doorRanges(s.getValue(STAGE),col-1))shape=Shapes.or(shape,Shapes.box(r[0],0,.35,r[1],1,.65));}
  int rotations=(s.getValue(FACING).get2DDataValue()+2)%4;
  for(int i=0;i<rotations;i++){VoxelShape[] out={Shapes.empty()};shape.forAllBoxes((a,b,d,e,g,h)->out[0]=Shapes.or(out[0],Shapes.box(1-h,b,a,1-d,g,e)));shape=out[0];}return shape;
 }
 public VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
 public ItemStack getCloneItemStack(BlockGetter l,BlockPos p,BlockState s){return new ItemStack(AtlantisDoorRegistry.kit(s.getValue(SOLID),s.getValue(STYLE),s.getValue(AtlantisFinish.PROPERTY)));}
 @Override public void playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide&&!player.getAbilities().instabuild)popResource(l,p,getCloneItemStack(l,p,s));super.playerWillDestroy(l,p,s,player);}
 public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
  super.onRemove(s,l,p,next,moving);
  if(next.is(this)||l.isClientSide||REMOVING.get())return;
  REMOVING.set(true);try{BlockPos a=root(p,s);Direction f=s.getValue(FACING);for(int col=0;col<5;col++)for(int row=0;row<4;row++){BlockPos q=at(a,f,col,row);BlockState old=l.getBlockState(q);if(old.is(this)&&root(q,old).equals(a))l.removeBlock(q,false);}}finally{REMOVING.set(false);}
 }
}

