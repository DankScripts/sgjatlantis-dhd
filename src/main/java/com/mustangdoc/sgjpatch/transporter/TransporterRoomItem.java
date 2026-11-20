package com.mustangdoc.sgjpatch.transporter;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class TransporterRoomItem extends Item {
    private final AtlantisFinish finish;
    public TransporterRoomItem(){this(AtlantisFinish.LEGACY);}
    public TransporterRoomItem(AtlantisFinish finish){super(new Properties().stacksTo(1));this.finish=finish;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> text,TooltipFlag flag){
        text.add(Component.literal("Places a 5 × 5 × 4 Atlantis transporter cabin with a 3 × 3 × 2 interior."));
        text.add(Component.literal("Click the floor at the entrance centre. The room extends away from you."));
        text.add(Component.literal("Name in an anvil, or use /sgjatlantis transporter rename."));
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null||c.getClickedFace()!=Direction.UP)return InteractionResult.FAIL;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        ServerLevel l=(ServerLevel)c.getLevel();Direction f=c.getHorizontalDirection().getOpposite();BlockPos a=TransporterRoom.placementAnchor(c.getClickedPos(),f);
        for(BlockPos p:TransporterRoom.volume(a,f)){
            // The supporting floor is part of the prefab; never overwrite machines or inventories.
            boolean floor=p.getY()==a.getY()-1;
            if(!l.isInWorldBounds(p)||!l.getWorldBorder().isWithinBounds(p)||!c.getPlayer().mayUseItemAt(p,Direction.UP,c.getItemInHand())
                ||!l.mayInteract(c.getPlayer(),p)||l.getBlockEntity(p)!=null||l.getBlockState(p).getDestroySpeed(l,p)<0
                ||(!floor&&!l.getBlockState(p).canBeReplaced()&&!TransporterRoom.orphanedPart(l,p))){
                c.getPlayer().displayClientMessage(Component.literal("Placement blocked by "+l.getBlockState(p).getBlock().getName().getString()+" at "+p.getX()+", "+p.getY()+", "+p.getZ()+". Clear a 5 x 4 area behind the entrance with four blocks of headroom."),true);return InteractionResult.FAIL;
            }
        }
        // Check the material that will actually be placed, not the empty passenger space.
        for(var entry:TransporterRoom.shell(a,f).entrySet()){
            BlockPos p=entry.getKey();var state=entry.getValue().setValue(TransporterBlock.STAGE,0);
            for(var box:state.getCollisionShape(l,p).toAabbs()){
                if(!l.getEntities((net.minecraft.world.entity.Entity)null,box.move(p),e->e.isAlive()&&!e.isSpectator()).isEmpty()){
                    c.getPlayer().displayClientMessage(Component.literal("A wall or doorway would overlap an entity at "+p.getX()+", "+p.getY()+", "+p.getZ()+". Step back from the entrance."),true);return InteractionResult.FAIL;
                }
            }
        }
        String name=c.getItemInHand().hasCustomHoverName()?c.getItemInHand().getHoverName().getString():"Transporter "+a.getX()+", "+a.getY()+", "+a.getZ();
        var snapshots=TransporterRoom.volume(a,f).stream().map(p->net.minecraftforge.common.util.BlockSnapshot.create(l.dimension(),l,p)).toList();
        if(net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(c.getPlayer(),snapshots,Direction.UP))return InteractionResult.FAIL;
        TransporterRoom.build(l,a,f,name,finish);
        if(!c.getPlayer().getAbilities().instabuild)c.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}


