package com.mustangdoc.sgjpatch.client.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** The supplied plate silhouette and connected bronze lattice, in GUI coordinates. */
public final class AtlantisKeypadSurface {
    private static final List<Point> OUTLINE = outline();
    private static final int PLATE = 0xFFB97A56; // Supplied plate.png bronze.
    private AtlantisKeypadSurface() {}

    public static void backplate(GuiGraphics graphics, int left, int top) {
        polygon(graphics, expanded(3), left, top, 0xFF201B18);
        polygon(graphics, expanded(2), left, top, 0xFFD49765);
        polygon(graphics, expanded(1), left, top, PLATE);
    }

    /** Atlantis green crystal palette shared by every supported gate dialer. */
    public static void crystal(GuiGraphics graphics, int x, int y, int size, boolean up, int frame) {
        int color = frame == 1 ? 0xFF50D8BF : 0xFF159C89;
        // A thin bevel sits on the shared plate, replacing the separate thick bronze frames.
        triangle(graphics, x, y, size, up, 0xFF21150E);
        double inset = .65;
        double inner = size - 2 * inset;
        triangle(graphics, x + inset, y + (size - inner) * (up ? 2d/3 : 1d/3), inner, up, color);
    }

    private static void triangle(GuiGraphics graphics, double x, double y, double size, boolean up, int color) {
        for (int row = (int) Math.ceil(y); row < y + size; row++) {
            double t = Math.min(1, (row + .5 - y) / size);
            double half = (up ? t : 1 - t) * size / 2;
            graphics.fill((int) Math.ceil(x + size/2 - half), row,
                    (int) Math.ceil(x + size/2 + half), row + 1, color);
        }
    }

    private record Point(double x, double y) {}
    private static List<Point> outline() {
        List<Point> points=new ArrayList<>();
        double[] src={23,137,254,369,479,597};
        double h=52*Math.sqrt(3)/2;
        double[] dst={62,64+h,64+2*h,64+3*h,64+4*h,64+5*h};
        for(double[] p:AtlantisReferencePlate.OUTLINE){
            int row=0;while(row<4 && p[1]>src[row+1])row++;
            double y=dst[row]+(p[1]-src[row])/(src[row+1]-src[row])*(dst[row+1]-dst[row]);
            points.add(new Point(Math.max(278,Math.min(542,272+p[0]*52/149)),y));
        }
        return List.copyOf(points);
    }
    private static List<Point> expanded(double distance) {
        List<Point> result=new ArrayList<>();
        for(int i=0;i<OUTLINE.size();i++) {
            Point p=OUTLINE.get(i), before=OUTLINE.get((i+OUTLINE.size()-1)%OUTLINE.size()), after=OUTLINE.get((i+1)%OUTLINE.size());
            double ax=p.x-before.x, ay=p.y-before.y, bx=after.x-p.x, by=after.y-p.y;
            double al=Math.hypot(ax,ay), bl=Math.hypot(bx,by);
            double nx=ay/al, ny=-ax/al, mx=by/bl, my=-bx/bl;
            double denominator=1+nx*mx+ny*my;
            result.add(new Point(p.x+distance*(nx+mx)/denominator, p.y+distance*(ny+my)/denominator));
        }
        return result;
    }
    private static void polygon(GuiGraphics graphics, List<Point> polygon, int left, int top, int color) {
        double minY=polygon.stream().mapToDouble(Point::y).min().orElseThrow();
        double maxY=polygon.stream().mapToDouble(Point::y).max().orElseThrow();
        for(int y=(int)Math.ceil(minY);y<maxY;y++) {
            double scan=y+.5;
            List<Double> intersections=new ArrayList<>();
            for(int i=0;i<polygon.size();i++) {
                Point a=polygon.get(i), b=polygon.get((i+1)%polygon.size());
                if(scan>=Math.min(a.y,b.y)&&scan<Math.max(a.y,b.y))
                    intersections.add(a.x+(scan-a.y)*(b.x-a.x)/(b.y-a.y));
            }
            intersections.sort(Double::compare);
            // The reference has concave notches between its tabs: fill alternate spans.
            for(int i=0;i+1<intersections.size();i+=2)
                graphics.fill(left+(int)Math.ceil(intersections.get(i)),top+y,
                        left+(int)Math.ceil(intersections.get(i+1)),top+y+1,color);
        }
    }
}
