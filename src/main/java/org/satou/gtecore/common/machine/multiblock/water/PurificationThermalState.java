package org.satou.gtecore.common.machine.multiblock.water;

/**
 * Deterministic, tick-based temperature control for first-stage water purification.
 * Temperature uses tenths of a degree Celsius. No wall clock or game objects are involved.
 */
public final class PurificationThermalState {

    public static final int TARGET_INTERVAL_TICKS = 60 * 20;
    public static final int GRACE_TICKS = 15 * 20;
    public static final int FULL_EFFICIENCY_TICKS = 600 * 20;
    public static final int MIN_CENTER_C = 45;
    public static final int MAX_CENTER_C = 85;
    public static final int HALF_BAND_DECI_C = 50;

    private int temperatureDeciC = 650;
    private int targetCenterC = 65;
    private int phaseTicks;
    private int outOfRangeTicks;
    private int stableTicks;
    private boolean faulted;
    private int outputRemainder;

    /**
     * Call exactly once per server tick. Working means the recipe actually made progress.
     * Redstone heats by 0.1 C/tick; no redstone cools at the same rate, including while stopped.
     * Idle resets continuous stable progress, but freezes target phase and the used grace budget.
     * Unloading should not call this method: restoring a snapshot resumes the exact saved state.
     *
     * @param targetChoice arbitrary deterministic/random integer, consumed only at target changes
     * @return true only on the tick that latches a new fault
     */
    public boolean tick(boolean working, boolean heating, int targetChoice) {
        temperatureDeciC = Math.clamp(temperatureDeciC + (heating ? 1 : -1), 0, 1000);
        if (faulted) return false;
        if (!working) {
            stableTicks = 0;
            return false;
        }

        if (++phaseTicks == TARGET_INTERVAL_TICKS) {
            phaseTicks = 0;
            changeTarget(targetChoice);
        }
        if (isInRange()) {
            outOfRangeTicks = 0;
            stableTicks = Math.min(stableTicks + 1, FULL_EFFICIENCY_TICKS);
        } else if (++outOfRangeTicks > GRACE_TICKS) {
            faulted = true;
            stableTicks = 0;
            outputRemainder = 0;
            return true;
        }
        return false;
    }

    private void changeTarget(int choice) {
        int lower = Math.max(MIN_CENTER_C, targetCenterC - 10);
        int upper = Math.min(MAX_CENTER_C, targetCenterC + 10);
        // There are (upper - lower) candidates after excluding the current center.
        int next = lower + Math.floorMod(choice, upper - lower);
        if (next >= targetCenterC) next++;
        targetCenterC = next;
    }

    /** Only the machine's successful maintenance/repair path may clear the fault latch. */
    public void clearFault() {
        if (!faulted) return;
        faulted = false;
        stableTicks = 0;
        outOfRangeTicks = 0;
        outputRemainder = 0;
    }

    /**
     * Sample at recipe completion, never when selecting a recipe. Integer remainder preserves
     * fractional millibuckets across batches. Call only when committing this output, not probing.
     * The caller must persist the remainder along with all other state.
     */
    public int scaleOutput(int nominalAmount) {
        if (nominalAmount < 0) throw new IllegalArgumentException("Negative output amount");
        if (faulted || stableTicks == 0) return 0;
        long numerator = (long) nominalAmount * stableTicks + outputRemainder;
        outputRemainder = (int) (numerator % FULL_EFFICIENCY_TICKS);
        return (int) (numerator / FULL_EFFICIENCY_TICKS);
    }

    public int temperatureDeciC() { return temperatureDeciC; }

    public int lowerDeciC() { return targetCenterC * 10 - HALF_BAND_DECI_C; }

    public int upperDeciC() { return targetCenterC * 10 + HALF_BAND_DECI_C; }

    public int stableTicks() { return stableTicks; }

    public int phaseTicks() { return phaseTicks; }

    public int outOfRangeTicks() { return outOfRangeTicks; }

    public boolean isFaulted() { return faulted; }

    public boolean isInRange() { return temperatureDeciC >= lowerDeciC() && temperatureDeciC <= upperDeciC(); }

    public double efficiency() { return faulted ? 0.0 : (double) stableTicks / FULL_EFFICIENCY_TICKS; }

    public Snapshot snapshot() {
        return new Snapshot(temperatureDeciC, targetCenterC, phaseTicks, outOfRangeTicks, stableTicks,
                faulted, outputRemainder);
    }

    public void restore(Snapshot snapshot) {
        temperatureDeciC = snapshot.temperatureDeciC();
        targetCenterC = snapshot.targetCenterC();
        phaseTicks = snapshot.phaseTicks();
        outOfRangeTicks = snapshot.outOfRangeTicks();
        stableTicks = snapshot.stableTicks();
        faulted = snapshot.faulted();
        outputRemainder = snapshot.outputRemainder();
    }

    public record Snapshot(int temperatureDeciC, int targetCenterC, int phaseTicks, int outOfRangeTicks,
                           int stableTicks, boolean faulted, int outputRemainder) {

        public Snapshot {
            if (temperatureDeciC < 0 || temperatureDeciC > 1000 ||
                    targetCenterC < MIN_CENTER_C || targetCenterC > MAX_CENTER_C ||
                    phaseTicks < 0 || phaseTicks >= TARGET_INTERVAL_TICKS ||
                    outOfRangeTicks < 0 || outOfRangeTicks > GRACE_TICKS + 1 ||
                    stableTicks < 0 || stableTicks > FULL_EFFICIENCY_TICKS ||
                    outputRemainder < 0 || outputRemainder >= FULL_EFFICIENCY_TICKS ||
                    (faulted && (stableTicks != 0 || outputRemainder != 0))) {
                throw new IllegalArgumentException("Invalid purification thermal snapshot");
            }
        }
    }
}
