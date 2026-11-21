package com.mustangdoc.sgjpatch.transporter;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class TransporterScreen extends AbstractContainerScreen<TransporterMenu> {
    private static final ResourceLocation MAP=TransporterRegistry.id("textures/gui/transporter_map.png");
    private int floor,page;

    private final List<Button> destinationButtons=new ArrayList<>();
    public TransporterScreen(TransporterMenu menu,Inventory inv,Component title){
        super(menu,inv,title);imageWidth=350;imageHeight=240;inventoryLabelY=10000;
        floor=0;
    }
    private boolean lowerCity(TransporterNetwork.Station station){
        var pos=station.pos();
        return TransporterLevels.isLower(pos.getX(),pos.getY(),pos.getZ(),station.name());
    }
    private List<Integer> visible(){
        List<Integer> indices=new ArrayList<>();
        for(int i=0;i<menu.destinations.size();i++)if(lowerCity(menu.destinations.get(i))==(floor==1))indices.add(i);
        return indices;
    }
    public void refreshDirectory(){page=0;clearWidgets();init();}
    @Override protected void init(){
        super.init();destinationButtons.clear();
        addRenderableWidget(Button.builder(Component.literal("▲"),b->{floor=Math.max(0,floor-1);page=0;updateButtons();}).bounds(leftPos+9,topPos+63,24,20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"),b->{floor=1;page=0;updateButtons();}).bounds(leftPos+9,topPos+116,24,20).build());
        for(int row=0;row<7;row++){
            final int r=row;
            destinationButtons.add(addRenderableWidget(Button.builder(Component.empty(),b->{List<Integer> list=visible();int n=page*7+r;if(n<list.size())select(list.get(n));}).bounds(leftPos+240,topPos+40+row*23,100,20).build()));
        }
        addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);updateButtons();}).bounds(leftPos+240,topPos+205,26,18).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->{page=Math.min(Math.max(0,(visible().size()-1)/7),page+1);updateButtons();}).bounds(leftPos+314,topPos+205,26,18).build());
        updateButtons();
    }
    private void updateButtons(){
        List<Integer> list=visible();
        for(int row=0;row<destinationButtons.size();row++){
            int n=page*7+row;Button b=destinationButtons.get(row);b.visible=n<list.size();
            if(b.visible){var s=menu.destinations.get(list.get(n));b.setMessage(Component.literal(font.plainSubstrByWidth(s.name(),92)));b.active=!s.pos().equals(menu.panel.below());}
        }
    }
    private void select(int id){
        if(minecraft!=null&&minecraft.player!=null&&minecraft.gameMode!=null
            &&!menu.destinations.get(id).pos().equals(menu.panel.below()))
            TransporterPackets.send(menu.containerId,menu.destinations.get(id).pos());
    }
    private int mapX(int x){return leftPos+42+(int)Math.round((x+4496)*190.0/1248);}
    private int mapY(int z){return topPos+32+(int)Math.round((z-5264)*190.0/1248);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff363b3d);
        g.renderOutline(leftPos,topPos,imageWidth,imageHeight,0xffb3a58b);
        g.fill(leftPos+5,topPos+5,leftPos+imageWidth-5,topPos+imageHeight-5,0xff102b3d);
        g.blit(MAP,leftPos+42,topPos+32,0,0,190,190,190,190);
        g.drawString(font,title,leftPos+12,topPos+13,0xffe6e5d2,false);
        g.drawString(font,"LEVEL",leftPos+7,topPos+45,0xffc5d5d7,false);
        g.drawString(font,floor==0?"CITY":"LOWER",leftPos+7,topPos+95,0xffefc967,false);
        for(int index:visible()){
            var s=menu.destinations.get(index);int x=mapX(s.pos().getX()),y=mapY(s.pos().getZ());
            if(x<leftPos+42||x>leftPos+232||y<topPos+32||y>topPos+222)continue;
            int color=s.pos().equals(menu.panel.below())?0xffe95147:0xffffd776;
            g.fill(x-3,y-3,x+4,y+4,color);g.renderOutline(x-5,y-5,11,11,color);
        }
        g.drawString(font,"Select a destination",leftPos+242,topPos+27,0xffd3e4e8,false);
        g.drawString(font,"Red: your location | Gold: destination",leftPos+9,topPos+228,0xffb9ced4,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);super.render(g,mx,my,partial);
        for(int i:visible()){
            var s=menu.destinations.get(i);if(Math.abs(mx-mapX(s.pos().getX()))<=6&&Math.abs(my-mapY(s.pos().getZ()))<=6)
                g.renderTooltip(font,Component.literal(s.name()),mx,my);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&x>=leftPos+42&&x<=leftPos+232&&y>=topPos+32&&y<=topPos+222){
            int best=-1;double distance=49;
            for(int i:visible()){var s=menu.destinations.get(i);if(s.pos().equals(menu.panel.below()))continue;
                double d=Math.pow(x-mapX(s.pos().getX()),2)+Math.pow(y-mapY(s.pos().getZ()),2);if(d<distance){best=i;distance=d;}}
            if(best>=0){select(best);return true;}
        }
        return super.mouseClicked(x,y,button);
    }
}
