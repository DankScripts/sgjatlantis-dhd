package com.mustangdoc.sgjpatch.transporter;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=TransporterRegistry.NS)
public final class TransporterService {
    private record Trip(TransporterEntity from,TransporterEntity to,long started){}
    private static final Map<ServerLevel,List<Trip>> TRIPS=new WeakHashMap<>();
    public static boolean start(ServerPlayer player,TransporterEntity from,BlockPos target){
        ServerLevel l=player.serverLevel();
        if(from.reserved||from.anchor().equals(target)||!TransporterRoom.interior(from.anchor(),from.facing()).contains(player.position()))return false;
        var station=TransporterNetwork.get(l).find(target);if(station==null)return false;
        // Load all cabin chunks, not just the controller chunk, before validation.
        for(BlockPos p:TransporterRoom.volume(target,station.facing()))l.getChunk(p);
        if(!(l.getBlockEntity(target.above()) instanceof TransporterEntity to)||!TransporterRoom.complete(l,target,station.facing())){
            TransporterNetwork.get(l).remove(target);player.displayClientMessage(Component.literal("Destination room is missing or incomplete."),true);return false;
        }
        if(to.reserved){player.displayClientMessage(Component.literal("Destination transporter is in use."),true);return false;}
        if(!TransporterRoom.complete(l,from.anchor(),from.facing()))return false;
        if(!safe(l,from,to)||!safe(l,to,from)){player.displayClientMessage(Component.literal("Clear the cabin interiors and doorway; dismount before transport."),true);return false;}
        pin(l,from);pin(l,to);from.reserved=true;to.reserved=true;
        TRIPS.computeIfAbsent(l,k->new ArrayList<>()).add(new Trip(from,to,l.getGameTime()));
        l.playSound(null,from.getBlockPos(),SoundEvents.IRON_DOOR_CLOSE,SoundSource.BLOCKS,.55f,.7f);
        player.closeContainer();return true;
    }
    private static void pin(ServerLevel l,TransporterEntity e){
        l.getChunkSource().addRegionTicket(TicketType.PORTAL,new ChunkPos(e.anchor()),3,e.anchor());
    }
    private static List<Entity> occupants(ServerLevel l,TransporterEntity e){
        return l.getEntities((Entity)null,TransporterRoom.interior(e.anchor(),e.facing()),x->x.isAlive()&&!x.isSpectator());
    }
    private static boolean safe(ServerLevel l,TransporterEntity from,TransporterEntity to){
        AABB box=TransporterRoom.interior(from.anchor(),from.facing());
        // Do not close a door through an entity or transport part of an entity.
        BlockPos door=TransporterRoom.at(from.anchor(),from.facing(),0,0,3);
        AABB threshold=new AABB(door).inflate(from.facing().getAxis()==Direction.Axis.Z?1:.1,1,from.facing().getAxis()==Direction.Axis.X?1:.1);
        if(!l.getEntities((Entity)null,threshold,e->e.isAlive()&&!e.isSpectator()).isEmpty())return false;
        for(Entity e:occupants(l,from)){
            if(e.isPassenger()||e.isVehicle()||!inside(box,e.getBoundingBox()))return false;
            Vec3 p=TransporterRoom.transform(e.position(),from.anchor(),from.facing(),to.anchor(),to.facing());
            AABB projected=e.getBoundingBox().move(p.subtract(e.position()));
            if(!inside(TransporterRoom.interior(to.anchor(),to.facing()),projected)
                ||l.getBlockCollisions(e,projected).iterator().hasNext()||!l.getWorldBorder().isWithinBounds(projected))return false;
        }
        return true;
    }
    private static boolean inside(AABB a,AABB b){return b.minX>=a.minX&&b.minY>=a.minY&&b.minZ>=a.minZ&&b.maxX<=a.maxX&&b.maxY<=a.maxY&&b.maxZ<=a.maxZ;}
    private static void move(List<Entity> group,TransporterEntity from,TransporterEntity to,ServerLevel l){
        for(Entity e:group){
            Vec3 p=TransporterRoom.transform(e.position(),from.anchor(),from.facing(),to.anchor(),to.facing());
            float yaw=e.getYRot()+to.facing().toYRot()-from.facing().toYRot();
            if(e instanceof ServerPlayer sp){sp.closeContainer();sp.teleportTo(l,p.x,p.y,p.z,yaw,e.getXRot());}
            else {e.teleportTo(p.x,p.y,p.z);e.setYRot(yaw);}
            e.setDeltaMovement(Vec3.ZERO);e.fallDistance=0;
        }
    }
    private static void flash(ServerLevel l,TransporterEntity e){
        Vec3 c=TransporterRoom.interior(e.anchor(),e.facing()).getCenter();
        l.sendParticles(ParticleTypes.FLASH,c.x,c.y,c.z,1,0,0,0,0);
        l.sendParticles(ParticleTypes.END_ROD,c.x,c.y,c.z,70,1,1,1,.025);
        l.playSound(null,e.getBlockPos(),SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,.6f,1.5f);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel l))return;
        List<Trip> trips=TRIPS.get(l);if(trips==null)return;
        for(Iterator<Trip> it=trips.iterator();it.hasNext();){
            Trip t=it.next();long age=l.getGameTime()-t.started();
            boolean valid=!t.from().isRemoved()&&!t.to().isRemoved()
                &&TransporterRoom.complete(l,t.from().anchor(),t.from().facing())&&TransporterRoom.complete(l,t.to().anchor(),t.to().facing());
            if(valid&&age<TransporterEntity.DOOR_TICKS+8)valid=safe(l,t.from(),t.to())&&safe(l,t.to(),t.from());
            if(valid&&age==TransporterEntity.DOOR_TICKS+8){
                valid=safe(l,t.from(),t.to())&&safe(l,t.to(),t.from());
                if(valid){
                    List<Entity> a=occupants(l,t.from()),b=occupants(l,t.to());
                    flash(l,t.from());flash(l,t.to());
                    move(a,t.from(),t.to(),l);move(b,t.to(),t.from(),l);
                }
            }
            if(!valid||age>=TransporterEntity.DOOR_TICKS+20){
                t.from().reserved=false;t.to().reserved=false;if(valid){t.from().openForArrival();t.to().openForArrival();}
                else {t.from().openForEntry();t.to().openForEntry();}
                l.getChunkSource().removeRegionTicket(TicketType.PORTAL,new ChunkPos(t.from().anchor()),3,t.from().anchor());
                l.getChunkSource().removeRegionTicket(TicketType.PORTAL,new ChunkPos(t.to().anchor()),3,t.to().anchor());
                it.remove();
            }
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload e){if(e.getLevel() instanceof ServerLevel l)TRIPS.remove(l);}
}

