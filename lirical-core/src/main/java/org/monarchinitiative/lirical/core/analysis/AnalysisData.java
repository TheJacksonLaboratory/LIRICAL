package org.monarchinitiative.lirical.core.analysis;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Representation of subject data required by LIRICAL analysis.
 */
public interface AnalysisData {

    /**
     * Construct analysis data from the inputs.
     *
     * @param sampleId non-null sample identifier.
     * @param age subject's age or {@code null} if not available.
     * @param sex non-null sex.
     * @param presentPhenotypeTerms a collection of observed HPO terms.
     * @param negatedPhenotypeTerms a collection of excluded HPO terms.
     * @param genes non-null container of genes and genotypes.
     */
    static AnalysisData of(String sampleId,
                           Age age,
                           Sex sex,
                           Collection<TermId> presentPhenotypeTerms,
                           Collection<TermId> negatedPhenotypeTerms,
                           GenesAndGenotypes genes) {
        return new AnalysisDataDefault(sampleId,
                age,
                sex,
                presentPhenotypeTerms,
                negatedPhenotypeTerms,
                genes);
    }

    /**
     * @return a non-null sample ID.
     */
    @JsonGetter
    String sampleId();

    /**
     * @return an optional with age or empty optional if age is not available.
     */
    @JsonGetter
    Optional<Age> age();

    /**
     * @return a non-null sex of the subject.
     */
    @JsonGetter(value = "sex")
    Sex sex();

    /**
     * @return a list of the HPO terms that were observed in the subject.
     */
    @JsonGetter(value = "observedPhenotypicFeatures")
    List<TermId> presentPhenotypeTerms();

    /**
     * @return a list of the HPO terms whose presence was explicitly excluded in the subject.
     */
    @JsonGetter(value = "excludedPhenotypicFeatures")
    List<TermId> negatedPhenotypeTerms();

    /**
     * @return container with genes and genotypes observed in the subject.
     */
    @JsonIgnore
    GenesAndGenotypes genes();

}
