package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Optional;

/**
 * An interface for representing proband data.
 */
public interface AnalysisData {

    static AnalysisData of(String sampleId,
                           Age age,
                           Sex sex,
                           List<TermId> presentPhenotypeTerms,
                           List<TermId> negatedPhenotypeTerms,
                           GenesAndGenotypes genes) {
        return new AnalysisDataDefault(sampleId,
                age,
                sex,
                presentPhenotypeTerms,
                negatedPhenotypeTerms,
                genes);
    }

    String sampleId();

    /**
     * @return age of the proband or an empty optional if the age is unknown.
     */
    Optional<Age> age();

    Sex sex();

    List<TermId> presentPhenotypeTerms();

    List<TermId> negatedPhenotypeTerms();

    GenesAndGenotypes genes();

}
