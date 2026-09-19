package org.satou.gtecore.common.machine.multiblock.water;

/** Dependency-free regression runner; execute with scripts/test-purification-thermal.ps1. */
public final class PurificationThermalStateTest {

    public static void main(String[] args) {
        targetChangesAtSixtySeconds();
        graceAndRepairBoundaries();
        tenMinutesOfActualStableWork();
        idleAndReloadCannotRefreshGrace();
        fractionalOutputsSurviveReload();
        allTargetsRemainReachable();
        System.out.println("PurificationThermalState: 6 regression scenarios passed");
    }

    private static void targetChangesAtSixtySeconds() {
        var state = new PurificationThermalState();
        for (int tick = 1; tick < 1200; tick++) state.tick(true, tick % 2 == 0, 0);
        check(state.lowerDeciC() == 600 && state.phaseTicks() == 1199, "target changed too soon");
        state.tick(true, true, 0);
        check(state.lowerDeciC() == 500 && state.phaseTicks() == 0, "target must change at tick 1200");
        check(state.outOfRangeTicks() == 1 && state.stableTicks() == 1199, "change must freeze unsafe progress");
    }

    private static void graceAndRepairBoundaries() {
        var state = new PurificationThermalState();
        state.restore(new PurificationThermalState.Snapshot(701, 65, 0, 0, 500, false, 0));
        for (int tick = 0; tick < 300; tick++) {
            check(!state.tick(true, true, 0), "fault during 15-second grace");
        }
        check(!state.isFaulted() && state.stableTicks() == 500, "grace must freeze stable progress");
        check(state.tick(true, true, 0), "301st unsafe tick must fault");
        check(state.isFaulted() && state.efficiency() == 0 && state.scaleOutput(1000) == 0, "fault must zero yield");
        for (int tick = 0; tick < 350; tick++) state.tick(false, false, 0);
        check(state.isInRange() && state.isFaulted(), "temperature recovery must not repair fault");
        check(!state.tick(true, false, 0), "fault event must only fire once");
        state.clearFault();
        check(!state.isFaulted() && state.stableTicks() == 0 && state.outOfRangeTicks() == 0, "repair reset");
        state.tick(true, true, 0);
        check(state.stableTicks() == 1, "repaired machine should restart stable progress");

        state.restore(new PurificationThermalState.Snapshot(701, 65, 0, 300, 500, false, 0));
        check(!state.tick(true, false, 0) && state.outOfRangeTicks() == 0, "inclusive boundary permits last-moment rescue");
    }

    private static void tenMinutesOfActualStableWork() {
        var state = new PurificationThermalState();
        for (int tick = 1; tick <= 12000; tick++) {
            int choice = state.snapshot().targetCenterC() == 65 ? 10 : 9;
            check(!state.tick(true, tick % 2 == 0, choice), "safe operation faulted");
            check(state.stableTicks() == tick, "stable tick was lost");
            if (tick < 12000) check(state.efficiency() < 1, "100% reached before 600 seconds");
        }
        check(state.efficiency() == 1 && state.scaleOutput(Integer.MAX_VALUE) == Integer.MAX_VALUE,
                "600 seconds must yield 100%, including large parallel outputs");
        state.tick(false, false, 0);
        check(state.efficiency() == 0, "idle must reset continuous stable work");
        for (int tick = 0; tick < 12000; tick++) state.tick(false, tick % 2 == 0, 0);
        check(state.efficiency() == 0, "idle must not farm efficiency");
    }

    private static void idleAndReloadCannotRefreshGrace() {
        var state = new PurificationThermalState();
        state.restore(new PurificationThermalState.Snapshot(800, 65, 1199, 299, 99, false, 13));
        state.tick(false, true, 0);
        var restored = new PurificationThermalState();
        restored.restore(state.snapshot());
        check(restored.snapshot().equals(state.snapshot()), "reload must preserve every field");
        check(restored.phaseTicks() == 1199 && restored.outOfRangeTicks() == 299, "idle refreshed timers");
        restored.tick(true, true, 0);
        check(!restored.isFaulted() && restored.outOfRangeTicks() == 300, "target switch refreshed grace");
        check(restored.tick(true, true, 0), "restart escaped timeout");
        state.restore(restored.snapshot());
        state.tick(false, false, 0);
        check(state.isFaulted(), "reload/idle cleared fault");
    }

    private static void fractionalOutputsSurviveReload() {
        var state = new PurificationThermalState();
        state.restore(new PurificationThermalState.Snapshot(650, 65, 0, 0, 1, false, 0));
        int total = 0;
        for (int batch = 0; batch < 12000; batch++) {
            total += state.scaleOutput(1);
            var reloaded = new PurificationThermalState();
            reloaded.restore(state.snapshot());
            state = reloaded;
        }
        check(total == 1 && state.snapshot().outputRemainder() == 0, "fractional low yield was lost");
    }

    private static void allTargetsRemainReachable() {
        var state = new PurificationThermalState();
        for (int center = 45; center <= 85; center++) {
            for (int choice = -30; choice <= 30; choice++) {
                state.restore(new PurificationThermalState.Snapshot(center * 10, center, 1199, 0, 0, false, 0));
                state.tick(true, true, choice);
                int next = state.snapshot().targetCenterC();
                check(next >= 45 && next <= 85 && next != center && Math.abs(next - center) <= 10,
                        "target out of bounds, unchanged, or unreachable");
                // At 2 C/s even opposite safe-band edges are less than 15 seconds apart.
                check(Math.abs(next - center) + 10 < 30, "target cannot be reached within grace");
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
