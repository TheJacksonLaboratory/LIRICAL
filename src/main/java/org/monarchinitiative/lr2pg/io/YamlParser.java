package org.monarchinitiative.lr2pg.io;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FilenameUtils;
import org.monarchinitiative.lr2pg.configuration.YamlConfig;
import org.monarchinitiative.lr2pg.exception.Lr2PgRuntimeException;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;

import java.io.File;
import java.io.IOException;

/**
 * This class ingests a YAML file with parameters that will be used for the analysis.
 * @author Peter Robinson
 */
public class YamlParser {

    private YamlConfig yconfig;


    public YamlParser(String yamlPath) {
        if (yamlPath==null || !new File(yamlPath).exists()) {
            throw new PhenolRuntimeException("[ERROR] Could not find YAML configuration file for VCF analysis. Terminating program");
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            yconfig = mapper.readValue(new File(yamlPath), YamlConfig.class);
        } catch (JsonMappingException e) {
            throw new Lr2PgRuntimeException(String.format("[FATAL] Malformed YAML file: Unrecognized field name in YAML file %s.\n %s" ,
                    yamlPath ,e.getMessage()));
        } catch (JsonParseException e ) {
            System.err.println("[FATAL] YAML file parse error " + e.getMessage());
        } catch (IOException e) {
            yconfig=null;
            throw new Lr2PgRuntimeException("Could not find YAML file: "+ e.getMessage());
        }
    }

    public String getMvStorePath() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=getPathWithoutTrailingSeparatorIfPresent(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_variants.mv.db", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        } else {
            throw new Lr2pgException("No MvStore path found in YAML configuration file");
        }
    }


    private static String getPathWithoutTrailingSeparatorIfPresent(String path) {
        String sep = File.separator;
        if (path.endsWith(sep)) {
            int i=path.lastIndexOf(sep);
            return path.substring(0,i);
        } else {
            return path;
        }
    }





    /**
     * @return path to the approprioate Jannovar transcript file (UCSC, Ensembl, or RefSeq).
     * @throws Lr2pgException
     */
    public String jannovarFile() throws Lr2pgException {
        String tdb = transcriptdb();
        switch (tdb) {
            case "UCSC": return jannovarFileUCSC();
            case "ENSEMBL": return jannovarFileEnsembl();
            case "REFSEQ": return jannovarFileRefSeq();
        }
        return jannovarFileUCSC();
    }


    public String transcriptdb() {
        if (yconfig.getAnalysis().containsKey("transcriptdb")) {
            String trdb = yconfig.getAnalysis().get("transcriptdb");
            switch (trdb.toUpperCase()) {
                case "UCSC": return "UCSC";
                case "ENSEMBL": return "ENSEMBL";
                case "REFSEQ": return "REFSEQ";
            }
        }
        // default
        return "UCSC";
    }

    public String getExomiserDataDir() throws Lr2pgException{
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String datadir=yconfig.getAnalysis().get("exomiser");
            datadir=getPathWithoutTrailingSeparatorIfPresent(datadir);
            return datadir;
        }  else {
            throw new Lr2pgException("No exomiser path found in YAML configuration file");
        }
    }

    public String getDataDir() throws Lr2pgException{
        if (yconfig.getAnalysis().containsKey("datadir")) {
            return yconfig.getAnalysis().get("datadir");
        }  else {
            throw new Lr2pgException("No data/mim2gene_medgen path found in YAML configuration file");
        }
    }



    private String jannovarFileUCSC() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=FilenameUtils.getFullPathNoEndSeparator(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_transcripts_ucsc.ser", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        }  else {
            throw new Lr2pgException("No jannovar UCSC transcript file path found in YAML configuration file");
        }
    }

    private String jannovarFileEnsembl() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=FilenameUtils.getFullPathNoEndSeparator(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_transcripts_ensembl.ser", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        }  else {
            throw new Lr2pgException("No jannovar UCSC transcript file path found in YAML configuration file");
        }
    }

    private String jannovarFileRefSeq() throws Lr2pgException {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=FilenameUtils.getFullPathNoEndSeparator(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_transcripts_refseq.ser", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        }  else {
            throw new Lr2pgException("No jannovar UCSC transcript file path found in YAML configuration file");
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
