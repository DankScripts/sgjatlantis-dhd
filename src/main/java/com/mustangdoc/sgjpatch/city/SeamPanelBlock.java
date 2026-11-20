package com.mustangdoc.sgjpatch.city;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
/** Alternate the original whole texture vertically; keep one full cube. */
public class SeamPanelBlock extends Block {
 public static final BooleanProperty FLIPPED=BooleanProperty.create("seam_flipped");
 public SeamPanelBlock(BlockBehaviour.Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FLIPPED,false));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FLIPPED);}
 public static BlockState aligned(BlockState s,BlockPos p){return s.hasProperty(FLIPPED)?s.setValue(FLIPPED,(p.getY()&1)!=0):s;}
 public BlockState getStateForPlacement(BlockPlaceContext c){return aligned(defaultBlockState(),c.getClickedPos());}
 public BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor world,BlockPos p,BlockPos other){return aligned(super.updateShape(s,d,neighbor,world,p,other),p);}
 public void onPlace(BlockState s,Level world,BlockPos p,BlockState old,boolean moving){super.onPlace(s,world,p,old,moving);BlockState a=aligned(s,p);if(a!=s&&!world.isClientSide)world.setBlock(p,a,2);}
}
