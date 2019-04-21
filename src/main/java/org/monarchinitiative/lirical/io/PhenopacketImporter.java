package org.monarchinitiative.lirical.io;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.JsonFormat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Phenotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * This class ingests a phenopacket, which is required to additionally contain the
 * path of a VCF file that will be used for the analysis.
 * @author Peter Robinson
 */
public class PhenopacketImporter {
    private static final Logger logger = LoggerFactory.getLogger(PhenopacketImporter.class);
    /** The Phenopacket that represents the individual being sequenced in the current run. */
    private final Phenopacket phenoPacket;
    /** A list of non-negated HPO terms observed in the subject of this Phenopacket. */
    private ImmutableList<TermId> hpoTerms;
    /** A list of negated HPO terms observed in the subject of this Phenopacket. */
    private ImmutableList<TermId> negatedHpoTerms;
    /** Path to the VCF file with variants identified in the subject of this Phenopacket. */
    private String vcfPath;
    /** Genome assembly of the VCF file in {@link #vcfPath}. */
    private String genomeAssembly;
    /** Name of the proband of the Phenopacket (corresponds to the {@code id} element of the phenopacket). */
    private final String samplename;


    /**
     * Factory method to obtain a PhenopacketImporter object starting from a phenopacket in Json format
     * @param pathToJsonPhenopacketFile -- path to the phenopacket
     * @return {@link PhenopacketImporter} object corresponding to the PhenoPacket
     * @throws ParseException if the JSON code cannot be parsed
     * @throws IOException if the File cannot be found
     */
    public static PhenopacketImporter fromJson(String pathToJsonPhenopacketFile) throws ParseException,IOException {
        JSONParser parser = new JSONParser();
        logger.trace("Importing Phenopacket: " + pathToJsonPhenopacketFile);
        Object obj = parser.parse(new FileReader(pathToJsonPhenopacketFile));
        JSONObject jsonObject = (JSONObject) obj;
        String phenopacketJsonString = jsonObject.toJSONString();
        Phenopacket phenopacket;
        try {
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
            phenopacket = phenoPacketBuilder.build();
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new LiricalRuntimeException("Could not load phenopacket at " + pathToJsonPhenopacketFile);
        }
        return new PhenopacketImporter(phenopacket);
    }

    public PhenopacketImporter(Phenopacket ppack){
        this.phenoPacket=ppack;
        this.samplename = this.phenoPacket.getSubject().getId();
        extractProbandHpoTerms();
        extractNegatedProbandHpoTerms();
        extractVcfData();
    }

    public boolean hasVcf() { return  this.vcfPath !=null; }

    public List<TermId> getHpoTerms() {
        return hpoTerms;
    }

    public List<TermId> getNegatedHpoTerms() {
        return negatedHpoTerms;
    }

    public String getVcfPath() {
        return vcfPath;
    }

    public String getGenomeAssembly() {
        return genomeAssembly;
    }

    public String getSamplename() {
        return samplename;
    }



    /**
     * This method extracts a list of
     * all of the non-negated HPO terms that are annotated to the proband of this
     * phenopacket. Note that we use "distinct" to get only distinct elements, defensively,
     * even though a valid phenopacket should not have duplicates.
     */
    private void extractProbandHpoTerms() {
        this.hpoTerms= phenoPacket
                .getPhenotypesList()
                .stream()
                .distinct()
                .filter(((Predicate<Phenotype>) Phenotype::getNegated).negate()) // i.e., just take non-negated phenotypes
                .map(Phenotype::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * This function gets a list of all negated HPO terms associated with the proband.
     */
    private void extractNegatedProbandHpoTerms() {
        this.negatedHpoTerms = phenoPacket
                .getPhenotypesList()
                .stream()
                .filter(Phenotype::getNegated) // i.e., just take negated phenotypes
                .map(Phenotype::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(ImmutableList.toImmutableList());
    }

    /** This method extracts the VCF file and the corresponding GenomeBuild. We assume that
     * the phenopacket contains a single VCF file and that this file is for a single person. */
    private void extractVcfData() {
        List<HtsFile> htsFileList = phenoPacket.getHtsFilesList();
        if (htsFileList.size() > 1 ) {
            logger.error("Warning: multiple HTsFiles associated with this phenopacket");
            logger.error("Warning: we will return the path to the first VCF file we find");
        } else if (htsFileList.isEmpty()) {
            return;
        }
        for (HtsFile htsFile : htsFileList) {
            if (htsFile.getHtsFormat().equals(HtsFile.HtsFormat.VCF)) {
                this.vcfPath=htsFile.getFile().getPath();
                this.genomeAssembly=htsFile.getGenomeAssembly();
            }
        }
    }
}
