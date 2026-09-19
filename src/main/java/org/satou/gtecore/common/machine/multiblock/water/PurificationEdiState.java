package org.satou.gtecore.common.machine.multiblock.water;

import java.util.Objects;

/** Exact EDI membrane load in treated-water mB; batch execution belongs to the controller. */
public final class PurificationEdiState {

    public static final long CAPACITY = 1_200_000L;

    private long load;

    public long load() { return load; }

    public long remainingCapacity() { return CAPACITY - load; }

    /** Floor quantization preserves small loads; strength 15 means completely full. */
    public int signal() { return (int) (load * 15 / CAPACITY); }

    public int limitProductionParallel(long loadPerParallel, int requested) {
        validateParallel(loadPerParallel, requested);
        return (int) Math.min(requested, remainingCapacity() / loadPerParallel);
    }

    /** Include one final paid parallel to remove a residue smaller than its cleaning budget. */
    public int limitRegenerationParallel(long cleanPerParallel, int requested) {
        validateParallel(cleanPerParallel, requested);
        long needed = load / cleanPerParallel + (load % cleanPerParallel == 0 ? 0 : 1);
        return (int) Math.min(requested, needed);
    }

    private static void validateParallel(long amountPerParallel, int requested) {
        if (amountPerParallel <= 0 || requested < 0) {
            throw new IllegalArgumentException("Positive amount per parallel and nonnegative parallel count required");
        }
    }

    /** Commit only after the corresponding output water has been successfully emitted. */
    public void produced(long amount) {
        if (amount < 0 || amount > remainingCapacity()) {
            throw new IllegalArgumentException("Produced water exceeds remaining EDI capacity");
        }
        load += amount;
    }

    /** Commit only after a paid regeneration batch completes; return the actual load removed. */
    public long regenerate(long budget) {
        if (budget < 0) throw new IllegalArgumentException("Regeneration budget must be nonnegative");
        long removed = Math.min(load, budget);
        load -= removed;
        return removed;
    }

    public Snapshot snapshot() { return new Snapshot(load); }

    public void restore(Snapshot snapshot) {
        load = Objects.requireNonNull(snapshot, "snapshot").load;
    }

    public record Snapshot(long load) {

        public Snapshot {
            if (load < 0 || load > CAPACITY) {
                throw new IllegalArgumentException("Persisted EDI load is outside membrane capacity");
            }
        }
    }
}
