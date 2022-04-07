package org.monarchinitiative.lirical.io.yaml;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * This class is used to input the YAML configuration file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class YamlConfig {

    private String sampleId;
    private List<String> hpoIds;
    private List<String> negatedHpoIds;
    private String age;
    private String sex;
    private String vcf;

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public void setHpoIds(List<String> hpoIds) {
        this.hpoIds = hpoIds;
    }

    public void setNegatedHpoIds(List<String> negatedHpoIds) {
        this.negatedHpoIds = negatedHpoIds;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }


    public void setVcf(String vcf) {
        this.vcf = vcf;
    }

    public String getSampleId() {
        return sampleId;
    }

    /** @return list of HPO ids observed in the proband. */
    public List<String> getHpoIds() {
        return hpoIds == null ? List.of() : hpoIds;
    }

    public List<String> getNegatedHpoIds() { return negatedHpoIds == null ? List.of() : negatedHpoIds; }

    public String age() {
        return age;
    }

    public String sex() {
        return sex;
    }

    public String vcf() {
        return vcf;
    }

    public Optional<Path> vcfPath() {
        return vcf == null ?
                Optional.empty()
                : Optional.of(Path.of(vcf));
    }

}
