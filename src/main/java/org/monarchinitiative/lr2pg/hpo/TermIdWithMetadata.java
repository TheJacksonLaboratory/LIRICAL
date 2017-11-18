package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;


/**
 * A {@link TermId} with Frequency and Onset metadata
 */
public interface TermIdWithMetadata extends TermId {



    /**
     * @return The annotated {@link TermId}.
     */
    public TermId getTermId();

    /**
     * @return The annotating {@link HpoFrequency}.
     */
    public HpoFrequency getFrequency() ;


    public HpoOnset getOnset() ;

    /**
     * Query for term ID's prefix.
     *
     * @return {@link TermPrefix} of the identifier
     */
    TermPrefix getPrefix();

    /**
     * Query for term ID.
     */
    String getId();

    /**
     * Return the full term ID including prefix.
     *
     * @return The full Id.
     */
    String getIdWithPrefix();



}

