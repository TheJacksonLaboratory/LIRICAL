package org.monarchinitiative.lirical.io;

import com.google.protobuf.util.JsonFormat;

import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;

import org.monarchinitiative.phenol.ontology.data.TermId;

import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * This class ingests a phenopacket, which is required to additionally contain the
 * path of a VCF file that will be used for the analysis.
 * @author Peter Robinson
 */
public class PhenopacketImporter {
    private static final Logger logger = LoggerFactory.getLogger(PhenopacketImporter.class);
    private static final JsonFormat.Parser PARSER = JsonFormat.parser();
    /** The Phenopacket that represents the individual being sequenced in the current run. */
    private final Phenopacket phenopacket;
    /** Object representing the VCF file with variants identified in the subject of this Phenopacket. */
    private final HtsFile vcfFile;

    /**
     * Factory method to obtain a PhenopacketImporter object starting from a phenopacket in Json format
     * @param phenopacketPath -- path to the phenopacket
     * @return {@link PhenopacketImporter} object corresponding to the phenopacket
     */
    public static PhenopacketImporter fromJson(Path phenopacketPath)  {
        try (BufferedReader reader = Files.newBufferedReader(phenopacketPath)) {
            Phenopacket.Builder builder = Phenopacket.newBuilder();
            PARSER.merge(reader, builder);
            Phenopacket phenopacket = builder.build();
            return of(phenopacket);
        } catch (IOException  e1) {
            throw new LiricalRuntimeException("I/O Error: Could not load phenopacket  (" + phenopacketPath +"): "+ e1.getMessage());
        }
    }

    public static PhenopacketImporter of(Phenopacket phenopacket) {
        return new PhenopacketImporter(phenopacket);
    }

    private PhenopacketImporter(Phenopacket phenopacket){
        this.phenopacket = Objects.requireNonNull(phenopacket);
        List<HtsFile> htsFileList = phenopacket.getHtsFilesList();
        if (htsFileList.size() > 1)
            logger.warn("Multiple HtsFiles associated with this phenopacket. We will use the first VCF file.");

        this.vcfFile = htsFileList.stream()
                .filter(hts -> hts.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                .findFirst()
                .orElse(null);
    }

    public boolean hasVcf() {
        return vcfFile != null;
    }

    public List<TermId> getHpoTerms() {
        return phenopacket.getPhenotypicFeaturesList().stream()
                .filter(pf -> !pf.getNegated())
                .map(PhenotypicFeature::getType)
                .map(type -> createTermId(type.getId()))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<TermId> getNegatedHpoTerms() {
        return phenopacket.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getNegated)
                .map(PhenotypicFeature::getType)
                .map(type -> createTermId(type.getId()))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<TermId> createTermId(String termId) {
        try {
            return Optional.of(TermId.of(termId));
        } catch (PhenolRuntimeException e) {
            logger.warn("Skipping unparsable HPO term id {}", termId);
            return Optional.empty();
        }
    }

    public Optional<Age> getAge() {
        return Optional.of(phenopacket.getSubject().getAgeAtCollection());
    }

    public Optional<Sex> getSex() {
        return Optional.of(phenopacket.getSubject().getSex());
    }

    public String getGene() {
        if (phenopacket.getGenesCount()==0) return null;
        Gene g = phenopacket.getGenes(0);
        return g.getId();
    }

    public Optional<HtsFile> getVcfFile() {
        return Optional.ofNullable(vcfFile);
    }


    public Optional<Path> getVcfPath() {
        return getVcfFile()
                .flatMap(hts -> toUri(hts.getUri()))
                .map(Path::of);
    }

    private Optional<URI> toUri(String uri) {
        try {
            return Optional.of(new URI(uri));
        } catch (URISyntaxException e) {
            logger.warn("Invalid URI {}: {}", uri, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> getGenomeAssembly() {
        return getVcfFile()
                .map(HtsFile::getGenomeAssembly);
    }

    public String getSampleId() {
        return phenopacket.getSubject().getId();
    }

    public List<Variant> getVariantList() { return phenopacket.getVariantsList(); }


    public Optional<Disease> getDiagnosis() {
        if (phenopacket.getDiseasesCount() == 0) {
            logger.info("No diseases found in phenopacket");
            return Optional.empty();
        } else if (phenopacket.getDiseasesCount() > 1)
            logger.warn("Phenopacket associated with {} diseases. Getting the first disease", phenopacket.getDiseasesCount());
        return Optional.of(phenopacket.getDiseases(0));
    }


    @Deprecated(forRemoval = true) // inline the QC where it is actually used
    public boolean qcPhenopacket() {
        if (phenopacket.getDiseasesCount() != 1) {
            System.err.println("[ERROR] to run this simulation a phenopacket must have exactly one disease diagnosis");
            System.err.println("[ERROR]  " + phenopacket.getSubject().getId() + " had " + phenopacket.getDiseasesCount());
            return false; // skip to next Phenopacket
        }
        List<PhenotypicFeature> phenolist = phenopacket.getPhenotypicFeaturesList();
        int n_observed = (int) phenolist.stream().filter(p -> !p.getNegated()).count();
        if (n_observed==0) {
            System.err.println("[ERROR] phenopackets must have at least one observed HPO term. ");
            return false; // skip to next Phenopacket
        }
        return true;
    }

}
