package com.mustangdoc.sgjpatch.doors;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public final class AtlantisDoorItem extends Item {
 private final boolean solid;private final int style;private final AtlantisFinish finish;
 public AtlantisDoorItem(boolean solid){this(solid,0);}
 public AtlantisDoorItem(boolean solid,int style){this(solid,style,AtlantisFinish.LEGACY);}
 public AtlantisDoorItem(boolean solid,int style,AtlantisFinish finish){super(new Properties().stacksTo(16));this.solid=solid;this.style=style;this.finish=finish;}
 @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){lines.add(Component.literal(style==2?"Click the floor under the centre of the 3-wide opening.":"Click the floor under the left half of the opening, viewed from the front."));lines.add(Component.literal(style==2?"3 x 3 clear opening; 5 wide x 4 high x 1 deep with frame.":"Needs 4 blocks wide x 4 high x 1 deep, including frame."));lines.add(Component.literal("Mount a separate Door Control Panel on the right jamb on each side."));}
 @Override public InteractionResult useOn(UseOnContext c){
  if(c.getPlayer()==null||c.getClickedFace()!=Direction.UP)return InteractionResult.FAIL;
  Level l=c.getLevel();Direction f=c.getHorizontalDirection().getOpposite();BlockPos a=c.getClickedPos().above();List<BlockPos> positions=new ArrayList<>();
  for(int col=0;col<(style==2?5:4);col++)for(int row=0;row<4;row++){BlockPos p=AtlantisDoorBlock.at(a,f,col,row);if(!l.isInWorldBounds(p)||!l.getWorldBorder().isWithinBounds(p)||!l.getBlockState(p).canBeReplaced()||l.getBlockEntity(p)!=null||!l.mayInteract(c.getPlayer(),p)||!c.getPlayer().mayUseItemAt(p,Direction.UP,c.getItemInHand())){if(!l.isClientSide)c.getPlayer().displayClientMessage(Component.literal("Clear "+(style==2?5:4)+" blocks wide and 4 high for the door and frame."),true);return InteractionResult.FAIL;}positions.add(p);}
  for(BlockPos p:positions)if(!l.getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(p),e->e.isAlive()&&!e.isSpectator()).isEmpty()){if(!l.isClientSide)c.getPlayer().displayClientMessage(Component.literal("Clear the doorway before placing the kit."),true);return InteractionResult.FAIL;}
  if(l.isClientSide)return InteractionResult.SUCCESS;
  var snapshots=positions.stream().map(p->net.minecraftforge.common.util.BlockSnapshot.create(l.dimension(),l,p)).toList();if(net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(c.getPlayer(),snapshots,Direction.UP))return InteractionResult.FAIL;
  for(int col=0;col<(style==2?5:4);col++)for(int row=0;row<4;row++)l.setBlock(AtlantisDoorBlock.at(a,f,col,row),AtlantisDoorRegistry.BLOCK.defaultBlockState().setValue(AtlantisDoorBlock.FACING,f).setValue(AtlantisDoorBlock.COL,col).setValue(AtlantisDoorBlock.ROW,row).setValue(AtlantisDoorBlock.SOLID,solid).setValue(AtlantisDoorBlock.ALIGNED,true).setValue(AtlantisDoorBlock.STYLE,style).setValue(AtlantisFinish.PROPERTY,finish),3);
  if(!c.getPlayer().getAbilities().instabuild)c.getItemInHand().shrink(1);return InteractionResult.CONSUME;
 }
}


