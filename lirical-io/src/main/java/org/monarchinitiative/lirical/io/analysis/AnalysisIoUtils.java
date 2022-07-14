package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

class AnalysisIoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisIoUtils.class);

    static Optional<TermId> createTermId(String termId) {
        try {
            return Optional.of(TermId.of(termId));
        } catch (PhenolRuntimeException e) {
            LOGGER.warn("Skipping unparsable HPO term id {}", termId);
            return Optional.empty();
        }
    }
}
