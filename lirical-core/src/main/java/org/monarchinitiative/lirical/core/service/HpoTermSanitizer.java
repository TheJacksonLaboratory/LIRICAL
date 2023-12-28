package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HpoTermSanitizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HpoTermSanitizer.class);

    private final MinimalOntology hpo;

    public HpoTermSanitizer(MinimalOntology hpo) {
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
        Optional<Term> term = hpo.termForTermId(termId);
        if (term.isEmpty()) {
            LOGGER.warn("Dropping unknown HPO term id {}", termId.getValue());
            return Optional.empty();
        }
        Term t = term.get();
        if (!t.id().equals(termId)) {
            LOGGER.info("Replacing obsolete HPO term id {} with current id {}", termId, t.id());
            return Optional.of(t.id());
        }
        return Optional.of(termId);
    }
}
