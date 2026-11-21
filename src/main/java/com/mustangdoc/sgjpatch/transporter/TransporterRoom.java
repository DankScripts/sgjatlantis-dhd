package com.mustangdoc.sgjpatch.transporter;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Anchor is the back wall at foot height; the panel is one block above it. */
public final class TransporterRoom {
    public static List<double[]> doorRanges(int stage,int column) {
        double slide=stage/8.0,left=1.5-slide-column,right=1.5+slide-column;
        List<double[]> ranges=new ArrayList<>();
        double lo=Math.max(0,.5-column),hi=Math.min(1,2.5-column);
        if(left>lo)ranges.add(new double[]{lo,Math.min(hi,left)});
        if(right<hi)ranges.add(new double[]{Math.max(lo,right),hi});
        return ranges;
    }
    public static BlockPos placementAnchor(BlockPos clickedFloor,Direction facing){return clickedFloor.above().relative(facing,-3);}
    public static BlockPos at(BlockPos anchor,Direction facing,int side,int height,int depth) {
        Direction right=facing.getClockWise();
        return anchor.relative(right,side).relative(facing,depth).above(height);
    }
    public static Map<BlockPos,BlockState> shell(BlockPos a,Direction f) {
        Map<BlockPos,BlockState> result=new LinkedHashMap<>();
        for(int d=0;d<=3;d++) for(int x=-2;x<=2;x++) for(int y=-1;y<=3;y++) {
            TransporterBlock.Part p=null;
            if(y==-1)p=TransporterBlock.Part.FLOOR;
            else if(y==3)p=TransporterBlock.Part.CEILING;
            else if(d==0||x==-2||x==2)p=TransporterBlock.Part.WALL;
            else if(d==3)p=TransporterBlock.Part.DOOR;
            if(d==0&&x==0&&y==1)p=TransporterBlock.Part.PANEL;
            if(d==3&&Math.abs(x)<2&&y>=0&&y<3)p=TransporterBlock.Part.DOOR;
            if(d==3&&x==0&&y==3)p=TransporterBlock.Part.CROWN;

            if(p!=null)result.put(at(a,f,x,y,d),(p==TransporterBlock.Part.PANEL?TransporterRegistry.PANEL:TransporterRegistry.BLOCK).defaultBlockState()
                .setValue(TransporterBlock.FACING,f).setValue(TransporterBlock.PART,p)
                .setValue(TransporterBlock.COLUMN,Math.max(0,Math.min(2,x+1)))
                .setValue(TransporterBlock.ROW,Math.max(0,Math.min(2,y))));
        }
        result.put(at(a,f,-2,1,4),TransporterRegistry.BLOCK.defaultBlockState().setValue(TransporterBlock.FACING,f).setValue(TransporterBlock.PART,TransporterBlock.Part.SWITCH));
        return result;
    }
    public static List<BlockPos> volume(BlockPos a,Direction f) {
        List<BlockPos> positions=new ArrayList<>();
        for(int d=0;d<=3;d++)for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++)positions.add(at(a,f,x,y,d));
        positions.add(at(a,f,-2,1,4));
        return positions;
    }
    public static AABB interior(BlockPos a,Direction f) {
        Vec3 c=Vec3.atBottomCenterOf(a).add(f.getStepX()*1.5,0,f.getStepZ()*1.5);
        double rx=f.getAxis()==Direction.Axis.X?1:1.5,rz=f.getAxis()==Direction.Axis.Z?1:1.5;
        return new AABB(c.x-rx,a.getY(),c.z-rz,c.x+rx,a.getY()+3,c.z+rz);
    }
    public static boolean complete(Level l,BlockPos a,Direction f) {
        for(var e:shell(a,f).entrySet()) {
            BlockState s=l.getBlockState(e.getKey()),expected=e.getValue();
            if(!s.is(expected.getBlock())||s.getValue(TransporterBlock.PART)!=expected.getValue(TransporterBlock.PART)
                ||s.getValue(TransporterBlock.FACING)!=f||s.getValue(TransporterBlock.COLUMN)!=expected.getValue(TransporterBlock.COLUMN)
                ||s.getValue(TransporterBlock.ROW)!=expected.getValue(TransporterBlock.ROW))return false;
        }
        return true;
    }
    public static void build(ServerLevel l,BlockPos a,Direction f,String name) {build(l,a,f,name,AtlantisFinish.LEGACY);}
    public static void build(ServerLevel l,BlockPos a,Direction f,String name,AtlantisFinish finish) {
        Map<BlockPos,BlockState> shell=shell(a,f);
        shell.replaceAll((pos,state)->state.setValue(AtlantisFinish.PROPERTY,finish));
        // Place the controller last so its first tick sees a completed cabin.
        BlockPos panel=a.above();
        for(BlockPos p:volume(a,f))if(!p.equals(panel))l.setBlock(p,shell.getOrDefault(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()),3);
        l.setBlock(panel,shell.get(panel),3);
        if(l.getBlockEntity(panel) instanceof TransporterEntity be){be.rename(name);be.currentLayout();}
    }
    /** Move an existing five-deep cabin's back wall forward, retaining its doorway. */
    public static boolean shorten(ServerLevel l,TransporterEntity old){
        Direction f=old.facing();BlockPos oldAnchor=old.getBlockPos().below(),newAnchor=oldAnchor.relative(f);
        List<BlockPos> footprint=new ArrayList<>();
        for(int d=0;d<=4;d++)for(int side=-2;side<=2;side++)for(int y=-1;y<=3;y++)
            footprint.add(at(oldAnchor,f,side,y,d));
        footprint.add(at(oldAnchor,f,-2,1,5));
        for(BlockPos p:footprint)if(!l.hasChunkAt(p))return false;
        BlockPos min=new BlockPos(footprint.stream().mapToInt(BlockPos::getX).min().orElseThrow(),oldAnchor.getY()-1,
            footprint.stream().mapToInt(BlockPos::getZ).min().orElseThrow());
        BlockPos max=new BlockPos(footprint.stream().mapToInt(BlockPos::getX).max().orElseThrow(),oldAnchor.getY()+3,
            footprint.stream().mapToInt(BlockPos::getZ).max().orElseThrow());
        if(!l.getEntities(null,new AABB(min.getX(),min.getY(),min.getZ(),
            max.getX()+1,max.getY()+1,max.getZ()+1).inflate(1)).isEmpty())return false;
        for(BlockPos p:volume(newAnchor,f)){
            BlockState s=l.getBlockState(p);
            if(!s.isAir()&&!s.is(TransporterRegistry.BLOCK)&&!s.is(TransporterRegistry.PANEL))return false;
        }
        var id=TransporterRegistry.id("transporter_backups/short_"+oldAnchor.asLong()+"_"+l.getGameTime());
        var backup=l.getStructureManager().getOrCreate(id);
        backup.fillFromWorld(l,min,new Vec3i(max.getX()-min.getX()+1,5,max.getZ()-min.getZ()+1),false,null);
        if(!l.getStructureManager().save(id))return false;
        String name=old.stationName();
        TransporterNetwork.get(l).remove(oldAnchor);
        l.removeBlock(old.getBlockPos(),false);
        build(l,newAnchor,f,name);
        for(int side=-2;side<=2;side++)for(int y=-1;y<=3;y++){
            BlockPos p=at(oldAnchor,f,side,y,0);BlockState s=l.getBlockState(p);
            if(s.is(TransporterRegistry.BLOCK)||s.is(TransporterRegistry.PANEL))
                l.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        }
        return true;
    }
    public static boolean upgrade(ServerLevel l,TransporterEntity be){
        var positions=volume(be.anchor(),be.facing());
        BlockPos min=new BlockPos(positions.stream().mapToInt(BlockPos::getX).min().orElseThrow(),be.anchor().getY()-1,positions.stream().mapToInt(BlockPos::getZ).min().orElseThrow());
        BlockPos max=new BlockPos(positions.stream().mapToInt(BlockPos::getX).max().orElseThrow(),be.anchor().getY()+3,positions.stream().mapToInt(BlockPos::getZ).max().orElseThrow());
        if(!l.getEntities(null,new AABB(min.getX(),min.getY(),min.getZ(),max.getX()+1,max.getY()+1,max.getZ()+1)).isEmpty())return false;
        for(BlockPos p:positions){
            if(!l.hasChunkAt(p))return false;
            BlockState s=l.getBlockState(p);
            String id=String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()));
            boolean oldPanel=p.equals(at(be.anchor(),be.facing(),-2,1,5))&&
                    (id.equals("sgjadditions:wallpanel")||id.equals("sgjatlantis_dhd:wallpanel")||id.equals("stargate_atlantis:wallpanel"));
            if(!s.isAir()&&!s.is(TransporterRegistry.BLOCK)&&!s.is(TransporterRegistry.PANEL)&&!oldPanel)return false;
        }
        var manager=l.getStructureManager();
        var id=TransporterRegistry.id("transporter_backups/compact_"+be.anchor().asLong()+"_"+l.getGameTime());
        var backup=manager.getOrCreate(id);
        backup.fillFromWorld(l,min,new Vec3i(max.getX()-min.getX()+1,5,max.getZ()-min.getZ()+1),false,null);
        if(!manager.save(id))return false;
        for(var entry:shell(be.anchor(),be.facing()).entrySet())if(!entry.getKey().equals(be.getBlockPos()))l.setBlock(entry.getKey(),entry.getValue(),3);
        removeDuplicateBackWall(l,be);
        return true;
    }
    /** Remove only the redundant lining of an intact revision-4 cabin. */
    public static boolean removeDuplicateBackWall(ServerLevel l, TransporterEntity be) {
        if (!complete(l, be.anchor(), be.facing())) return false;
        for (int x=-1;x<=1;x++) for (int y=0;y<3;y++) {
            BlockPos pos=at(be.anchor(),be.facing(),x,y,1);
            if (!l.hasChunkAt(pos)) return false;
            BlockState state=l.getBlockState(pos);
            if (!state.isAir() && (!state.is(TransporterRegistry.BLOCK)
                || (state.getValue(TransporterBlock.PART)!=TransporterBlock.Part.WALL
                    && state.getValue(TransporterBlock.PART)!=TransporterBlock.Part.MAP))) return false;
        }
        for (int x=-1;x<=1;x++) for (int y=0;y<3;y++)
            l.setBlock(at(be.anchor(),be.facing(),x,y,1),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        return true;
    }
    public static boolean orphanedPart(Level level, BlockPos pos) {
        // Open door segments can be invisible and cannot be targeted for removal.
        // Only non-controller pieces without a surviving controller are reusable.
        if (!level.getBlockState(pos).is(TransporterRegistry.BLOCK)) return false;
        return controller(level,pos)==null;
    }
    public static TransporterEntity controller(Level l,BlockPos part) {
        BlockState state=l.getBlockState(part);
        if(state.is(TransporterRegistry.BLOCK)&&state.getValue(TransporterBlock.PART)==TransporterBlock.Part.SWITCH){
            Direction f=state.getValue(TransporterBlock.FACING);
            BlockPos panel=part.relative(f,-4).relative(f.getClockWise(),2);
            return l.getBlockEntity(panel) instanceof TransporterEntity be?be:null;
        }
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=-2;dy<=2;dy++) {
            if(l.getBlockEntity(part.offset(dx,dy,dz)) instanceof TransporterEntity be
                &&shell(be.anchor(),be.facing()).containsKey(part))return be;
        }
        return null;
    }
    public static Vec3 transform(Vec3 position,BlockPos from,Direction ff,BlockPos to,Direction tf) {
        Vec3 origin=Vec3.atCenterOf(from).add(0,-.5,0),target=Vec3.atCenterOf(to).add(0,-.5,0);
        Vec3 v=position.subtract(origin);Direction r=ff.getClockWise(),tr=tf.getClockWise();
        double depth=v.x*ff.getStepX()+v.z*ff.getStepZ(),side=v.x*r.getStepX()+v.z*r.getStepZ();
        return target.add(depth*tf.getStepX()+side*tr.getStepX(),v.y,depth*tf.getStepZ()+side*tr.getStepZ());
    }
}

