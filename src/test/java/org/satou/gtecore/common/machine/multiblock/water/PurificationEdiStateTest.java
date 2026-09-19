package org.satou.gtecore.common.machine.multiblock.water;

/** Dependency-free regression runner; execute with scripts/test-purification-edi.ps1. */
public final class PurificationEdiStateTest {

    public static void main(String[] args) {
        signalThresholds();
        tinyLoadAccumulation();
        productionParallelLimits();
        regenerationAndTailCleanup();
        explicitCommitAccounting();
        reloadAndInvalidInputProtection();
        System.out.println("PurificationEdiState: 6 regression scenarios passed");
    }

    private static void signalThresholds() {
        var state = new PurificationEdiState();
        check(state.signal() == 0 && state.remainingCapacity() == PurificationEdiState.CAPACITY, "initial state");
        for (int signal = 1; signal <= 15; signal++) {
            long boundary = PurificationEdiState.CAPACITY * signal / 15;
            state.restore(new PurificationEdiState.Snapshot(boundary - 1));
            check(state.signal() == signal - 1, "signal advanced before boundary");
            state.produced(1);
            check(state.signal() == signal, "signal did not advance at boundary");
        }
        state.restore(new PurificationEdiState.Snapshot(959999));
        check(state.signal() < 12, "regeneration latch triggered early");
        state.produced(1);
        check(state.signal() == 12, "regeneration latch threshold");
        state.restore(new PurificationEdiState.Snapshot(320000));
        check(state.signal() > 3, "production latch triggered early");
        state.regenerate(1);
        check(state.signal() == 3, "production latch threshold");
    }

    private static void tinyLoadAccumulation() {
        var state = new PurificationEdiState();
        for (int i = 0; i < 79999; i++) state.produced(1);
        check(state.load() == 79999 && state.signal() == 0, "small loads were rounded away");
        state.produced(1);
        check(state.load() == 80000 && state.signal() == 1, "accumulated small loads did not reach threshold");
        state.regenerate(79999);
        check(state.load() == 1 && state.signal() == 0, "signal zero lost remaining load");
    }

    private static void productionParallelLimits() {
        var state = new PurificationEdiState();
        check(state.limitProductionParallel(1000, 256) == 256, "requested parallel cap");
        check(state.limitProductionParallel(1000, Integer.MAX_VALUE) == 1200, "capacity parallel cap");
        check(state.limitProductionParallel(Long.MAX_VALUE, Integer.MAX_VALUE) == 0, "large amount overflow");
        state.produced(PurificationEdiState.CAPACITY - 2501);
        check(state.limitProductionParallel(1000, 256) == 2, "fractional production parallel rounded up");
        state.produced(2000);
        check(state.limitProductionParallel(1000, 256) == 0, "insufficient space allowed production");
        state.produced(501);
        check(state.signal() == 15 && state.remainingCapacity() == 0, "full state");
        check(state.limitProductionParallel(1, 1) == 0, "full membrane allowed production");
        var full = state.snapshot();
        invalid(() -> state.produced(1));
        invalid(() -> state.produced(Long.MAX_VALUE));
        check(state.snapshot().equals(full), "rejected production changed full load");
        check(state.limitRegenerationParallel(1000, 256) == 256, "full membrane blocked regeneration");
    }

    private static void regenerationAndTailCleanup() {
        var state = new PurificationEdiState();
        check(state.limitRegenerationParallel(1000, 256) == 0, "empty membrane requested regeneration");
        state.produced(2501);
        check(state.limitRegenerationParallel(1000, 2) == 2, "requested regeneration cap");
        check(state.limitRegenerationParallel(1000, 256) == 3, "tail regeneration must round up");
        check(state.regenerate(2000) == 2000 && state.load() == 501, "paid regeneration accounting");
        check(state.limitRegenerationParallel(1000, 256) == 1, "tail cannot be cleaned");
        check(state.regenerate(1000) == 501 && state.load() == 0, "tail cleanup did not cap removal");
        check(state.regenerate(Long.MAX_VALUE) == 0, "empty regeneration removed load");
        state.produced(1);
        check(state.limitRegenerationParallel(Long.MAX_VALUE, Integer.MAX_VALUE) == 1, "ceil overflow");
        check(state.regenerate(Long.MAX_VALUE) == 1 && state.load() == 0, "large cleanup budget overflow");
        state.produced(2000);
        check(state.limitRegenerationParallel(1000, 256) == 2, "exact division added an unnecessary parallel");
    }

    private static void explicitCommitAccounting() {
        var state = new PurificationEdiState();
        state.produced(12345);
        var before = state.snapshot();
        for (int i = 0; i < 100; i++) {
            state.load();
            state.remainingCapacity();
            state.signal();
            state.limitProductionParallel(1000, 256);
            state.limitRegenerationParallel(1000, 256);
        }
        state.produced(0);
        check(state.regenerate(0) == 0 && state.snapshot().equals(before), "planning or idle reads changed load");
        state.produced(1000);
        check(state.load() == 13345, "production commit did not account exact water");
        check(state.regenerate(1000) == 1000 && state.snapshot().equals(before), "regeneration commit mismatch");
    }

    private static void reloadAndInvalidInputProtection() {
        var state = new PurificationEdiState();
        state.produced(1);
        var restored = new PurificationEdiState();
        restored.restore(state.snapshot());
        check(restored.load() == 1 && restored.signal() == 0, "reload lost sub-signal residue");
        state.produced(PurificationEdiState.CAPACITY - 1);
        restored.restore(state.snapshot());
        check(restored.load() == PurificationEdiState.CAPACITY && restored.signal() == 15, "full reload");
        restored.restore(new PurificationEdiState.Snapshot(0));
        check(restored.load() == 0, "empty reload");
        invalid(() -> new PurificationEdiState.Snapshot(-1));
        invalid(() -> new PurificationEdiState.Snapshot(PurificationEdiState.CAPACITY + 1));
        invalid(() -> new PurificationEdiState.Snapshot(Long.MAX_VALUE));
        invalid(() -> new PurificationEdiState.Snapshot(Long.MIN_VALUE));
        invalid(() -> restored.produced(Long.MAX_VALUE));
        invalid(() -> restored.produced(-1));
        invalid(() -> restored.regenerate(-1));
        for (long amount : new long[] { 0, -1, Long.MIN_VALUE }) {
            invalid(() -> restored.limitProductionParallel(amount, 1));
            invalid(() -> restored.limitRegenerationParallel(amount, 1));
        }
        invalid(() -> restored.limitProductionParallel(1, -1));
        invalid(() -> restored.limitRegenerationParallel(1, -1));
        check(restored.limitProductionParallel(1, 0) == 0 && restored.limitRegenerationParallel(1, 0) == 0,
                "zero parallel request");
        check(restored.load() == 0, "invalid mutation changed load");
    }

    private static void invalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid EDI state was accepted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
