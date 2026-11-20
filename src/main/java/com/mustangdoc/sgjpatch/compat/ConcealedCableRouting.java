package com.mustangdoc.sgjpatch.compat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** Cable traversal is based on cable blocks, including segments without block entities. */
public final class ConcealedCableRouting {
    private ConcealedCableRouting() {}

    public static <P, R> List<R> findReceivers(P start, int limit, Predicate<P> isCable,
            Function<P, ? extends Iterable<P>> neighbors,
            Function<P, ? extends Iterable<R>> receiversAbove) {
        var pending = new ArrayDeque<P>();
        var visited = new HashSet<P>();
        var receivers = new LinkedHashSet<R>();
        pending.add(start);
        int cables = 0;
        while (!pending.isEmpty() && cables < limit) {
            P pos = pending.remove();
            if (!visited.add(pos) || !isCable.test(pos)) continue;
            cables++;
            for (R receiver : receiversAbove.apply(pos)) receivers.add(receiver);
            for (P neighbor : neighbors.apply(pos))
                if (!visited.contains(neighbor)) pending.add(neighbor);
        }
        return new ArrayList<>(receivers);
    }

    /** Each receiver gets a distinct unused side; a hatch cannot replace the DHD's output. */
    public static <S, R> Map<S, R> assign(List<S> freeSides, List<R> receivers) {
        Map<S, R> routes = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(freeSides.size(), receivers.size()); i++)
            routes.put(freeSides.get(i), receivers.get(i));
        return routes;
    }
}
