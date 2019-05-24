package org.monarchinitiative.lirical.likelihoodratio;

import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * Convenience class for returning a term and its corresponding frequency
 */
public class Term2Freq {
    public final TermId tid;
    public final double frequency;

    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    public Term2Freq(TermId t, double f) {
        this.tid=t;
        this.frequency=f;
    }

    public boolean nonRootCommonAncestor() {
        return ! tid.equals(PHENOTYPIC_ABNORMALITY);
    }


}
