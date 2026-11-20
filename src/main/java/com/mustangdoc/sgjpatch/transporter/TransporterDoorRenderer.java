package com.mustangdoc.sgjpatch.transporter;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Moving leaves are rendered once per cabin, independently of chunk rebuilds. */
@Mod.EventBusSubscriber(modid=TransporterRegistry.NS,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TransporterDoorRenderer implements BlockEntityRenderer<TransporterEntity> {
    private static final java.util.Map<String,RenderType> MATERIALS=new java.util.HashMap<>();
    private static RenderType material(String name){return MATERIALS.computeIfAbsent(name,n->RenderType.entityCutoutNoCull(new ResourceLocation(n.startsWith("atlantis_")?"sgjadditions_capacity_patch":"minecraft","textures/block/"+n+".png")));}
    public TransporterDoorRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(TransporterRegistry.ENTITY,TransporterDoorRenderer::new);}
    @Override public boolean shouldRenderOffScreen(TransporterEntity be){return true;}
    @Override public void render(TransporterEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        String finishTexture=be.getBlockState().getValue(AtlantisFinish.PROPERTY).texture();
        float progress=be.progress(partial);
        float slide=progress*progress*(3-2*progress);
        pose.pushPose();pose.translate(.5,-1,.5);
        pose.mulPose(Axis.YP.rotationDegrees(180-be.facing().toYRot()));
        pose.translate(-1.5,0,-3.15);
        for(int leaf=0;leaf<2;leaf++){
            float x=.5f+leaf+(leaf==0?-slide:slide);
            box(pose,buffers,light,overlay,x,0,0,x+1,3,.3f,finishTexture);
            box(pose,buffers,light,overlay,x+(leaf==0?.5f:0),3,0,x+(leaf==0?1:.5f),3.5f,.3f,finishTexture);
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
        for(int i=0;i<4;i++)v.vertex(p.last().pose(),points[i][0],points[i][1],points[i][2]).color(255,255,255,255)
            .uv(i==0||i==3?0:1,i<2?1:0).overlayCoords(overlay).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();
    }
}

