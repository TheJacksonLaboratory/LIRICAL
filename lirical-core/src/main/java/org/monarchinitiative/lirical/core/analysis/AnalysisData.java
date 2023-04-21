package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
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

    // TODO - make non-null or wrap into Optional. See the TODO in Age for more info.
    Age age();

    // TODO - make non-null or wrap into Optional.
    Sex sex();

    List<TermId> presentPhenotypeTerms();

    List<TermId> negatedPhenotypeTerms();

    GenesAndGenotypes genes();

}
