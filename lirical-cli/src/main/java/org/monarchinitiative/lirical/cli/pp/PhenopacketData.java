package org.monarchinitiative.lirical.cli.pp;

import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.lirical.core.analysis.AnalysisInputs;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class PhenopacketData implements AnalysisInputs {

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

    public Optional<String> getGenomeAssembly() {
        return Optional.ofNullable(genomeAssembly);
    }

    public String getSampleId() {
        return sampleId;
    }

    public Stream<TermId> getHpoTerms() {
        return hpoTerms.stream().map(TermId::of);
    }

    public Stream<TermId> getNegatedHpoTerms() {
        return negatedHpoTerms.stream().map(TermId::of);
    }

    public Iterable<GenotypedVariant> getVariants() {
        return variants;
    }

    public Optional<String> getAge() {
        return Optional.ofNullable(age);
    }

    public Optional<String> getSex() {
        return Optional.ofNullable(sex);
    }

    public List<TermId> getDiseaseIds() {
        return diseaseIds;
    }

    @Override
    public String sampleId() {
        return sampleId;
    }

    @Override
    public List<String> presentHpoTerms() {
        return hpoTerms;
    }

    @Override
    public List<String> excludedHpoTerms() {
        return negatedHpoTerms;
    }

    @Override
    public String age() {
        return age;
    }

    @Override
    public String sex() {
        return sex;
    }

    @Override
    public String vcf() {
        return vcfPath;
    }
}
