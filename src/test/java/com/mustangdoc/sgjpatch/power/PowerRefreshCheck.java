package com.mustangdoc.sgjpatch.power;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;

public final class PowerRefreshCheck {
    private static void checkRefresh() {
        BlockPos start = new BlockPos(0, 64, 0);
        Set<BlockPos> cable = Set.of(start, start.above(), start.above(2), start.above(3));
        var complete = CableNetworkRefresh.scan(start, 100, pos -> true, cable::contains);
        if (complete.status() != CableNetworkRefresh.ScanStatus.COMPLETE || !complete.cables().equals(cable))
            throw new AssertionError("A loaded cable bridge must be scanned completely");
        BlockPos unloaded = start.above(3);
        var incomplete = CableNetworkRefresh.scan(start, 100, pos -> !pos.equals(unloaded), cable::contains);
        if (incomplete.status() != CableNetworkRefresh.ScanStatus.UNLOADED || !unloaded.equals(incomplete.unloaded()))
            throw new AssertionError("An unloaded cable component must be deferred");
        if (CableNetworkRefresh.scan(start, 2, pos -> true, cable::contains).status()
                != CableNetworkRefresh.ScanStatus.LIMIT)
            throw new AssertionError("A truncated native traversal must not be rebuilt");
        if (!CableNetworkRefresh.allNetworksMissing(List.of(10, 10), id -> false)
                || CableNetworkRefresh.allNetworksMissing(List.of(10, 20), id -> id == 20)
                || CableNetworkRefresh.allNetworksMissing(List.of(), id -> false))
            throw new AssertionError("Refresh must not remove an existing SGJourney network");
    }

    private static final class PowerBranch {
        private PowerBranch first, second;
        private long simulate() {
            return MixedPowerTransfer.run(this, 1, true, false, () -> {
                if (first == null) return 0;
                long sent = first.simulate();
                return sent != 0 ? sent : second.simulate();
            });
        }
    }

    private static void checkMixedPower() {
        Object source = new Object(), output = new Object();
        long accepted = MixedPowerTransfer.run(source, 3, false, true, () -> {
            long first = MixedPowerTransfer.run(output, 1, false, true, () -> 0);
            long second = MixedPowerTransfer.run(output, 2, false, true, () -> 2);
            return first + second;
        });
        if (accepted != 2) throw new AssertionError("A small rejected offer must not block a larger offer");
        Object root = new Object(), branchA = new Object(), branchB = new Object();
        Object junction = new Object(), nested = new Object(), primer = new Object();
        long simulated = MixedPowerTransfer.run(root, 10, true, false, () -> {
            long first = MixedPowerTransfer.run(branchA, 10, true, false, () -> {
                MixedPowerTransfer.run(primer, 10, true, false,
                        () -> MixedPowerTransfer.run(nested, 10, true, false,
                                () -> MixedPowerTransfer.run(branchA, 10, true, false, () -> 10)));
                return MixedPowerTransfer.run(junction, 10, true, false,
                        () -> MixedPowerTransfer.run(nested, 10, true, false,
                                () -> MixedPowerTransfer.run(branchA, 10, true, false, () -> 10)));
            });
            long second = MixedPowerTransfer.run(branchB, 10, true, false,
                    () -> MixedPowerTransfer.run(junction, 10, true, false,
                            () -> MixedPowerTransfer.run(nested, 10, true, false,
                                    () -> MixedPowerTransfer.run(branchA, 10, true, false, () -> 10))));
            return first + second;
        });
        if (simulated != 10) throw new AssertionError("Cached simulations must respect active routes");
        long[] remaining = {10};
        MixedPowerTransfer.run(root, 10, false, false, () -> {
            long first = MixedPowerTransfer.run(output, 10, true, false, () -> remaining[0]);
            MixedPowerTransfer.run(output, 10, false, false, () -> { remaining[0] = 0; return 10; });
            long second = MixedPowerTransfer.run(output, 10, true, false, () -> remaining[0]);
            if (first != 10 || second != 0) throw new AssertionError("Commit must invalidate cached probes");
            return 10;
        });
        PowerBranch[][] branches = new PowerBranch[24][2];
        for (PowerBranch[] layer : branches)
            for (int i = 0; i < layer.length; i++) layer[i] = new PowerBranch();
        for (int i = 0; i < branches.length - 1; i++)
            for (PowerBranch branch : branches[i]) {
                branch.first = branches[i + 1][0];
                branch.second = branches[i + 1][1];
            }
        long reachable = MixedPowerTransfer.run(root, 1, true, false, () -> {
            long first = branches[0][0].simulate();
            return first != 0 ? first : MixedPowerTransfer.run(output, 1, true, false, () -> 1);
        });
        if (reachable != 1) throw new AssertionError("Converging dead ends must not hide a reachable output");

        long[] stored = {0};
        SGJourneyEnergy receiver = new SGJourneyEnergy(600, 600, 0) {
            @Override public long receiveLongEnergy(long amount, boolean simulate) { return 0; }
            @Override public long receiveZeroPointEnergy(long amount, boolean simulate) {
                long received = Math.min(amount, 600 - stored[0]);
                if (!simulate) stored[0] += received;
                return received;
            }
            @Override public void onEnergyChanged(long difference, boolean simulate) {}
        };
        int regular = MixedPowerTransfer.run(root, 100, true, false,
                () -> MixedPowerTransfer.probeCableOutput(receiver, 100, true)) > 0 ? 1 : 0;
        long zeroPoint = MixedPowerTransfer.run(root, 100, true, true,
                () -> MixedPowerTransfer.probeCableOutput(receiver, 100, true));
        if (regular != 0 || zeroPoint != 100 || stored[0] != 0)
            throw new AssertionError("Native output probing must preserve the energy type without committing");
    }

    public static void main(String[] args) {
        checkRefresh();
        checkMixedPower();
        System.out.println("PASS: missing cable refresh, mixed-route caching and zero-point output probing.");
    }
}
