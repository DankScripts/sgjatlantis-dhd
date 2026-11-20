package com.mustangdoc.sgjpatch.compat;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchTableBlockEntity;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchCableBootstrapEvents;

/** Missing concealed cables are rechecked on world changes, never polled forever. */
@Mod.EventBusSubscriber(modid="sgjadditions_capacity_patch", bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ConcealedPowerBootstrapChanges {
    private static final Set<BlockEntity> WATCHED=Collections.newSetFromMap(new WeakHashMap<>());
    public static void watch(BlockEntity table){WATCHED.add(table);}
    private static void recheck(BlockEntity table){
        if(table instanceof AtlantisHatchTableBlockEntity hatch)AtlantisHatchCableBootstrapEvents.register(hatch);
        else AtlantisDHDConcealedCableBootstrapEvents.register(table);
    }
    @SubscribeEvent public static void neighborChanged(BlockEvent.NeighborNotifyEvent event){
        if(event.getLevel().isClientSide())return;
        for(BlockEntity table:WATCHED.toArray(BlockEntity[]::new)){
            if(table.isRemoved()){WATCHED.remove(table);continue;}
            if(table.getLevel()!=event.getLevel())continue;
            var a=table.getBlockPos();var b=event.getPos();
            if(Math.abs(a.getX()-b.getX())<=2 && Math.abs(a.getZ()-b.getZ())<=2
                    && b.getY()>=a.getY()-4 && b.getY()<=a.getY())recheck(table);
        }
    }
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event){
        if(event.getLevel().isClientSide())return;
        var chunk=event.getChunk().getPos();
        for(BlockEntity table:WATCHED.toArray(BlockEntity[]::new)){
            if(table.isRemoved()){WATCHED.remove(table);continue;}
            var p=table.getBlockPos();
            if(table.getLevel()==event.getLevel() && Math.abs((p.getX()>>4)-chunk.x)<=1
                    && Math.abs((p.getZ()>>4)-chunk.z)<=1)recheck(table);
        }
    }
}
