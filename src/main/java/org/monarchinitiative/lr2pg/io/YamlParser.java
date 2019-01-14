package org.monarchinitiative.lr2pg.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.monarchinitiative.lr2pg.configuration.YamlConfig;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;

import java.io.File;
import java.io.IOException;

/**
 * This class ingests a YAML file with parameters that will be used for the analysis.
 * @author Peter Robinson
 */
public class YamlParser {

    private YamlConfig yconfig;


    public YamlParser(String path) throws Lr2pgException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            yconfig = mapper.readValue(new File(path), YamlConfig.class);
            //System.out.println(ReflectionToStringBuilder.toString(yconfig, ToStringStyle.MULTI_LINE_STYLE));
        } catch (IOException e) {
            yconfig=null;
            throw new Lr2pgException("Could not find YAML file: "+ e.getMessage());
        }
    }

    public String getMvStorePath() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("mvstore")) {
            return yconfig.getAnalysis().get("mvstore");
        } else {
            throw new Lr2pgException("No MvStore path found in YAML configuration file");
        }
    }

    public String getHpOboPath() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("datadir")) {
            String datadir=yconfig.getAnalysis().get("datadir");
            return String.format("%s%s%s",datadir,File.separator,"hp.obo");
        } else {
            throw new Lr2pgException("No data/hp.obo path found in YAML configuration file");
        }
    }

    public String getMedgen() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("datadir")) {
            String datadir=yconfig.getAnalysis().get("datadir");
            return String.format("%s%s%s",datadir,File.separator,"mim2gene_medgen");
        }  else {
            throw new Lr2pgException("No data/mim2gene_medgen path found in YAML configuration file");
        }
    }

    /**@return A String representing the genome assembly of the VCF file (should be hg19 or hg38). */
    public String getGenomeAssembly() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("genomeAssembly")) {
            return yconfig.getAnalysis().get("genomeAssembly");
        }  else {
            throw new Lr2pgException("genomeAssembly not found in YAML configuration file");
        }
    }


    public String getGeneInfo() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("datadir")) {
            String datadir=yconfig.getAnalysis().get("datadir");
            return String.format("%s%s%s",datadir,File.separator,"Homo_sapiens_gene_info.gz");
        }  else {
            throw new Lr2pgException("No Homo_sapiens_gene_info.gz path found in YAML configuration file");
        }
    }

    public String vcfPath() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("vcf")) {
            return yconfig.getAnalysis().get("vcf");
        }  else {
            throw new Lr2pgException("No VCF file path found in YAML configuration file");
        }
    }

    public String jannovarFile() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("jannovar")) {
            return yconfig.getAnalysis().get("jannovar");
        }  else {
            throw new Lr2pgException("No jannovar transcript file path found in YAML configuration file");
        }
    }

    public String phenotypeAnnotation() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("datadir")) {
            String datadir=yconfig.getAnalysis().get("datadir");
            return String.format("%s%s%s",datadir,File.separator,"phenotype.hpoa");
        }  else {
            throw new Lr2pgException("No phenotype.hpoa path found in YAML configuration file");
        }
    }

    /**
     * @return array of Strings representing HPOs, e.g., HP:0001234,HPO:0002345
     */
    public String[] getHpoTermList() {
        return yconfig.getHpoIds();
    }

}
