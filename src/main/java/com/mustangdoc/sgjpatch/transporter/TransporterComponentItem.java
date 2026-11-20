package com.mustangdoc.sgjpatch.transporter;

import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Placeable replacement parts; the complete cabin recipe includes both controls and map. */
public final class TransporterComponentItem extends BlockItem {
    private final TransporterBlock.Part part;
    public TransporterComponentItem(TransporterBlock.Part part){super(TransporterRegistry.BLOCK,new Item.Properties());this.part=part;}
    @Override protected BlockState getPlacementState(BlockPlaceContext c){
        Direction f=c.getClickedFace();
        if(!f.getAxis().isHorizontal())return null;
        return TransporterRegistry.BLOCK.defaultBlockState().setValue(TransporterBlock.FACING,f).setValue(TransporterBlock.PART,part).setValue(TransporterBlock.ROW,part==TransporterBlock.Part.MAP?1:0);
    }
}

