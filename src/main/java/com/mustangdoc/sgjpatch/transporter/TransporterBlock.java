package com.mustangdoc.sgjpatch.transporter;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public class TransporterBlock extends HorizontalDirectionalBlock {
    public enum Part implements StringRepresentable {
        WALL,FLOOR,CEILING,PANEL,DOOR,CONTROL,MAP,LINING,TOP,SWITCH,CROWN;
        public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final EnumProperty<Part> PART=EnumProperty.create("part",Part.class);
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,8);
    public static final IntegerProperty COLUMN=IntegerProperty.create("column",0,2),ROW=IntegerProperty.create("row",0,2);
    public TransporterBlock() {
        super(Properties.of().strength(4,20).sound(SoundType.METAL).noOcclusion()
            .lightLevel(s->s.getValue(PART)==Part.CEILING?12:s.getValue(PART)==Part.SWITCH?10:s.getValue(PART)==Part.PANEL?7:0));
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,Part.WALL)
            .setValue(STAGE,8).setValue(COLUMN,1).setValue(ROW,0).setValue(AtlantisFinish.PROPERTY,AtlantisFinish.LEGACY));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART,STAGE,COLUMN,ROW,AtlantisFinish.PROPERTY);}
    public static final class Controller extends TransporterBlock implements EntityBlock {
        public Controller(){super();registerDefaultState(defaultBlockState().setValue(PART,Part.PANEL));}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TransporterEntity(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
            return !l.isClientSide&&t==TransporterRegistry.ENTITY?(world,p,state,e)->TransporterEntity.tick((ServerLevel)world,(TransporterEntity)e):null;
        }
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        Part part=s.getValue(PART);
        if(part==Part.TOP)return Shapes.box(0,.5,0,1,1,1);
        if(part!=Part.DOOR&&part!=Part.LINING&&part!=Part.SWITCH&&part!=Part.CROWN)return Shapes.block();
        VoxelShape shape=Shapes.empty();
        int col=s.getValue(COLUMN),row=s.getValue(ROW);
        if(part==Part.CROWN){
            shape=Shapes.box(0,.5,0,1,1,1);
            for(double[] range:TransporterRoom.doorRanges(s.getValue(STAGE),1))shape=Shapes.or(shape,Shapes.box(range[0],0,.35,range[1],.5,.65));
        }
        else if(part==Part.SWITCH)shape=Shapes.box(.34,.06,.8,.66,.94,1);
        else {
            if(col==0)shape=Shapes.box(0,0,0,.5,1,1);
            if(col==2)shape=Shapes.box(.5,0,0,1,1,1);
            if(part==Part.DOOR)for(double[] range:TransporterRoom.doorRanges(s.getValue(STAGE),col))
                shape=Shapes.or(shape,Shapes.box(range[0],0,.35,range[1],1,.65));
        }
        Direction f=s.getValue(FACING);
        int rotations=(f.get2DDataValue()+2)%4;
        for(int i=0;i<rotations;i++){
            VoxelShape[] out={Shapes.empty()};shape.forAllBoxes((a,b,d,e,g,h)->out[0]=Shapes.or(out[0],Shapes.box(1-h,b,a,1-d,g,e)));shape=out[0];
        }
        return shape;
    }
    @Override public VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override public InteractionResult use(BlockState state,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){
        if(l.isClientSide)return InteractionResult.SUCCESS;
        TransporterEntity be=TransporterRoom.controller(l,p);
        if(be==null)return InteractionResult.PASS;
        if(!TransporterRoom.complete(l,be.anchor(),be.facing())){player.displayClientMessage(Component.literal("Transporter room is incomplete."),true);return InteractionResult.CONSUME;}
        if(state.getValue(PART)==Part.SWITCH||state.getValue(PART)==Part.CONTROL){be.toggleDoor();return InteractionResult.CONSUME;}
        if(state.getValue(PART)!=Part.PANEL&&state.getValue(PART)!=Part.MAP)return InteractionResult.PASS;
        if(!TransporterRoom.interior(be.anchor(),be.facing()).contains(player.position())){
            player.displayClientMessage(Component.literal("Step inside the transporter to choose a destination."),true);return InteractionResult.CONSUME;
        }
        if(player instanceof ServerPlayer sp){
            var destinations=TransporterNetwork.get((ServerLevel)l).validatedStations((ServerLevel)l);
            NetworkHooks.openScreen(sp,new SimpleMenuProvider((id,inv,ignored)->new TransporterMenu(id,inv,be.getBlockPos(),destinations),
                Component.literal(be.stationName())),buffer->{buffer.writeBlockPos(be.getBlockPos());TransporterMenu.writeStations(buffer,destinations);});
        }
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){
        if(!next.is(this)&&l instanceof ServerLevel sl){
            TransporterEntity be=TransporterRoom.controller(l,p);
            if(be!=null)TransporterNetwork.get(sl).remove(be.anchor());
        }
        super.onRemove(old,l,p,next,moving);
    }
}


