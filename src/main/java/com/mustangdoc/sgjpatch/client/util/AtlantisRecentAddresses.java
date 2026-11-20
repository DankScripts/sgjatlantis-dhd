package com.mustangdoc.sgjpatch.client.util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
public final class AtlantisRecentAddresses {
    public static void render(GuiGraphics g,AbstractDHDEntity dhd,int left,int top,int mouseX,int mouseY) {
        int x=left+498,y=top+162,w=128;
        g.fill(x,y,x+w,y+145,0xFF704836);
        g.fill(x+1,y+1,x+w-1,y+144,0xFF231A19);
        String[] entries=AtlantisAddressGuide.recent(dhd);
        text(g,"RECENT ADDRESSES",x+5,y+5,0xFFE4C798);
        if(entries.length==0)text(g,"No connections saved yet",x+5,y+21,0xFFB8C1C0);
        for(int i=0;i<entries.length;i++) {
            String address=entries[i];
            AtlantisAddressGuide.request(dhd,address);
            int row=y+20+i*15;
            if(AtlantisAddressGuide.selected(address) || mouseX>=x && mouseX<x+w && mouseY>=row && mouseY<row+15)
                g.fill(x+2,row,x+w-2,row+14,0xFF3C3330);
            text(g,shorten(AtlantisAddressGuide.name(address),27),x+5,row+1,0xFFE4C798);
            text(g,shorten(address,33),x+5,row+8,0xFFBFEAE7);
        }
        text(g,shorten(AtlantisAddressGuide.status(dhd),47),x,top+314,0xFFE4C798);
    }
    public static boolean click(AbstractDHDEntity dhd,int left,int top,double mouseX,double mouseY) {
        int x=left+498,y=top+182;
        String[] entries=AtlantisAddressGuide.recent(dhd);
        if(mouseX<x || mouseX>=x+128 || mouseY<y || mouseY>=y+entries.length*15)return false;
        AtlantisAddressGuide.select(dhd,entries[(int)(mouseY-y)/15]);
        return true;
    }
    private static String shorten(String text,int max) { return text.length()>max ? text.substring(0,max-1)+"…" : text; }
    private static void text(GuiGraphics g,String s,int x,int y,int color){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(.6F,.6F,1);
        g.drawString(Minecraft.getInstance().font,s,0,0,color,false);g.pose().popPose();
    }
}
