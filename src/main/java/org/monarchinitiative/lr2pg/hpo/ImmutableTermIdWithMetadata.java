package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;

public class ImmutableTermIdWithMetadata implements TermIdWithMetadata {

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
    public ImmutableTermIdWithMetadata(TermId termId, HpoFrequency frequency, HpoOnset onset) {
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
     * If frequency are both null and/or onset are both null, we are still equal!
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (! (o instanceof TermIdWithMetadata)) return false;
        TermIdWithMetadata otherTIDM = (TermIdWithMetadata) o;
        if (! this.termId.equals(otherTIDM.getTermId())) return false;
        if ((this.frequency == null && otherTIDM.getFrequency() == null &&
        this.onset==null && otherTIDM.getOnset()==null) ) return true; // only tid initialized
        if (this.onset == null) {
            if (otherTIDM != null) return false;
        } else {
            if (otherTIDM.getOnset()==null) return false;
            if (! this.onset.equals(otherTIDM.getOnset()) ) return  false;
        }
        // if we get here, tid and onset are equal
        if (this.frequency == null) {
            if (otherTIDM.getFrequency() != null) return false;
        } else {
            if (otherTIDM.getFrequency()==null) return false;
            return this.frequency.equals(otherTIDM.getFrequency());
        }
        return true;
    }

    @Override
    public int compareTo(TermId other) {
        return 1;
    }


}
