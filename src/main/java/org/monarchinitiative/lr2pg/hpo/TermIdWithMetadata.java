package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.ontology.data.TermId;


/**
 * A {@link TermId} with Frequency and Onset metadata
 */
public class TermIdWithMetadata {

    /** The annotated {@link TermId}. */
    private final TermId termId;

    /** The {@link HpoFrequency}. */
    private final HpoFrequency frequency;
    /** The characteristic age of onset of a feature in a certain disease. */
    private final HpoOnset onset;

    /**
     * Constructor.
     *
     * @param termId Annotated {@link TermId}.
     * @param frequency That the term is annotated with.
     */
    public TermIdWithMetadata(TermId termId, HpoFrequency frequency, HpoOnset onset) {
        this.termId = termId;
        this.frequency = frequency;
        this.onset=onset;
    }

    /**
     * @return The annotated {@link TermId}.
     */
    public TermId getTermId() {
        return termId;
    }

    /**
     * @return The annotating {@link HpoFrequency}.
     */
    public HpoFrequency getFrequency() {
        return frequency;
    }


    public HpoOnset getOnset() { return onset;    }

    @Override
    public String toString() {
        return "TermIdWithMetadata [termId=" + termId + ", frequency=" + frequency + ", onset="+ onset + "]";
    }

}

