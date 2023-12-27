package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Result of input sanitation.
 * <p>
 * The HPO terms are guaranteed to be valid HPO CURIEs, age and sex are either well-formed or {@code null},
 * and VCF points to a readable file.
 *
 * @author Daniel Danis
 */
public final class SanitizedInputs {
    private final String sampleId;
    private final List<TermId> presentHpoTerms = new ArrayList<>();
    private final List<TermId> excludedHpoTerms = new ArrayList<>();
    private Age age;
    private Sex sex;
    private Path vcf;

    SanitizedInputs(String sampleId) {
        this.sampleId = sampleId;
    }

    SanitizedInputs(String sampleId,
                    Collection<TermId> present,
                    List<TermId> excluded,
                    Age age,
                    Sex sex,
                    Path vcf) {
        this.sampleId = sampleId;
        this.presentHpoTerms.addAll(present);
        this.excludedHpoTerms.addAll(excluded);
        this.age = age; // nullable
        this.sex = sex; // nullable
        this.vcf = vcf; // nullable
    }

    public String sampleId() {
        return sampleId;
    }

    public List<TermId> presentHpoTerms() {
        return presentHpoTerms;
    }

    public List<TermId> excludedHpoTerms() {
        return excludedHpoTerms;
    }

    void setAge(Age age) {
        this.age = age;
    }

    public Age age() {
        return age;
    }


    void setSex(Sex sex) {
        this.sex = sex;
    }

    public Sex sex() {
        return sex;
    }

    void setVcf(Path vcf) {
        this.vcf = vcf;
    }

    public Path vcf() {
        return vcf;
    }

    @Override
    public String toString() {
        return "SanitizedInputs[" + "sampleId=" + sampleId + ", " + "presentHpoTerms=" + presentHpoTerms + ", " + "excludedHpoTerms=" + excludedHpoTerms + ", " + "age=" + age + ", " + "sex=" + sex + ", " + "vcf=" + vcf + ']';
    }

}
