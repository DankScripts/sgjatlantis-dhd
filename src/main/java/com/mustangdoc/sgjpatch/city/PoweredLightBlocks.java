package com.mustangdoc.sgjpatch.city;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Powered copies retain the original geometry and placement rules. */
public final class PoweredLightBlocks {
 private PoweredLightBlocks() {}
 public interface Powered extends EntityBlock {
  default BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PoweredLightBlockEntity(pos,state);}
  default <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
   if(level.isClientSide || type!=PoweredLightBlockEntity.TYPE)return null;
   return (world,pos,s,be)->PoweredLightBlockEntity.tick(world,pos,s,(PoweredLightBlockEntity)be);
  }
 }
 public static final class Panel extends SeamPanelBlock implements Powered {
  public Panel(BlockBehaviour.Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.LIT);}
 }
 public static final class Facing extends AtlantisBuildingBlocks.FacingBlock implements Powered {
  public Facing(BlockBehaviour.Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.LIT);}
 }
 public static final class Pillar extends AtlantisPillarBlock implements Powered {
  public Pillar(BlockBehaviour.Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.LIT);}
 }
 public static final class Wall extends AtlantisWallLight implements Powered {
  public Wall(BlockBehaviour.Properties p){super(p,false);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.LIT);}
 }
 public static final class WideWall extends AtlantisWallLight implements Powered {
  public WideWall(BlockBehaviour.Properties p){super(p,true);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.LIT);}
 }
 public static final class Stairs extends AtlantisGlowStairs implements Powered {
  public Stairs(BlockBehaviour.Properties p){super(Blocks.STONE.defaultBlockState(),p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
 }
 public static final class OpenStairs extends AtlantisOpenStairs implements Powered {
  public OpenStairs(BlockBehaviour.Properties p){super(Blocks.STONE.defaultBlockState(),p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
 }
 public static final class Slab extends AtlantisGlowSlab implements Powered {
  public Slab(BlockBehaviour.Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT,false));}
 }
}

