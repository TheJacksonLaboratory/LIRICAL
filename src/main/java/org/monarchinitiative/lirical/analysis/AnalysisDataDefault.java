package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.lirical.model.Age;
import org.monarchinitiative.lirical.model.Sex;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
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
