package org.monarchinitiative.lirical.io;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.JsonFormat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import org.monarchinitiative.phenol.ontology.data.TermId;

import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This class ingests a phenopacket, which is required to additionally contain the
 * path of a VCF file that will be used for the analysis.
 * @author Peter Robinson
 */
public class PhenopacketImporter {
    private static final Logger logger = LoggerFactory.getLogger(PhenopacketImporter.class);
    /** The Phenopacket that represents the individual being sequenced in the current run. */
    private final Phenopacket phenoPacket;
    /** Object representing the VCF file with variants identified in the subject of this Phenopacket. */
    private HtsFile vcfFile;
    /** Name of the proband of the Phenopacket (corresponds to the {@code id} element of the phenopacket). */
    private final String samplename;
    /** Reference to HPO ontology */
    private final Ontology hpo;

    /**
     * Factory method to obtain a PhenopacketImporter object starting from a phenopacket in Json format
     * @param pathToJsonPhenopacketFile -- path to the phenopacket
     * @return {@link PhenopacketImporter} object corresponding to the PhenoPacket
     */
    public static PhenopacketImporter fromJson(String pathToJsonPhenopacketFile, Ontology ontology)  {
        JSONParser parser = new JSONParser();
        logger.trace("Importing Phenopacket: " + pathToJsonPhenopacketFile);
        java.io.File tmp = new java.io.File(pathToJsonPhenopacketFile);
        if (! tmp.exists() ) {
            System.err.println("[ERROR] Could not find phenopacket file at " + pathToJsonPhenopacketFile);
            throw new LiricalRuntimeException("[ERROR] Could not find phenopacket file at " + pathToJsonPhenopacketFile);
        }
        try {
            Object obj = parser.parse(new FileReader(pathToJsonPhenopacketFile));
            JSONObject jsonObject = (JSONObject) obj;
            String phenopacketJsonString = jsonObject.toJSONString();
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
            Phenopacket phenopacket = phenoPacketBuilder.build();
            return new PhenopacketImporter(phenopacket,ontology);
        } catch (IOException  e1) {
            throw new LiricalRuntimeException("I/O Error: Could not load phenopacket  (" + pathToJsonPhenopacketFile +"): "+ e1.getMessage());
        } catch (ParseException ipbe) {
            System.err.println("[ERROR] Malformed phenopacket: " + ipbe.getMessage());
            throw new LiricalRuntimeException("Could not load phenopacket (" + pathToJsonPhenopacketFile +"): "+ ipbe.getMessage());
        }
    }

    PhenopacketImporter(Phenopacket ppack, Ontology ontology){
        this.phenoPacket=ppack;
        this.samplename = this.phenoPacket.getSubject().getId();
        this.hpo=ontology;
        extractVcfData();
    }

    public boolean hasVcf() { return  this.vcfFile !=null; }

    public List<TermId> getHpoTerms() {
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (PhenotypicFeature feature : phenoPacket.getPhenotypicFeaturesList()) {
            if (feature.getNegated()) continue;
            String id = feature.getType().getId();
            TermId tid = TermId.of(id);
            if (! hpo.getTermMap().containsKey(tid)) {
                logger.error("Could not identify HPO term id {}.",tid.getValue());
                System.err.println("[ERROR] Could not identify HPO term id " + tid.getValue() +". ");
                System.err.println("[ERROR] Please check the input file and update to the latest hp.obo file. ");
                throw new PhenolRuntimeException("Could not identify HPO term id: "+tid.getValue());
            } else if (hpo.getObsoleteTermIds().contains(tid)) {
                TermId current =  hpo.getPrimaryTermId(tid);
                builder.add(current);
                logger.error("Replacing obsolete HPO term id {} with current id {}.",tid.getValue(),current.getValue());
                System.err.println("[ERROR] Replacing obsolete HPO term id " + tid.getValue() +". with current id "+current.getValue());
            } else {
                builder.add(tid);
            }
        }
        return builder.build();
    }

    public String getGene() {
        if (phenoPacket.getGenesCount()==0) return null;
        Gene g = phenoPacket.getGenes(0);
        return g.getId();
    }




    public List<TermId> getNegatedHpoTerms() {
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (PhenotypicFeature feature : phenoPacket.getPhenotypicFeaturesList()) {
            if (! feature.getNegated()) continue;
            String id = feature.getType().getId();
            TermId tid = TermId.of(id);
            if (! hpo.getTermMap().containsKey(tid)) {
                logger.error("Could not identify HPO term id {}.",tid.getValue());
                System.err.println("[ERROR] Could not identify HPO term id " + tid.getValue() +". ");
                System.err.println("[ERROR] Please check the input file and update to the latest hp.obo file. ");
                throw new PhenolRuntimeException("Could not identify HPO term id: "+tid.getValue());
            } else if (hpo.getObsoleteTermIds().contains(tid)) {
                TermId current =  hpo.getPrimaryTermId(tid);
                builder.add(current);
                logger.error("Replacing obsolete HPO term id {} with current id {}.",tid.getValue(),current.getValue());
                System.err.println("[ERROR] Replacing obsolete HPO term id " + tid.getValue() +". with current id "+current.getValue());
            } else {
                builder.add(tid);
            }
        }
        return builder.build();
    }

    /**
     * The path to the VCF file may be a string such as file:/path/to/examples/BBS1.vcf
     * In this case, remove the prefix 'path:', otherwise return the original URI
     * @return URI of VCF file mentioned in the Phenopacket
     */
    public String getVcfPath() {
        if (this.vcfFile == null) {
            return null;
        }
        return this.vcfFile.getUri().startsWith("file:") ?
                this.vcfFile.getUri().substring(5) :
                this.vcfFile.getUri();
    }

    public HtsFile getVcfFile() {
        return this.vcfFile;
    }

    public String getGenomeAssembly() {
        return   this.vcfFile!=null ?
                this.vcfFile.getGenomeAssembly() :
                null;
    }

    public String getSamplename() {
        return samplename;
    }

    public List<Variant> getVariantList() { return phenoPacket.getVariantsList(); }


    public Disease getDiagnosis() {
        if (phenoPacket.getDiseasesCount() >1 ) {
            logger.info("Phenopacket associated with {} diseases. Just returning the first disease", phenoPacket.getDiseasesCount());
        } else if (phenoPacket.getDiseasesCount()==0) {
            logger.info("No diseases found in Phenopacket.");
            return null;
        }
        return phenoPacket.getDiseases(0);
    }


    public boolean qcPhenopacket() {
        if (phenoPacket.getDiseasesCount() != 1) {
            System.err.println("[ERROR] to run this simulation a phenopacket must have exactly one disease diagnosis");
            System.err.println("[ERROR]  " + phenoPacket.getSubject().getId() + " had " + phenoPacket.getDiseasesCount());
            return false; // skip to next Phenopacket
        }
        List<PhenotypicFeature> phenolist = phenoPacket.getPhenotypicFeaturesList();
        int n_observed = phenolist.stream().filter( p -> ! p.getNegated()).collect(Collectors.toList()).size();
        if (n_observed==0) {
            System.err.println("[ERROR] phenopackets must have at least one observed HPO term. ");
            return false; // skip to next Phenopacket
        }
        return true;
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
                this.vcfFile = htsFile;
            }
        }
    }
}
