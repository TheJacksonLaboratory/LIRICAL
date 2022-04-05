package org.monarchinitiative.lirical.service;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HpoTermSanitizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HpoTermSanitizer.class);

    private final Ontology hpo;

    public HpoTermSanitizer(Ontology hpo) {
        this.hpo = hpo;
    }

    /**
     * The method performs 2 things:
     * <ul>
     *     <li>drops <code>termId</code> if it's absent from current HPO, and</li>
     *     <li>replaces the <code>termId</code> with current term if <code>termId</code> is obsolete.</li>
     * </ul>
     */
    public Optional<TermId> replaceIfObsolete(TermId termId) {
        if (!hpo.getTermMap().containsKey(termId)) {
            LOGGER.warn("Dropping unknown HPO term id {}", termId.getValue());
            return Optional.empty();
        }
        if (hpo.getObsoleteTermIds().contains(termId)) {
            TermId primary = hpo.getPrimaryTermId(termId);
            LOGGER.info("Replacing obsolete HPO term id {} with current id {}", termId, primary);
            return Optional.of(primary);
        }
        return Optional.of(termId);
    }
}
