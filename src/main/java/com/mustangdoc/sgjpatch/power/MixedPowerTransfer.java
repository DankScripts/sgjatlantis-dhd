package com.mustangdoc.sgjpatch.power;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

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
    }
    private MixedPowerTransfer() {}
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
            long accepted;
            try{accepted=Math.max(0,Math.min(amount,transfer.getAsLong()));}
            finally{context.active.remove(network);}
            if(simulate)context.probes.computeIfAbsent(network,k->new HashMap<>()).put(probe,accepted);
            else if(accepted==0)context.empty.add(network);
            return accepted;
        }finally{if(root)CURRENT.remove();}
    }
}
