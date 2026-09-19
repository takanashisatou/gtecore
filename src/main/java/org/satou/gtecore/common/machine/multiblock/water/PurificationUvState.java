package org.satou.gtecore.common.machine.multiblock.water;

import java.util.Objects;

/** Deterministic UV dose accounting; only successful processing ticks deliver dose. */
public final class PurificationUvState {

    public static final int MAX_LAMPS = 4;
    public static final int PARALLELS_PER_LAMP = 64;

    private Plan plan;
    private long deliveredDose;

    public static Plan plan(int parallels, int lamps, int powerPercent, int baseDurationTicks,
                            int dosePerParallel) {
        validateInputs(parallels, lamps, powerPercent, baseDurationTicks, dosePerParallel);
        long required = calculateDose(parallels, dosePerParallel);
        long rate = (long) lamps * PARALLELS_PER_LAMP * powerPercent;
        return new Plan(parallels, lamps, powerPercent, baseDurationTicks, dosePerParallel,
                required, rate, calculateDuration(required, rate, baseDurationTicks));
    }

    private static void validateInputs(int parallels, int lamps, int powerPercent, int baseDurationTicks,
                                       int dosePerParallel) {
        if (parallels <= 0 || lamps < 1 || lamps > MAX_LAMPS || baseDurationTicks <= 0 || dosePerParallel <= 0) {
            throw new IllegalArgumentException("Positive parallel, duration and dose values and 1-4 lamps required");
        }
        if (powerPercent < 25 || powerPercent > 100 || powerPercent % 25 != 0) {
            throw new IllegalArgumentException("UV power must be 25, 50, 75 or 100 percent");
        }
    }

    private static long calculateDose(int parallels, int dosePerParallel) {
        try {
            return Math.multiplyExact(Math.multiplyExact((long) parallels, dosePerParallel), 100L);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Required UV dose exceeds the supported range", overflow);
        }
    }

    private static int calculateDuration(long required, long rate, int baseDurationTicks) {
        long ticks = required / rate + (required % rate == 0 ? 0 : 1);
        if (ticks > Integer.MAX_VALUE) throw new IllegalArgumentException("UV duration exceeds the supported range");
        return Math.max(baseDurationTicks, (int) ticks);
    }

    public void begin(Plan nextPlan) {
        plan = Objects.requireNonNull(nextPlan, "plan");
        deliveredDose = 0;
    }

    /** Call once after a successful working tick, never for idle time or unloaded time. */
    public void tick() {
        if (plan != null) deliveredDose += Math.min(plan.requiredDose - deliveredDose, plan.dosePerTick);
    }

    public void clear() {
        plan = null;
        deliveredDose = 0;
    }

    public boolean active() { return plan != null; }

    /** Dose completion only; the controller must also enforce the planned recipe duration. */
    public boolean complete() { return plan != null && deliveredDose == plan.requiredDose; }

    public long deliveredDose() { return deliveredDose; }

    public long requiredDose() { return plan == null ? 0 : plan.requiredDose; }

    public long dosePerTick() { return plan == null ? 0 : plan.dosePerTick; }

    public Plan plan() { return plan; }

    public double fraction() { return plan == null ? 0.0 : (double) deliveredDose / plan.requiredDose; }

    public Snapshot snapshot() { return new Snapshot(plan, deliveredDose); }

    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        plan = snapshot.plan;
        deliveredDose = snapshot.deliveredDose;
    }

    /** Derived fields are validated even when loading a persisted plan directly. */
    public record Plan(int parallels, int lamps, int powerPercent, int baseDurationTicks, int dosePerParallel,
                       long requiredDose, long dosePerTick, int durationTicks) {

        public Plan {
            validateInputs(parallels, lamps, powerPercent, baseDurationTicks, dosePerParallel);
            long expectedDose = calculateDose(parallels, dosePerParallel);
            long expectedRate = (long) lamps * PARALLELS_PER_LAMP * powerPercent;
            if (requiredDose != expectedDose || dosePerTick != expectedRate ||
                    durationTicks != calculateDuration(expectedDose, expectedRate, baseDurationTicks)) {
                throw new IllegalArgumentException("Persisted UV plan does not match its inputs");
            }
        }
    }

    public record Snapshot(Plan plan, long deliveredDose) {

        public Snapshot {
            if (deliveredDose < 0 || (plan == null ? deliveredDose != 0 : deliveredDose > plan.requiredDose)) {
                throw new IllegalArgumentException("Delivered UV dose is outside the active plan");
            }
        }
    }
}
