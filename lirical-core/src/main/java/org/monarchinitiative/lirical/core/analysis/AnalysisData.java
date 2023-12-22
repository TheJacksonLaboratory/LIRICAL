package org.monarchinitiative.lirical.core.analysis;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * An interface for representing proband data.
 */
public interface AnalysisData {

    static AnalysisData of(String sampleId,
                           Age age,
                           Sex sex,
                           Collection<TermId> presentPhenotypeTerms,
                           Collection<TermId> negatedPhenotypeTerms,
                           GenesAndGenotypes genes) {
        return new AnalysisDataDefault(sampleId,
                age,
                Objects.requireNonNull(sex),
                List.copyOf(presentPhenotypeTerms),
                List.copyOf(negatedPhenotypeTerms),
                genes);
    }

    String sampleId();

    // TODO - make non-null or wrap into Optional. See the TODO in Age for more info.
    Age age();

    Sex sex();

    @JsonGetter(value = "observedPhenotypicFeatures")
    List<TermId> presentPhenotypeTerms();

    @JsonGetter(value = "excludedPhenotypicFeatures")
    List<TermId> negatedPhenotypeTerms();

    @JsonIgnore
    GenesAndGenotypes genes();

}
