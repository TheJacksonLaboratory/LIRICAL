package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.lirical.hpo.Age;
import org.monarchinitiative.lirical.hpo.Sex;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

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

    Age age();

    Sex sex();

    List<TermId> presentPhenotypeTerms();

    List<TermId> negatedPhenotypeTerms();

    GenesAndGenotypes genes();

}
