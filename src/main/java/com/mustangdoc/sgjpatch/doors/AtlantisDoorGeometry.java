package com.mustangdoc.sgjpatch.doors;
import java.util.*;
/** Grid-aligned aperture spans columns 1 and 2; crown spans their central halves. */
public final class AtlantisDoorGeometry {
 public static List<double[]> ranges(int stage,int column,boolean crown){
  double slide=stage/8.0,lo=(crown?1.5:1)-column,hi=(crown?2.5:3)-column;
  List<double[]> result=new ArrayList<>();
  add(result,Math.max(0,lo),Math.min(1,Math.min(hi,2-slide-column)));
  add(result,Math.max(0,Math.max(lo,2+slide-column)),Math.min(1,hi));
  return result;
 }
 public static List<double[]> wideRanges(int stage,int column){
  double slide=stage/8.0*1.5;List<double[]> result=new ArrayList<>();
  add(result,Math.max(0,1-column),Math.min(1,2.5-slide-column));
  add(result,Math.max(0,2.5+slide-column),Math.min(1,4-column));return result;
 }
 private static void add(List<double[]> result,double lo,double hi){if(hi>lo)result.add(new double[]{lo,hi});}
}
