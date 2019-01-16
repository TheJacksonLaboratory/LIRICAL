package org.monarchinitiative.lr2pg.configuration;



import java.util.Map;

/**
 * This class is used to input the YAML configuration file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class YamlConfig {

    private Map<String,String> analysis;
    private String prefix;
    private String[] hpoIds;

    public void setAnalysis(Map<String,String> an) {
        this.analysis=an;
    }

    /**
     * This is a map in the YAML file that contains the following items:
     * genomeAssembly, vcf,jannovar, hp.obo, phenotype.hpoa,
     * gene_info, medgen, background_freq, datadir, mvstore
     * In each case, the item is a path to a file that is needed (except for
     * genomeAssembly, which should be {@code hg19} or {@code hg38}).
     *
     * @return map with analysis parameters
     */
    public Map<String,String> getAnalysis(){
        return analysis;
    }
    //public void setPrefix(String out) {
   //     this.prefix=out;
    //}
    /** @return name (prefix) of the output file */
    public String getPrefix() {
        return prefix;
    }
    public void setHpoIds(String[] ids) {
        this.hpoIds=ids;
    }
    /** @return list of HPO ids observed in the proband. */
    public String[] getHpoIds() {
        return hpoIds;
    }



}
