package org.monarchinitiative.lirical.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A subset of ClinVar allele data relevant for LIRICAL analysis.
 * <p>
 * We use the primary interpretation for prioritization and the allele ID for linking out
 * (e.g. <a href="https://www.ncbi.nlm.nih.gov/clinvar/?term=270003[alleleid]">here</a> for an allele ID <code>270003</code>)
 */
public class ClinVarAlleleData {

    private final ClinvarClnSig clinvarClnSig;
    private final Long alleleId; // we box since the alleleId is nullable.

    public static ClinVarAlleleData of(ClinvarClnSig clinvarClnSig, Long alleleId) {
        return new ClinVarAlleleData(clinvarClnSig, alleleId);
    }

    private ClinVarAlleleData(ClinvarClnSig clinvarClnSig, Long alleleId) {
        this.clinvarClnSig = Objects.requireNonNull(clinvarClnSig);
        this.alleleId = alleleId; // nullable
    }

    /**
     * @return the primary interpretation of the ClinVar data for the variant
     */
    public ClinvarClnSig getClinvarClnSig() {
        return clinvarClnSig;
    }

    /**
     * Get ClinVar <a href="https://www.ncbi.nlm.nih.gov/clinvar/docs/identifiers/#allele">allele ID</a>.
     * <p>
     * E.g.
     *
     *
     * @return an {@linkplain Optional} ClinVar allele ID {@linkplain Long} or an empty {@linkplain Optional}.
     */
    public Optional<Long> getAlleleId() {
        return Optional.ofNullable(alleleId);
    }

    /**
     * @return ClinVar allele ID as {@linkplain String}
     * @see #getAlleleId()
     */
    public Optional<String> getAlleleIdString() {
        return alleleId == null ? Optional.empty() : Optional.of(alleleId.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClinVarAlleleData that = (ClinVarAlleleData) o;
        return clinvarClnSig == that.clinvarClnSig && Objects.equals(alleleId, that.alleleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clinvarClnSig, alleleId);
    }

    @Override
    public String toString() {
        return "ClinVarAlleleData{" +
                "clinvarClnSig=" + clinvarClnSig +
                ", alleleId=" + alleleId +
                '}';
    }
}
