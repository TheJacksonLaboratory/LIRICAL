package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class PhenopacketData {

    private final String genomeAssembly;
    private final String sampleId;
    private final List<TermId> hpoTerms;
    private final List<TermId> negatedHpoTerms;
    private final Age age;
    private final Sex sex;
    private final Path vcfPath;

    PhenopacketData(String genomeAssembly,
                    String sampleId,
                    List<TermId> hpoTerms,
                    List<TermId> negatedHpoTerms,
                    Age age,
                    Sex sex,
                    Path vcfPath) {
        this.genomeAssembly = genomeAssembly;
        this.sampleId = Objects.requireNonNull(sampleId);
        this.hpoTerms = hpoTerms;
        this.negatedHpoTerms = negatedHpoTerms;
        this.age = age;
        this.sex = sex;
        this.vcfPath = vcfPath;
    }

    public Optional<String> getGenomeAssembly() {
        return Optional.ofNullable(genomeAssembly);
    }

    public String getSampleId() {
        return sampleId;
    }

    public Stream<TermId> getHpoTerms() {
        return hpoTerms.stream();
    }

    public Stream<TermId> getNegatedHpoTerms() {
        return negatedHpoTerms.stream();
    }

    public Optional<Age> getAge() {
        return Optional.ofNullable(age);
    }

    public Optional<Sex> getSex() {
        return Optional.ofNullable(sex);
    }

    public Optional<Path> getVcfPath() {
        return Optional.ofNullable(vcfPath);
    }
}