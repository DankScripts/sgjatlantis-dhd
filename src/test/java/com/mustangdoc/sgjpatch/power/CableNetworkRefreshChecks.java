package com.mustangdoc.sgjpatch.power;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;

public final class CableNetworkRefreshChecks {
    private CableNetworkRefreshChecks() {}

    public static void run() {
        BlockPos start = new BlockPos(0, 64, 0);
        Set<BlockPos> cable = Set.of(start, start.above(), start.above(2), start.above(3));
        var complete = CableNetworkRefresh.scan(start, 100, pos -> true, cable::contains);
        if (complete.status() != CableNetworkRefresh.ScanStatus.COMPLETE || !complete.cables().equals(cable))
            throw new AssertionError("A loaded cable bridge must be scanned completely");
        BlockPos unloaded = start.above(3);
        var incomplete = CableNetworkRefresh.scan(start, 100, pos -> !pos.equals(unloaded), cable::contains);
        if (incomplete.status() != CableNetworkRefresh.ScanStatus.UNLOADED || !unloaded.equals(incomplete.unloaded()))
            throw new AssertionError("An unloaded cable component must be deferred");
        var limited = CableNetworkRefresh.scan(start, 2, pos -> true, cable::contains);
        if (limited.status() != CableNetworkRefresh.ScanStatus.LIMIT)
            throw new AssertionError("A truncated native traversal must not be rebuilt");
        if (!CableNetworkRefresh.allNetworksMissing(List.of(10, 10), id -> false)
                || CableNetworkRefresh.allNetworksMissing(List.of(10, 20), id -> id == 20)
                || CableNetworkRefresh.allNetworksMissing(List.of(), id -> false))
            throw new AssertionError("Refresh must not remove an existing SGJourney network");
        System.out.println("PASS: missing cable refresh checks loaded components, native limits and existing networks.");
    }
}
