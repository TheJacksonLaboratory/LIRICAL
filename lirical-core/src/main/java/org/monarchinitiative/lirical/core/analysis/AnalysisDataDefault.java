package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

/**
 * Default implementation of {@link AnalysisData}.
 */
record AnalysisDataDefault(String sampleId,
                           Age age,
                           Sex sex,
                           List<TermId> presentPhenotypeTerms,
                           List<TermId> negatedPhenotypeTerms,
                           GenesAndGenotypes genes) implements AnalysisData {
}
