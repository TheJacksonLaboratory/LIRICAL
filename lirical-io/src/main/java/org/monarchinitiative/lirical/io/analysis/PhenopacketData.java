package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.lirical.core.sanitize.SanitationInputs;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Phenopacket attributes that are relevant for LIRICAL.
 */
public class PhenopacketData implements SanitationInputs {

    private final String genomeAssembly;
    private final String sampleId;
    private final List<String> hpoTerms;
    private final List<String> negatedHpoTerms;
    private final List<GenotypedVariant> variants;
    private final String age;
    private final String sex;
    private final List<TermId> diseaseIds;
    private final String vcfPath;

    PhenopacketData(String genomeAssembly,
                    String sampleId,
                    List<String> hpoTerms,
                    List<String> negatedHpoTerms,
                    String age,
                    String sex,
                    List<TermId> diseaseIds,
                    List<GenotypedVariant> variants,
                    String vcfPath) {
        this.genomeAssembly = genomeAssembly;
        this.sampleId = Objects.requireNonNull(sampleId);
        this.hpoTerms = Objects.requireNonNull(hpoTerms);
        this.negatedHpoTerms = Objects.requireNonNull(negatedHpoTerms);
        this.variants = variants;
        this.age = age;
        this.sex = sex;
        this.diseaseIds = Objects.requireNonNull(diseaseIds);
        this.vcfPath = vcfPath;
    }

    @Override
    public String sampleId() {
        return sampleId;
    }

    @Override
    public List<String> presentHpoTerms() {
        return hpoTerms;
    }

    public Stream<TermId> presentHpoTermIds() {
        return hpoTerms.stream().map(TermId::of);
    }

    @Override
    public List<String> excludedHpoTerms() {
        return negatedHpoTerms;
    }

    public Stream<TermId> excludedHpoTermIds() {
        return negatedHpoTerms.stream().map(TermId::of);
    }

    @Override
    public String age() {
        return age;
    }

    /**
     * Try to parse the age and return an empty optional if the parsing fails.
     */
    public Optional<Age> parseAge() {
        try {
            return Optional.of(Age.parse(Period.parse(age)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    @Override
    public String sex() {
        return sex;
    }

    /**
     * Try to parse the sex and return an empty optional if the parsing fails.
     */
    public Optional<Sex> parseSex() {
        try {
            return Optional.of(Sex.valueOf(sex.toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    @Override
    public String vcf() {
        return vcfPath;
    }

    public Optional<String> genomeAssembly() {
        return Optional.ofNullable(genomeAssembly);
    }

    public Iterable<GenotypedVariant> variants() {
        return variants;
    }

    public List<TermId> diseaseIds() {
        return diseaseIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhenopacketData that = (PhenopacketData) o;
        return Objects.equals(genomeAssembly, that.genomeAssembly) && Objects.equals(sampleId, that.sampleId) && Objects.equals(hpoTerms, that.hpoTerms) && Objects.equals(negatedHpoTerms, that.negatedHpoTerms) && Objects.equals(variants, that.variants) && Objects.equals(age, that.age) && Objects.equals(sex, that.sex) && Objects.equals(diseaseIds, that.diseaseIds) && Objects.equals(vcfPath, that.vcfPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genomeAssembly, sampleId, hpoTerms, negatedHpoTerms, variants, age, sex, diseaseIds, vcfPath);
    }

    @Override
    public String toString() {
        return "PhenopacketData{" +
                "genomeAssembly='" + genomeAssembly + '\'' +
                ", sampleId='" + sampleId + '\'' +
                ", hpoTerms=" + hpoTerms +
                ", negatedHpoTerms=" + negatedHpoTerms +
                ", variants=" + variants +
                ", age='" + age + '\'' +
                ", sex='" + sex + '\'' +
                ", diseaseIds=" + diseaseIds +
                ", vcfPath='" + vcfPath + '\'' +
                '}';
    }
}
