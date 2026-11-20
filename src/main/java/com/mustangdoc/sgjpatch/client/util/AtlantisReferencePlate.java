package com.mustangdoc.sgjpatch.client.util;

import com.mustangdoc.sgjpatch.compat.LanteaJumperScreen.KeyShape;

/** Reference plate with row spacing corrected to the equilateral triangle lattice. */
public final class AtlantisReferencePlate {
    public static final double SCALE=.32, X=274, Y=74;
    // Match the horizontal divider to the perpendicular gap between sloping sides.
    private static final double ROW_SPACING = (110 * 1.08 + 142) * Math.sqrt(3) / (4 * 108);
    public static double plateY(double referenceY) {
        return Y + (310 + (referenceY - 310) * ROW_SPACING) * SCALE;
    }
    public static final double[][] OUTLINE={
        {240,3},
        {262,3},
        {275,22},
        {292,23},
        {381,23},
        {387,3},
        {416,4},
        {423,22},
        {525,22},
        {536,3},
        {562,3},
        {571,17},
        {562,46},
        {598,101},
        {615,126},
        {639,125},
        {650,137},
        {640,164},
        {684,237},
        {712,241},
        {722,254},
        {712,271},
        {713,274},
        {764,350},
        {787,351},
        {796,368},
        {785,388},
        {764,388},
        {715,462},
        {725,483},
        {715,497},
        {692,497},
        {641,575},
        {640,580},
        {650,601},
        {636,618},
        {610,618},
        {602,597},
        {498,597},
        {486,618},
        {464,617},
        {450,597},
        {352,598},
        {339,618},
        {318,618},
        {309,596},
        {201,596},
        {187,612},
        {166,617},
        {156,599},
        {168,579},
        {167,574},
        {115,495},
        {86,495},
        {80,481},
        {89,458},
        {47,391},
        {43,386},
        {17,389},
        {5,374},
        {18,351},
        {41,351},
        {91,278},
        {92,273},
        {82,255},
        {88,238},
        {119,235},
        {165,160},
        {153,143},
        {163,127},
        {191,127},
        {193,124},
        {241,41},
        {229,25}
    };
    private static final int[][] OPENINGS={
        {4,207,46,110,95,1},
        {10,279,46,109,95,0},
        {9,349,45,110,96,1},
        {3,421,46,109,95,0},
        {1,491,45,110,96,1},
        {14,136,152,110,95,1},
        {12,208,152,109,96,0},
        {21,278,152,109,95,1},
        {26,349,152,110,96,0},
        {20,420,152,109,95,1},
        {11,492,152,109,96,0},
        {2,562,152,110,96,1},
        {23,65,261,110,95,1},
        {6,137,261,109,96,0},
        {38,207,261,110,95,1},
        {28,279,261,109,96,0},
        {8,349,261,109,95,1},
        {32,421,261,109,96,0},
        {27,491,261,109,95,1},
        {13,562,261,110,96,0},
        {22,633,261,110,95,1},
        {35,66,369,109,95,0},
        {16,136,368,110,96,1},
        {30,207,369,110,96,0},
        {37,278,368,110,96,1},
        {34,349,369,110,96,0},
        {29,419,369,110,95,1},
        {17,491,369,109,95,0},
        {19,561,368,110,96,1},
        {5,633,369,109,96,0},
        {18,138,479,109,95,0},
        {25,208,479,109,95,1},
        {31,279,479,110,96,0},
        {24,350,479,109,95,1},
        {36,421,479,110,96,0},
        {7,491,479,110,95,1},
        {15,563,479,109,95,0}
    };
    private AtlantisReferencePlate() {}
    public static KeyShape keyShape(int symbol) {
        int[][] rows={{18,25,31,24,36,7,15},{35,16,30,37,34,29,17,19,5},
            {23,6,38,28,8,32,27,13,22},{14,12,21,26,20,11,2},{4,10,9,3,1}};
        double side=52,h=side*Math.sqrt(3)/2,x=0,y=0;boolean up=true;
        if(symbol==8||symbol==0||symbol==33||symbol==-1){
            x=386;y=64+2*h;
            if(symbol==8)x+=13;
            else{y+=h/2;if(symbol==33)x+=26;else if(symbol==-1)x+=13;}
            up=symbol!=-1;side=26;
        }else{
            boolean found=false;
            for(int r=0;r<5;r++)for(int c=0;c<rows[r].length;c++)if(rows[r][c]==symbol){
                x=282+((9-rows[r].length)/2d+c)*26;y=64+(4-r)*h;
                up=(c+(r<2?0:1))%2!=0;found=true;
            }
            if(!found)throw new IllegalArgumentException("Unknown symbol "+symbol);
        }
        h=side*Math.sqrt(3)/2;double w=side-4,ih=h*w/side;
        return new KeyShape(x+2,y+(h-ih)*(up?2d/3:1d/3),w,ih,up);
    }
}