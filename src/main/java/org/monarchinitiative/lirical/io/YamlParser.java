package org.monarchinitiative.lirical.io;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FilenameUtils;
import org.monarchinitiative.lirical.configuration.YamlConfig;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * This class ingests a YAML file with parameters that will be used for the analysis.
 * @author Peter Robinson
 */
public class YamlParser {
    private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);
    private YamlConfig yconfig;
    /** THe path to which LIRICAL will download data such as hp.obo by default. */
    private static final String DEFAULT_DATA_PATH="data";



    public YamlParser(String yamlPath) {
        if (yamlPath==null || !new File(yamlPath).exists()) {
            throw new PhenolRuntimeException("[ERROR] Could not find YAML configuration file at \""+yamlPath+"\"");
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            yconfig = mapper.readValue(new File(yamlPath), YamlConfig.class);
        } catch (JsonMappingException e) {
            throw new LiricalRuntimeException(String.format("[FATAL] Malformed YAML file: Unrecognized field name in YAML file %s.\n %s" ,
                    yamlPath ,e.getMessage()));
        } catch (JsonParseException e ) {
            System.err.println("[FATAL] YAML file parse error " + e.getMessage());
        } catch (IOException e) {
            yconfig=null;
            throw new LiricalRuntimeException("Could not find YAML file: "+ e.getMessage());
        }
    }





    String getMvStorePath()  {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=getPathWithoutTrailingSeparatorIfPresent(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_variants.mv.db", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        } else {
            throw new LiricalRuntimeException("exomiser path not found in YAML configuration file");
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
     * The YAML file is allowed to have an element called analysis/background for the background
     * frequency file. If it is present, we return in in an Optional object. Usually, users should use
     * the default files and not include this element in the yaml file.
     * @return path to the background-frequency path if present, otherwise an empty Optional object.
     */
    public Optional<String> getBackgroundPath() {
        if (yconfig.getAnalysis().containsKey("background")) {
            return Optional.of(yconfig.getAnalysis().get("background"));
        } else {
            return Optional.empty();
        }
    }





    /**
     * @return path to the approprioate Jannovar transcript file (UCSC, Ensembl, or RefSeq).
     * @throws LiricalException if there is an error retrieving the Jannovar data object
     */
    String jannovarFile() throws LiricalException {
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

    public String getExomiserDataDir(){
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String datadir=yconfig.getAnalysis().get("exomiser");
            datadir=getPathWithoutTrailingSeparatorIfPresent(datadir);
            return datadir;
        }  else {
            throw new LiricalRuntimeException("No exomiser path found in YAML configuration file");
        }
    }

    /**
     * In most cases, users should use the default data directory ("data") that is created by the LIRICAL download
     * command by default. If users choose another path, they should enter a datadir element in the YAML file.
     * An empty Optional object is return if nothing is present in the YAML file, indicating that the default
     * should be used
     * @return Path of non-default data directory or default. Trailing slash (if present) will be removed
     */
    public String getDataDir() {
        if (yconfig.getAnalysis().containsKey("datadir")) {
            String path = yconfig.getAnalysis().get("datadir");
            return getPathWithoutTrailingSeparatorIfPresent(path);
        }  else {
            return DEFAULT_DATA_PATH;
        }
    }

    public String getHpoPath() {
        String ddir = getDataDir();
        return String.format("%s%s%s",ddir,File.separator,"hp.obo");
    }

    /**
     * The Yaml file should have an entry {@code exomiser: /some/path/} in the analysis
     * section if the user wants to run a VCF-based analysis (this requires the Exomiser database). If there is
     * {@code mode: vcf} or if there is no entry, we will run a VCF analysis
     * @return true if phenotype only analysis should be performed.
     */
    public boolean phenotypeOnlyMode() {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            return false; // exomiser is only to be used for VCF analysis
        } else if (yconfig.getAnalysis().containsKey("vcf")) {
            return false;
        }
        return true;
    }



    private String jannovarFileUCSC() throws LiricalException {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=FilenameUtils.getFullPathNoEndSeparator(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_transcripts_ucsc.ser", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        }  else {
            throw new LiricalException("No jannovar UCSC transcript file path found in YAML configuration file");
        }
    }

    private String jannovarFileEnsembl() throws LiricalException {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=FilenameUtils.getFullPathNoEndSeparator(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_transcripts_ensembl.ser", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        }  else {
            throw new LiricalException("No jannovar UCSC transcript file path found in YAML configuration file");
        }
    }

    private String jannovarFileRefSeq() throws LiricalException {
        if (yconfig.getAnalysis().containsKey("exomiser")) {
            String exomiserPath = yconfig.getAnalysis().get("exomiser");
            // Remove the trailing directory slash if any
            exomiserPath=FilenameUtils.getFullPathNoEndSeparator(exomiserPath);
            String basename=FilenameUtils.getBaseName(exomiserPath);
            String filename=String.format("%s_transcripts_refseq.ser", basename);
            return String.format("%s%s%s", exomiserPath,File.separator,filename);
        }  else {
            throw new LiricalException("No jannovar UCSC transcript file path found in YAML configuration file");
        }
    }



    String getHpOboPath() {
        String datadir = getDataDir();
        return String.format("%s%s%s",datadir,File.separator,"hp.obo");
    }


    /**@return A String representing the genome assembly of the VCF file (should be hg19 or hg38). */
    public String getGenomeAssembly() {
        if (yconfig.getAnalysis().containsKey("genomeAssembly")) {
            return yconfig.getAnalysis().get("genomeAssembly");
        }  else {
            throw new LiricalRuntimeException("genomeAssembly not found in YAML configuration file");
        }
    }


    /**
     * The user can choose to run LIRICAL without a VCF file. Then, a phenotype only analysis is performed.
     * In this case, we return an empty Optional object.
     * @return Path to VCF file, if present.
     */
    public Optional<String> getOptionalVcfPath()  {
        if (yconfig.getAnalysis().containsKey("vcf")) {
            return Optional.of(yconfig.getAnalysis().get("vcf"));
        }  else {
            return Optional.empty();
        }
    }


    public String getPrefix() {
        return yconfig.getPrefix();
    }

    /**
     * @return list of Strings representing HPOs, e.g., HP:0001234,HPO:0002345
     */
    public List<String> getHpoTermList() {
        return ImmutableList.copyOf(yconfig.getHpoIds());
    }
    /** @return list of Strings with excluded (negated) HPO terms. */
    public List<String> getNegatedHpoTermList() {
        // if the YAML file has no negated section, the YAML config will return null.
        // in this case we return an empty list.
        if (yconfig.getNegatedHpoIds()==null) return ImmutableList.of();
        else return ImmutableList.copyOf(yconfig.getNegatedHpoIds());
    }


    String getSampleId() {
        if (yconfig==null || yconfig.getSampleId() == null) {
            throw new LiricalRuntimeException("YAML file does not contain required sampleId element");
        }
        return yconfig.getSampleId();
    }

    public Optional<String> getOutDirectory() {
        if (yconfig==null ) {
            throw new LiricalRuntimeException("Could not parse YAML file");
        }
        if (yconfig.getOutdir()==null) return Optional.empty();
        else return Optional.of(yconfig.getOutdir());
    }

    /** @return true if the YAML analysis section contains a KV pair keep:true */
    public boolean keep() {
        String v = yconfig.getAnalysis().getOrDefault("keep","notfound");
        return  v.equalsIgnoreCase("true");
    }

    public boolean orphanet() {
        String v = yconfig.getAnalysis().getOrDefault("orphanet","notfound");
        return v.equalsIgnoreCase("true");
    }

    public Optional<Integer> mindiff() {
        if (yconfig.getAnalysis().containsKey("mindiff")) {
            String md =  yconfig.getAnalysis().get("mindiff");
            try {
                return Optional.of(Integer.parseInt(md));
            } catch( NumberFormatException nfe) {
                System.err.println("[ERROR] Could not parse mindiff entry in YAML file. Using default");
            }
        }
        return Optional.empty();
    }

    /**
     * If the analysis section of the YAML file has a valid threshold entry, return it
     * @return threshold (may be empty)
     */
    public Optional<Double> threshold() {
        if (yconfig.getAnalysis().containsKey("threshold")) {
            String thr =  yconfig.getAnalysis().get("threshold");
            try {
                return Optional.of(Double.parseDouble(thr));
            } catch( NumberFormatException nfe) {
                System.err.println("[ERROR] Could not parse threshold entry in YAML file. Using default");
            }
        }
        return Optional.empty();
    }

    boolean doTsv() {
        if (yconfig.getAnalysis().containsKey("tsv")) {
            String k = yconfig.getAnalysis().get("tsv");
            return k.equalsIgnoreCase("true");
        }
        return false; // no TSV entry in YAML file
    }

}
