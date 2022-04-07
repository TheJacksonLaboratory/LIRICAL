package org.monarchinitiative.lirical.core.likelihoodratio;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoSubOntologyRootTermIds;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * Convenience class for returning a term and its corresponding frequency
 */
record Term2Freq(TermId termId, double frequency) {

    boolean nonRootCommonAncestor() {
        return !termId.equals(HpoSubOntologyRootTermIds.PHENOTYPIC_ABNORMALITY);
    }


}
