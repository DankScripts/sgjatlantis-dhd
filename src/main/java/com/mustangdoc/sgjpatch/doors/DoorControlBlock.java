package com.mustangdoc.sgjpatch.doors;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class DoorControlBlock extends HorizontalDirectionalBlock {
 private static final MapCodec<DoorControlBlock> CODEC=simpleCodec(p->new DoorControlBlock());
 @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
 public static final BooleanProperty CRYSTAL=BooleanProperty.create("crystal");
 public DoorControlBlock(){super(Properties.of().strength(2).sound(SoundType.METAL).noOcclusion().lightLevel(s->s.getValue(CRYSTAL)?5:0));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(CRYSTAL,true));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,CRYSTAL);}
 public static boolean rightJamb(BlockState s,Direction face){
  if(!s.is(AtlantisDoorRegistry.BLOCK)||s.getValue(AtlantisDoorBlock.ROW)!=1)return false;
  Direction door=s.getValue(AtlantisDoorBlock.FACING);int col=s.getValue(AtlantisDoorBlock.COL);
  return face==door&&col==0||face==door.getOpposite()&&col==(s.getValue(AtlantisDoorBlock.STYLE)==2?4:s.getValue(AtlantisDoorBlock.ALIGNED)?3:4);
 }
 public boolean canSurvive(BlockState s,LevelReader l,BlockPos p){Direction f=s.getValue(FACING);return rightJamb(l.getBlockState(p.relative(f.getOpposite())),f);}
 public BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){if(!canSurvive(s,l,p)){return Blocks.AIR.defaultBlockState();}return s;}
 public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING)){case NORTH->Shapes.box(.3,.05,.76,.7,.95,1);case SOUTH->Shapes.box(.3,.05,0,.7,.95,.24);case EAST->Shapes.box(0,.05,.3,.24,.95,.7);default->Shapes.box(.76,.05,.3,1,.95,.7);};}
 @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
  if(!s.getValue(CRYSTAL)&&held.is(AtlantisDoorRegistry.CRYSTAL)){
   if(!l.isClientSide){
    l.setBlock(p,s.setValue(CRYSTAL,true),3);
    if(!player.getAbilities().instabuild)held.shrink(1);
    player.displayClientMessage(Component.literal("Door control crystal installed."),true);
   }
   return ItemInteractionResult.SUCCESS;
  }
  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
 }
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
  if(l.isClientSide)return InteractionResult.SUCCESS;
  boolean crystal=s.getValue(CRYSTAL);
  if(crystal&&player.isShiftKeyDown()&&player.getMainHandItem().isEmpty()){
   l.setBlock(p,s.setValue(CRYSTAL,false),3);
   ItemStack removed=new ItemStack(AtlantisDoorRegistry.CRYSTAL);
   if(!player.addItem(removed))player.drop(removed,false);
   player.displayClientMessage(Component.literal("Door control crystal removed."),true);
   return InteractionResult.CONSUME;
  }
  if(!crystal){
   player.displayClientMessage(Component.literal("Inoperable: insert an Atlantis Door Control Crystal."),true);
   return InteractionResult.CONSUME;
  }
  BlockPos support=p.relative(s.getValue(FACING).getOpposite());
  BlockState frame=l.getBlockState(support);
  if(rightJamb(frame,s.getValue(FACING))&&l.getBlockEntity(AtlantisDoorBlock.root(support,frame)) instanceof AtlantisDoorEntity door){
   door.toggle();
  }
  return InteractionResult.CONSUME;
 }
 public ItemStack getCloneItemStack(BlockGetter l,BlockPos p,BlockState s){return new ItemStack(s.getValue(CRYSTAL)?AtlantisDoorRegistry.CONTROL:AtlantisDoorRegistry.DAMAGED);}
}

