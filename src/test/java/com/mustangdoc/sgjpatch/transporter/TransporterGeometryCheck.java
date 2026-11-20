package com.mustangdoc.sgjpatch.transporter;

import net.minecraft.core.*;
import net.minecraft.world.phys.*;

/** Executable regression checks, independent of a user's save. */
public final class TransporterGeometryCheck {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        BlockPos a=new BlockPos(-3897,413,5871),b=new BlockPos(-4267,326,5886);
        for(Direction f:Direction.Plane.HORIZONTAL){
            BlockPos clicked=new BlockPos(100,64,100),placed=TransporterRoom.placementAnchor(clicked,f);
            check(TransporterRoom.at(placed,f,0,0,3).equals(clicked.above()),"Placement click is the entrance for every orientation");
            check(placed.equals(clicked.above().relative(f.getOpposite(),3)),"Cabin extends behind entrance, away from player");
            check(TransporterRoom.volume(a,f).size()==101,"Four-deep cabin plus external control");
            check(TransporterRoom.interior(a,f).getXsize()*TransporterRoom.interior(a,f).getYsize()*TransporterRoom.interior(a,f).getZsize()==18,"Three wide by two deep interior with three-block headroom");
            for(Direction g:Direction.Plane.HORIZONTAL){
                Vec3 p=Vec3.atBottomCenterOf(TransporterRoom.at(a,f,0,0,2)).add(.12,0,.17);
                Vec3 target=TransporterRoom.transform(p,a,f,b,g);
                check(TransporterRoom.interior(b,g).contains(target),"Safe destination for every orientation");
                check(TransporterRoom.transform(target,b,g,a,f).distanceToSqr(p)<1e-12,"Round trip preserves position");
            }
            for(int stage=0;stage<=8;stage++){
                double width=0;
                for(int x=0;x<3;x++)for(double[] range:TransporterRoom.doorRanges(stage,x)){
                    check(range[0]>=0&&range[1]<=1&&range[1]>range[0],"Door collision inside cell");width+=range[1]-range[0];
                }
                check(Math.abs(width-(2-stage*2.0/8))<1e-9,"Door opening width at stage "+stage);
            }
        }
        check(!TransporterRoom.volume(a,Direction.EAST).contains(new BlockPos(-3898,412,5873)),"Back-to-back teleporter stays untouched");
        check(TransporterRoom.at(a,Direction.EAST,-2,1,4).equals(new BlockPos(-3893,414,5869)),"External control stays one block beyond the door");
        for(int stage=0;stage<=8;stage++){
            double width=0,crown=0;
            for(int col=1;col<=2;col++){
                for(double[] r:com.mustangdoc.sgjpatch.doors.AtlantisDoorGeometry.ranges(stage,col,false)){check(r[0]>=0&&r[1]<=1,"Aligned door stays within opening cells");width+=r[1]-r[0];}
                for(double[] r:com.mustangdoc.sgjpatch.doors.AtlantisDoorGeometry.ranges(stage,col,true))crown+=r[1]-r[0];
            }
            check(Math.abs(width-(2-stage/4.0))<1e-9,"Grid aligned doorway width");
            check(Math.abs(crown-Math.max(0,1-stage/4.0))<1e-9,"Centred stepped crown width");
        }
        for(int stage=0;stage<=8;stage++){
            double width=0;
            for(int col=1;col<=3;col++)for(double[] r:com.mustangdoc.sgjpatch.doors.AtlantisDoorGeometry.wideRanges(stage,col)){
                check(r[0]>=0&&r[1]<=1&&r[1]>r[0],"Wide door collision remains in its grid cell");width+=r[1]-r[0];
            }
            check(Math.abs(width-3*(1-stage/8.0))<1e-9,"Three-block door aperture at every slide stage");
        }
        System.out.println("PASS: cabin footprint, 16 orientation transfers, door collisions, adjacent teleporter preserved");
    }
}
