package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link AnalysisData}.
 */
final class AnalysisDataDefault implements AnalysisData {
    private final String sampleId;
    private final Age age;
    private final Sex sex;
    private final List<TermId> presentPhenotypeTerms;
    private final List<TermId> negatedPhenotypeTerms;
    private final GenesAndGenotypes genes;

    AnalysisDataDefault(String sampleId,
                        Age age,
                        Sex sex,
                        Collection<TermId> presentPhenotypeTerms,
                        Collection<TermId> negatedPhenotypeTerms,
                        GenesAndGenotypes genes) {
        this.sampleId = Objects.requireNonNull(sampleId);
        this.age = age;
        this.sex = Objects.requireNonNull(sex);
        this.presentPhenotypeTerms = List.copyOf(Objects.requireNonNull(presentPhenotypeTerms));
        this.negatedPhenotypeTerms = List.copyOf(Objects.requireNonNull(negatedPhenotypeTerms));
        this.genes = Objects.requireNonNull(genes);
    }

    @Override
    public String sampleId() {
        return sampleId;
    }

    @Override
    public Optional<Age> age() {
        return Optional.ofNullable(age);
    }

    @Override
    public Sex sex() {
        return sex;
    }

    @Override
    public List<TermId> presentPhenotypeTerms() {
        return presentPhenotypeTerms;
    }

    @Override
    public List<TermId> negatedPhenotypeTerms() {
        return negatedPhenotypeTerms;
    }

    @Override
    public GenesAndGenotypes genes() {
        return genes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AnalysisDataDefault) obj;
        return Objects.equals(this.sampleId, that.sampleId) &&
                Objects.equals(this.age, that.age) &&
                Objects.equals(this.sex, that.sex) &&
                Objects.equals(this.presentPhenotypeTerms, that.presentPhenotypeTerms) &&
                Objects.equals(this.negatedPhenotypeTerms, that.negatedPhenotypeTerms) &&
                Objects.equals(this.genes, that.genes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleId, age, sex, presentPhenotypeTerms, negatedPhenotypeTerms, genes);
    }

    @Override
    public String toString() {
        return "AnalysisDataDefault[" +
                "sampleId=" + sampleId + ", " +
                "age=" + age + ", " +
                "sex=" + sex + ", " +
                "presentPhenotypeTerms=" + presentPhenotypeTerms + ", " +
                "negatedPhenotypeTerms=" + negatedPhenotypeTerms + ", " +
                "genes=" + genes + ']';
    }

}
