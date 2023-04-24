package org.monarchinitiative.lirical.core.model;

import java.util.Objects;

/**
 * A container for associating sample id and the {@link AlleleCount}.
 */
public class SampleAlleleCount {

    private final String sampleId;
    private final AlleleCount alleleCount;

    public static SampleAlleleCount of(String sampleId, AlleleCount alleleCount) {
        return new SampleAlleleCount(sampleId, alleleCount);
    }

    private SampleAlleleCount(String sampleId, AlleleCount alleleCount) {
        this.sampleId = Objects.requireNonNull(sampleId);
        this.alleleCount = Objects.requireNonNull(alleleCount);
    }

    public String getSampleId() {
        return sampleId;
    }

    public AlleleCount getAlleleCount() {
        return alleleCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SampleAlleleCount that = (SampleAlleleCount) o;
        return Objects.equals(sampleId, that.sampleId) && Objects.equals(alleleCount, that.alleleCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleId, alleleCount);
    }

    @Override
    public String toString() {
        return "SampleAlleleCount{" +
                "sampleId='" + sampleId + '\'' +
                ", alleleCount=" + alleleCount +
                '}';
    }
}
