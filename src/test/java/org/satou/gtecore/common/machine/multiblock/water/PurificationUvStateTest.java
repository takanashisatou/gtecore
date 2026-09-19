package org.satou.gtecore.common.machine.multiblock.water;

/** Dependency-free regression runner; execute with scripts/test-purification-uv.ps1. */
public final class PurificationUvStateTest {

    public static void main(String[] args) {
        actualDoseAndCompletion();
        parallelAndPowerScaling();
        oxidantCapacity();
        reloadAndBatchReset();
        invalidAndOverflowProtection();
        System.out.println("PurificationUvState: 5 regression scenarios passed");
    }

    private static void actualDoseAndCompletion() {
        var state = new PurificationUvState();
        state.tick();
        check(!state.active() && !state.complete() && state.fraction() == 0, "empty state accumulated dose");
        state.begin(PurificationUvState.plan(65, 1, 100, 40, 40));
        check(state.plan().durationTicks() == 41, "fractional dose tick must round up");
        for (int tick = 0; tick < 40; tick++) {
            state.tick();
            check(!state.complete(), "UV dose completed prematurely");
        }
        check(state.deliveredDose() == 256000 && state.requiredDose() == 260000, "wrong dose accounting");
        var paused = state.snapshot();
        for (int sample = 0; sample < 100; sample++) state.fraction();
        check(state.snapshot().equals(paused), "reading idle state added dose");
        state.tick();
        check(state.complete() && state.deliveredDose() == 260000 && state.fraction() == 1, "final tick must cap dose");
        state.tick();
        check(state.deliveredDose() == 260000, "completed state must saturate");
        check(PurificationUvState.plan(1, 4, 100, 40, 40).durationTicks() == 40, "base duration must be enforced");
    }

    private static void parallelAndPowerScaling() {
        for (int lamps = 1; lamps <= 4; lamps++) {
            for (int power = 25; power <= 100; power += 25) {
                var plan = PurificationUvState.plan(256, lamps, power, 40, 40);
                check(plan.requiredDose() == 1024000, "power or lamp count changed required dose");
                check(plan.dosePerTick() == (long) lamps * 64 * power, "lamp/power scaling incorrect");
                check((long) plan.durationTicks() * plan.dosePerTick() >= plan.requiredDose(), "plan underdoses");
                if (plan.durationTicks() > 40) {
                    check((long) (plan.durationTicks() - 1) * plan.dosePerTick() < plan.requiredDose(), "duration too long");
                }
            }
        }
        check(PurificationUvState.plan(128, 1, 100, 40, 40).durationTicks() == 80, "parallel dose not scaled");
        check(PurificationUvState.plan(64, 1, 25, 40, 40).durationTicks() == 160, "quarter power must quadruple dose time");
    }

    private static void oxidantCapacity() {
        check(PurificationUvState.plan(64, 1, 100, 40, 40).durationTicks() == 40, "ozone full throughput");
        check(PurificationUvState.plan(64, 1, 100, 20, 40).durationTicks() == 40, "peroxide needs more lamp capacity");
        check(PurificationUvState.plan(64, 2, 100, 20, 40).durationTicks() == 20, "double lamps must restore peroxide throughput");
        check(PurificationUvState.plan(128, 4, 100, 20, 40).durationTicks() == 20, "four lamp peroxide capacity");
    }

    private static void reloadAndBatchReset() {
        var state = new PurificationUvState();
        var plan = PurificationUvState.plan(64, 2, 75, 20, 40);
        state.begin(plan);
        for (int tick = 0; tick < 11; tick++) state.tick();
        var restored = new PurificationUvState();
        restored.restore(state.snapshot());
        check(restored.snapshot().equals(state.snapshot()), "reload lost dose or plan");
        for (int tick = 11; tick < plan.durationTicks(); tick++) restored.tick();
        check(restored.complete(), "restored batch failed to finish");
        restored.begin(plan);
        check(restored.deliveredDose() == 0 && !restored.complete(), "new batch reused previous dose");
        restored.tick();
        restored.clear();
        restored.tick();
        check(!restored.active() && restored.plan() == null && restored.dosePerTick() == 0 &&
                restored.requiredDose() == 0 && restored.deliveredDose() == 0, "clear left batch state");
        state.restore(restored.snapshot());
        check(!state.active(), "empty snapshot failed to clear active state");
    }

    private static void invalidAndOverflowProtection() {
        invalid(() -> PurificationUvState.plan(0, 1, 100, 40, 40));
        invalid(() -> PurificationUvState.plan(1, 0, 100, 40, 40));
        invalid(() -> PurificationUvState.plan(1, 5, 100, 40, 40));
        for (int power : new int[] { -25, 0, 24, 26, 101, 125 }) {
            invalid(() -> PurificationUvState.plan(1, 1, power, 40, 40));
        }
        invalid(() -> PurificationUvState.plan(1, 1, 100, 0, 40));
        invalid(() -> PurificationUvState.plan(1, 1, 100, 40, 0));
        invalid(() -> PurificationUvState.plan(Integer.MAX_VALUE, 1, 25, 40, Integer.MAX_VALUE));
        invalid(() -> PurificationUvState.plan(Integer.MAX_VALUE, 1, 25, 40, 40));
        var large = PurificationUvState.plan(Integer.MAX_VALUE, 4, 100, 40, 40);
        check(large.requiredDose() == 8589934588000L && large.durationTicks() == 335544320, "valid large plan overflowed");
        var state = new PurificationUvState();
        state.restore(new PurificationUvState.Snapshot(large, large.requiredDose() - 1));
        state.tick();
        check(state.complete(), "large dose saturation failed");
        invalid(() -> new PurificationUvState.Snapshot(null, 1));
        invalid(() -> new PurificationUvState.Snapshot(large, -1));
        invalid(() -> new PurificationUvState.Snapshot(large, large.requiredDose() + 1));
        invalid(() -> new PurificationUvState.Plan(64, 1, 100, 40, 40, 0, 6400, 40));
        invalid(() -> new PurificationUvState.Plan(64, 1, 100, 40, 40, 256000, 0, 40));
        invalid(() -> new PurificationUvState.Plan(64, 1, 100, 40, 40, 256000, 6400, 39));
    }

    private static void invalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid UV state was accepted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
