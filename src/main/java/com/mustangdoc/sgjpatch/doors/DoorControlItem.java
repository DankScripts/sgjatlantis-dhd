package com.mustangdoc.sgjpatch.doors;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class DoorControlItem extends BlockItem {
 private final boolean working;
 public DoorControlItem(boolean working){super(AtlantisDoorRegistry.PANEL,new Properties());this.working=working;}
 public String getDescriptionId(){return "item.sgjadditions_capacity_patch.atlantis_door_control"+(working?"":"_damaged");}
 protected BlockState getPlacementState(BlockPlaceContext c){Direction f=c.getClickedFace();if(!f.getAxis().isHorizontal())return null;BlockState s=AtlantisDoorRegistry.PANEL.defaultBlockState().setValue(DoorControlBlock.FACING,f).setValue(DoorControlBlock.CRYSTAL,working);return s.canSurvive(c.getLevel(),c.getClickedPos())?s:null;}
 public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag f){text.add(Component.literal("Mount on the right jamb, one block above the doorway floor."));text.add(Component.literal("Sneak + empty-hand click removes the upper control crystal."));text.add(Component.literal("Insert a Door Control Crystal to restore operation."));}
}

