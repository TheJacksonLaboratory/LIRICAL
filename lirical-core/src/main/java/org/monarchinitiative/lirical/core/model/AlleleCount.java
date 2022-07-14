package org.monarchinitiative.lirical.core.model;

import java.util.Objects;

public class AlleleCount {

    // the most common cases
    private static final AlleleCount ZERO_ZERO = new AlleleCount((byte) 0, (byte) 0);
    private static final AlleleCount ZERO_ONE = new AlleleCount((byte) 0, (byte) 1);
    private static final AlleleCount ZERO_TWO = new AlleleCount((byte) 0, (byte) 2);
    private static final AlleleCount ONE_ONE = new AlleleCount((byte) 1, (byte) 1);

    public static AlleleCount zeroZero() {
        return ZERO_ZERO;
    }

    public static AlleleCount zeroOne() {
        return ZERO_ONE;
    }

    public static AlleleCount zeroTwo() {
        return ZERO_TWO;
    }

    public static AlleleCount oneOne() {
        return ONE_ONE;
    }

    public static AlleleCount of(byte ref, byte alt) {
        return new AlleleCount(ref, alt);
    }

    public static AlleleCount of(int ref, int alt) {
        if (ref > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Ref count (%d) must be <=127".formatted(ref));
        }
        if (alt > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Alt count (%d) must be <=127".formatted(alt));
        }
        return new AlleleCount((byte) ref, (byte) alt);
    }

    private final byte ref, alt;

    private AlleleCount(byte ref, byte alt) {
        this.ref = ref;
        this.alt = alt;
    }

    public byte ref() {
        return ref;
    }

    public byte alt() {
        return alt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlleleCount count = (AlleleCount) o;
        return ref == count.ref && alt == count.alt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, alt);
    }

    @Override
    public String toString() {
        return "AlleleCount{" +
                "ref=" + ref +
                ", alt=" + alt +
                '}';
    }
}
