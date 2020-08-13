package org.monarchinitiative.lirical.configuration;

import java.util.Map;

/**
 * This class is used to input the YAML configuration file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class YamlConfig {

    private Map<String,String> analysis;
    private String prefix;
    private String outdir;
    private String sampleId;
    private String[] hpoIds;
    private String[] negatedHpoIds;
    private boolean keep;
    private String mindiff;

    public void setAnalysis(Map<String,String> an) {
        this.analysis=an;
    }

    /**
     * This is a map in the YAML file that contains the following items:
     * genomeAssembly, vcf,jannovar, hp.obo, phenotype.hpoa,
     * gene_info, medgen, background_freq, datadir, mvstore, orphanet
     * In each case, the item is a path to a file that is needed (except for
     * genomeAssembly, which should be {@code hg19} or {@code hg38}).
     *
     * @return map with analysis parameters
     */
    public Map<String,String> getAnalysis(){
        return analysis;
    }

    public boolean hasAnalysis() {
        return analysis != null && analysis.size()>0;
    }

    /** @return name (prefix) of the output file */
    public String getPrefix() {
        return prefix;
    }

    public String getSampleId() {
        return sampleId;
    }

    /** @return list of HPO ids observed in the proband. */
    public String[] getHpoIds() {
        return hpoIds;
    }

    public String[] getNegatedHpoIds() { return negatedHpoIds; }

    public String getOutdir(){ return outdir;}

}
