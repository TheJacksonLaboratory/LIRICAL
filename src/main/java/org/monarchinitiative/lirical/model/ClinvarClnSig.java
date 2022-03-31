package org.monarchinitiative.lirical.model;

public enum ClinvarClnSig {

    PATHOGENIC,
    LIKELY_PATHOGENIC,
    PATHOGENIC_OR_LIKELY_PATHOGENIC,

    UNCERTAIN_SIGNIFICANCE,
    CONFLICTING_PATHOGENICITY_INTERPRETATIONS,

    BENIGN,
    LIKELY_BENIGN,
    BENIGN_OR_LIKELY_BENIGN,

    AFFECTS,
    ASSOCIATION,
    DRUG_RESPONSE,
    PROTECTIVE,
    RISK_FACTOR,

    OTHER,
    NOT_PROVIDED;

    public boolean isPathogenicOrLikelyPathogenic() {
        return switch (this) {
            case PATHOGENIC, LIKELY_PATHOGENIC, PATHOGENIC_OR_LIKELY_PATHOGENIC -> true;
            default -> false;
        };
    }
}
