package com.mustangdoc.sgjpatch.power;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import net.minecraftforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.config.CommonZPMConfig;

/** One transaction spans BOTH kinds of passive network, including SGJ probes. */
public final class MixedPowerTransfer {
    private static final int MAX_DISPATCHES=1024;
    private static final int MAX_DEPTH=64;
    private static final ThreadLocal<Context> CURRENT=new ThreadLocal<>();
    private record Probe(long amount, boolean zeroPoint) {}
    private record CachedProbe(Set<Object> active, Set<Object> visited, long accepted) {
        boolean matches(Set<Object> current){
            // Only visited networks can change this probe's cycle-guard decisions.
            if(active.size()!=current.size())return false;
            for(Object network:visited)
                if(active.contains(network)!=current.contains(network))return false;
            return true;
        }
    }
    private static final class Context {
        final Set<Object> active=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Object,Map<Probe,List<CachedProbe>>> probes=new IdentityHashMap<>();
        final List<Set<Object>> probeFrames=new ArrayList<>();
        int remaining=MAX_DISPATCHES;
        int incomplete;
        int mutations;
        boolean zeroPoint;
    }
    private MixedPowerTransfer() {}

    /** SGJourney's output count must probe with the same energy type it will send. */
    public static int probeCableOutput(IEnergyStorage output,int amount,boolean simulate){
        Context context=CURRENT.get();
        if(context==null||!context.zeroPoint)return output.receiveEnergy(amount,simulate);
        if(output instanceof SGJourneyEnergy sg)
            return (int)Math.min(amount,sg.receiveZeroPointEnergy(amount,simulate));
        return CommonZPMConfig.other_mods_use_zero_point_energy.get()?output.receiveEnergy(amount,simulate):0;
    }

    public static long run(Object network,long amount,boolean simulate,boolean zeroPoint,LongSupplier transfer){
        if(amount<=0)return 0;
        Context context=CURRENT.get();boolean root=context==null;
        if(root){context=new Context();CURRENT.set(context);}
        try{
            for(Set<Object> frame:context.probeFrames)frame.add(network);
            // A cable run can touch the same floor through many endpoints.
            // Reject the back edge before SGJ recursively enumerates outputs.
            if(context.active.contains(network))return 0;
            if(context.active.size()>=MAX_DEPTH){context.incomplete++;return 0;}
            Probe probe=new Probe(amount,zeroPoint);
            if(simulate){
                var cached=context.probes.get(network);
                if(cached!=null){
                    var results=cached.get(probe);
                    if(results!=null)for(CachedProbe result:results)
                        if(result.matches(context.active)){
                            for(Set<Object> frame:context.probeFrames)frame.addAll(result.visited());
                            return result.accepted();
                        }
                }
            }
            // Bound work even in multiply connected graphs with many distinct
            // offers. Unaccepted energy stays at the source for the next tick.
            if(context.remaining--<=0){context.incomplete++;return 0;}
            int incomplete=context.incomplete;
            int mutations=context.mutations;
            Set<Object> visited=null;
            if(simulate){
                visited=Collections.newSetFromMap(new IdentityHashMap<>());
                visited.add(network);
                context.probeFrames.add(visited);
            }
            context.active.add(network);
            boolean previousEnergyType=context.zeroPoint;
            context.zeroPoint=zeroPoint;
            long accepted;
            try{accepted=Math.max(0,Math.min(amount,transfer.getAsLong()));}
            finally{
                context.zeroPoint=previousEnergyType;
                context.active.remove(network);
                if(simulate)context.probeFrames.remove(context.probeFrames.size()-1);
            }
            if(simulate && incomplete==context.incomplete && mutations==context.mutations){
                Set<Object> active=Collections.newSetFromMap(new IdentityHashMap<>());
                active.addAll(context.active);
                context.probes.computeIfAbsent(network,k->new HashMap<>())
                        .computeIfAbsent(probe,k->new ArrayList<>()).add(new CachedProbe(active,visited,accepted));
            }else if(!simulate){
                context.mutations++;
                context.probes.clear();
            }
            return accepted;
        }finally{if(root)CURRENT.remove();}
    }
}
