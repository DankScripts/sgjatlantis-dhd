package com.mustangdoc.sgjpatch.power;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.config.CommonZPMConfig;

/** One transaction spans BOTH kinds of passive network, including SGJ probes. */
public final class MixedPowerTransfer {
    private static final int MAX_DISPATCHES=1024;
    private static final int MAX_DEPTH=64;
    private static final ThreadLocal<Context> CURRENT=new ThreadLocal<>();
    private record Probe(long amount, boolean zeroPoint) {}
    private static final class Context {
        final Set<Object> active=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Object,Map<Probe,Long>> probes=new IdentityHashMap<>();
        final Set<Object> empty=Collections.newSetFromMap(new IdentityHashMap<>());
        int remaining=MAX_DISPATCHES;
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
            // A cable run can touch the same floor through many endpoints.
            // Reject the back edge before SGJ recursively enumerates outputs.
            if(context.active.contains(network)||context.active.size()>=MAX_DEPTH)return 0;
            Probe probe=new Probe(amount,zeroPoint);
            if(simulate){
                var cached=context.probes.get(network);
                if(cached!=null && cached.containsKey(probe))return cached.get(probe);
            }else if(context.empty.contains(network))return 0;
            // Bound work even in multiply connected graphs with many distinct
            // offers. Unaccepted energy stays at the source for the next tick.
            if(context.remaining--<=0)return 0;
            context.active.add(network);
            boolean previousEnergyType=context.zeroPoint;
            context.zeroPoint=zeroPoint;
            long accepted;
            try{accepted=Math.max(0,Math.min(amount,transfer.getAsLong()));}
            finally{
                context.zeroPoint=previousEnergyType;
                context.active.remove(network);
            }
            if(simulate)context.probes.computeIfAbsent(network,k->new HashMap<>()).put(probe,accepted);
            else if(accepted==0)context.empty.add(network);
            return accepted;
        }finally{if(root)CURRENT.remove();}
    }
}
