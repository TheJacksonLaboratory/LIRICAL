package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Represent an HPO Term together with a Frequency and an Onset. This is intended to be used to represent a disease
 * annotation.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2017-11-24)
 */
public class ImmutableTermIdWithMetadata implements TermIdWithMetadata {
    private static final Logger logger = LogManager.getLogger();
    /** The annotated {@link TermId}. */
    private final TermId termId;

    /** The {@link HpoFrequency}. */
    private final HpoFrequency frequency;
    /** The characteristic age of onset of a feature in a certain disease. */
    private final HpoOnset onset;
    /** If no information is available, then assume that the feature is always present! */
    private static final HpoFrequency DEFAULT_HPO_FREQUENCY=HpoFrequency.ALWAYS_PRESENT;
    /** If no onset information is available, use the Onset term "Onset" (HP:0003674), which is the root of the subontology for onset. */
    private static final HpoOnset DEFAULT_HPO_ONSET=HpoOnset.ONSET;


    /**
     * Constructor.
     *
     * @param termId Annotated {@link TermId}.
     * @param frequency That the term is annotated with.
     */
    public ImmutableTermIdWithMetadata(TermId termId, HpoFrequency frequency, HpoOnset onset) {
        this.termId = termId;
        this.frequency = frequency!=null?frequency:DEFAULT_HPO_FREQUENCY;
        this.onset=onset!=null?onset:DEFAULT_HPO_ONSET;
    }


    public ImmutableTermIdWithMetadata(TermId t) {
        this.termId=t;
        this.frequency = DEFAULT_HPO_FREQUENCY;
        this.onset=DEFAULT_HPO_ONSET;
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

    /**
     * Return the full term ID including prefix.
     *
     * @return The full Id.
     */
    @Override
    public String getIdWithPrefix() {
        return String.format(this.termId.getIdWithPrefix());
    }

    @Override
    public TermPrefix getPrefix() { return this.termId.getPrefix();}


    @Override
    public String getId() { return this.termId.getId();}

    /** Objects are equal if the three components are equal.
     * Note that the constructor guarantees that the the TermId, the Frequency, and the Onset are not null.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (! (o instanceof TermIdWithMetadata)) return false;
        TermIdWithMetadata otherTIDM = (TermIdWithMetadata) o;

        return termId.getId().equals(otherTIDM.getId()) &&
                frequency.equals(otherTIDM.getFrequency()) &&
                onset.equals(otherTIDM.getOnset());

    }

    @Override
    public int compareTo(TermId other) {
        return 1;
    }


}
