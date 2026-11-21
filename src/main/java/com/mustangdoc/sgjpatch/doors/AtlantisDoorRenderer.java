package com.mustangdoc.sgjpatch.doors;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Moving leaves are rendered once per cabin, independently of chunk rebuilds. */
@EventBusSubscriber(modid = "sgjatlantis_dhd",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AtlantisDoorRenderer implements BlockEntityRenderer<AtlantisDoorEntity> {
    private static final java.util.Map<String,RenderType> MATERIALS=new java.util.HashMap<>();
    private static RenderType material(String name){return MATERIALS.computeIfAbsent(name,n->RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(n.startsWith("atlantis_")?"sgjatlantis_dhd":"minecraft","textures/block/"+n+".png")));}
    public AtlantisDoorRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(AtlantisDoorRegistry.ENTITY,AtlantisDoorRenderer::new);}
    @Override public boolean shouldRenderOffScreen(AtlantisDoorEntity be){return true;}
    @Override public AABB getRenderBoundingBox(AtlantisDoorEntity be){return new AABB(be.getBlockPos()).inflate(5);}
    @Override public void render(AtlantisDoorEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        String finishTexture=be.getBlockState().getValue(AtlantisFinish.PROPERTY).texture();
        float progress=be.progress(partial);
        float slide=progress*progress*(3-2*progress);
        pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(180-be.facing().toYRot()));
        int style=be.getBlockState().getValue(AtlantisDoorBlock.STYLE);
        if(style==2){pose.translate(-2.25,0,-.15);pose.scale(1.5f,1,1);}
        else pose.translate(be.getBlockState().getValue(AtlantisDoorBlock.ALIGNED)?-2:-1.5,0,-.15);
        for(int leaf=0;leaf<2;leaf++){
            float x=.5f+leaf+(leaf==0?-slide:slide);
            box(pose,buffers,light,overlay,x,0,0,x+1,3,.3f,finishTexture);
            if(style!=2)box(pose,buffers,light,overlay,x+(leaf==0?.5f:0),3,0,x+(leaf==0?1:.5f),3.5f,.3f,finishTexture);
            if(style==1){
                // Mirrored angular recessed panels and the square motif beside the centre seam.
                float inner=leaf==0?x+.82f:x+.18f,outer=leaf==0?x+.13f:x+.87f;
                stroke(pose,buffers,light,overlay,outer,2.55f,inner,2.10f);
                stroke(pose,buffers,light,overlay,inner,2.10f,inner,1.28f);
                stroke(pose,buffers,light,overlay,inner,1.28f,outer,1.70f);
                stroke(pose,buffers,light,overlay,outer,1.70f,outer,2.55f);
                stroke(pose,buffers,light,overlay,outer,1.42f,inner,1.0f);
                stroke(pose,buffers,light,overlay,inner,1.0f,inner,.55f);
                stroke(pose,buffers,light,overlay,inner,.55f,outer,.96f);
                stroke(pose,buffers,light,overlay,outer,.96f,outer,1.42f);
                float sx=leaf==0?x+.86f:x+.04f;
                for(float y:new float[]{.13f,.32f,.51f,.70f,2.68f,2.87f,3.06f}){
                    if(y>=3){if(leaf==0)sx=x+.78f;else sx=x+.12f;}
                    pane(pose,buffers,light,overlay,sx,y,-.018f,sx+.10f,y+.12f,.318f,"black_terracotta");
                    pane(pose,buffers,light,overlay,sx+.025f,y+.025f,-.022f,sx+.075f,y+.095f,.322f,"orange_terracotta");
                }
                continue;
            }
            if(style==2){
                // Tall asymmetric stained-glass mosaics mirrored across the centre seam.
                for(int strip=0;strip<16;strip++){
                    float u=strip/16f,v=(strip+1)/16f,inner=leaf==0?u:1-u;
                    float a=x+.14f+u*.72f,b=x+.14f+v*.72f;
                    float bottom=.3f+(inner<.22f?.18f:0),top=2.55f+(inner>.65f?.24f:inner*.35f);
                    pane(pose,buffers,light,overlay,a-.018f,bottom-.07f,-.015f,b+.018f,top+.07f,.315f,"black_terracotta");
                    String color=strip%6==0?"red_concrete":strip%6==1?"light_blue_concrete":strip%6==2?"white_concrete":strip%6==3?"yellow_terracotta":strip%6==4?"white_concrete":"light_gray_concrete";
                    pane(pose,buffers,light,overlay,a,bottom,-.025f,b,top,.325f,color);
                    pane(pose,buffers,light,overlay,a,.9f,-.03f,b,1.03f,.33f,finishTexture);
                }
                continue;
            }
            if(be.solid()){
                // Recessed rectilinear bronze trim on the solid quarters leaves, front and back.
                pane(pose,buffers,light,overlay,x+.16f,.25f,-.016f,x+.21f,2.8f,.316f,"gray_terracotta");
                pane(pose,buffers,light,overlay,x+.79f,.25f,-.016f,x+.84f,2.8f,.316f,"gray_terracotta");
                pane(pose,buffers,light,overlay,x+.21f,.25f,-.016f,x+.79f,.30f,.316f,"gray_terracotta");
                pane(pose,buffers,light,overlay,x+.21f,2.75f,-.016f,x+.79f,2.8f,.316f,"gray_terracotta");
                pane(pose,buffers,light,overlay,x+.38f,.3f,-.02f,x+.43f,1.45f,.32f,"gray_terracotta");
                pane(pose,buffers,light,overlay,x+.38f,1.4f,-.02f,x+.66f,1.45f,.32f,"gray_terracotta");
                pane(pose,buffers,light,overlay,x+.61f,1.4f,-.02f,x+.66f,2.75f,.32f,"gray_terracotta");
                continue;
            }
            // Mirrored, angled stained panels with dark dividers, following the supplied front views.
            for(int strip=0;strip<12;strip++){
                float u=strip/12f,v=(strip+1)/12f,angle=leaf==0?1-u:u;
                String glass=strip%4==0?"white_concrete":strip%4==1?"yellow_terracotta":strip%4==2?"orange_terracotta":"light_gray_concrete";
                for(int window=0;window<2;window++){
                    float bottom=window==0?.45f:1.55f,top=window==0?1.25f:2.75f;
                    bottom+=angle*.18f;top+=angle*.18f;
                    float a=x+.14f+u*.72f,b=x+.14f+v*.72f;
                    pane(pose,buffers,light,overlay,a,bottom,-.015f,b,top,.315f,"black_terracotta");
                    for(int band=0;band<4;band++){
                        float low=bottom+.04f+band*(top-bottom)/4,high=bottom+(band+1)*(top-bottom)/4-.035f;
                        pane(pose,buffers,light,overlay,a+.006f,low,-.025f,b-.006f,high,.325f,glass);
                    }
                }
            }
        }
        pose.popPose();
    }
    private static void stroke(PoseStack p,MultiBufferSource b,int light,int overlay,float x,float y,float X,float Y){
        int steps=32;for(int i=0;i<steps;i++){float t=i/(float)steps,u=(i+1)/(float)steps;
            float a=x+(X-x)*t,c=x+(X-x)*u,d=y+(Y-y)*t,e=y+(Y-y)*u;
            pane(p,b,light,overlay,Math.min(a,c)-.012f,Math.min(d,e)-.012f,-.02f,Math.max(a,c)+.012f,Math.max(d,e)+.012f,.32f,"black_terracotta");
        }
    }
    private static void pane(PoseStack p,MultiBufferSource b,int light,int overlay,float x,float y,float z,float X,float Y,float Z,String texture){
        x=Math.max(.5f,x);X=Math.min(2.5f,X);if(X<=x)return;
        VertexConsumer v=b.getBuffer(material(texture));
        face(v,p,light,overlay,0,0,-1,new float[][]{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}});
        face(v,p,light,overlay,0,0,1,new float[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}});
    }
    private static void box(PoseStack p,MultiBufferSource b,int light,int overlay,float x,float y,float z,float X,float Y,float Z,String texture){
        x=Math.max(y>=3?1:.5f,x);X=Math.min(y>=3?2:2.5f,X);if(X<=x)return;
        VertexConsumer v=b.getBuffer(material(texture));
        face(v,p,light,overlay,0,0,-1,new float[][]{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}});
        face(v,p,light,overlay,0,0,1,new float[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}});
        face(v,p,light,overlay,-1,0,0,new float[][]{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}});
        face(v,p,light,overlay,1,0,0,new float[][]{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}});
        face(v,p,light,overlay,0,1,0,new float[][]{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}});
        face(v,p,light,overlay,0,-1,0,new float[][]{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}});
    }
    private static void face(VertexConsumer v,PoseStack p,int light,int overlay,float nx,float ny,float nz,float[][] points){
        for(int i=0;i<4;i++)v.addVertex(p.last().pose(),points[i][0],points[i][1],points[i][2]).setColor(255,255,255,255)
            .setUv(i==0||i==3?0:1,i<2?1:0).setOverlay(overlay).setLight(light).setNormal(p.last(),nx,ny,nz);
    }
}


