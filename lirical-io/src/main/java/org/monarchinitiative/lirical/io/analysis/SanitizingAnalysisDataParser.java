package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

abstract class SanitizingAnalysisDataParser extends BaseAnalysisDataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanitizingAnalysisDataParser.class);

    protected final HpoTermSanitizer sanitizer;

    protected SanitizingAnalysisDataParser(HpoTermSanitizer sanitizer,
                                           VariantParserFactory variantParserFactory,
                                           HpoAssociationData associationData) {
        super(variantParserFactory, associationData);
        this.sanitizer = Objects.requireNonNull(sanitizer);
    }

    protected Optional<TermId> toTermId(String payload) {
        try {
            return sanitize(TermId.of(payload));
        } catch (PhenolRuntimeException e) {
            LOGGER.warn("Skipping non-parsable term {}", payload);
            return Optional.empty();
        }
    }

    protected Optional<TermId> sanitize(TermId termId) {
        return sanitizer.replaceIfObsolete(termId);
    }

    protected static Optional<URI> toUri(String uri) {
        try {
            return Optional.of(new URI(uri));
        } catch (URISyntaxException e) {
            LOGGER.warn("Invalid URI {}: {}", uri, e.getMessage());
            return Optional.empty();
        }
    }
}
