package org.monarchinitiative.lirical.analysis;

import htsjdk.variant.variantcontext.*;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Variant;
import org.phenopackets.schema.v1.core.VcfAllele;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simulator that injects variants defined from {@link Phenopacket} among variants present in single VCF file.
 */
public class VcfSimulator {

    private static final OntologyClass HET = OntologyClass.newBuilder().setId("GENO:0000135").setLabel("heterozygous").build();

    private static final OntologyClass HOM_ALT = OntologyClass.newBuilder().setId("GENO:0000136").setLabel("homozygous").build();

    private static final OntologyClass HEMIZYGOUS = OntologyClass.newBuilder().setId("GENO:0000134").setLabel("hemizygous").build();

    private static final Pattern INFO_BIFIELD = Pattern.compile("(\\w+)=(-?[\\w.]+)");

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfSimulator.class);
    /** Path to the file into which we will inject a mutation from a Phenopacket. */
    private final Path templateVcfPath;

    /**
     * @param templateVcfPath {@link Path} to possibly un-indexed VCF file
     */
    public VcfSimulator(Path templateVcfPath) {
        this.templateVcfPath = templateVcfPath;
    }


    static VCFHeader updateHeaderWithPhenopacketSample(VCFHeader original, String sampleId) {
        return new VCFHeader(original.getMetaDataInSortedOrder(), Collections.singleton(sampleId));
    }

    static UnaryOperator<VariantContext> changeSampleNameInGenotypes(final String sampleId) {
        return vc -> {
            final VariantContextBuilder vcb = new VariantContextBuilder(vc)
                    .noGenotypes() // remove present genotypes and then add updated
                    .genotypes(vc.getGenotypes().stream()
                            .map(gt -> new GenotypeBuilder(gt).name(sampleId).make()) // change sample Id on individual genotypes
                            .collect(Collectors.toList()));

            return vcb.make();
        };
    }

    /**
     * Map {@link Phenopacket} to {@link VariantContext}s. Genotypes in variant contexts are modified so that they
     * will contain phenopacket subject's id.
     */
    private static List<VariantContext> phenopacketToVariantContexts(String subjectId, List<Variant> variants) {
        List<VariantContext> vcs = new ArrayList<>();
        for (Variant variant : variants) {

            switch (variant.getAlleleCase()) {
                case ALLELE_NOT_SET:
                case SPDI_ALLELE:
                case ISCN_ALLELE:
                case HGVS_ALLELE:
                default:
                    LOGGER.warn("Variant data are not stored in VCF format, but as {}", variant.getAlleleCase());
                    continue;
                case VCF_ALLELE:
                    // continue execution
            }

            final VcfAllele vcfAllele = variant.getVcfAllele();



            // here the ref allele is always at 0, alt is at idx 1
            List<Allele> allAlleles = new ArrayList<>(2);
            allAlleles.add(Allele.create(vcfAllele.getRef(), true));
            allAlleles.add(Allele.create(vcfAllele.getAlt()));

            OntologyClass zygosity = variant.getZygosity();

            LOGGER.info(vcfAllele.getChr() + ":"+ vcfAllele.getPos() + vcfAllele.getRef() +"-"+vcfAllele.getAlt()
                    +" ["+ zygosity.getLabel() +"]" +" subject" + subjectId );



            GenotypeBuilder genotypeBuilder = new GenotypeBuilder()
                    .name(subjectId);

            if (zygosity.equals(HET)) {
                // 1x REF + 1x ALT
                genotypeBuilder.alleles(Arrays.asList(allAlleles.get(0), allAlleles.get(1)));
            } else if (zygosity.equals(HOM_ALT)) {
                // 2x ALT
                genotypeBuilder.alleles(Arrays.asList(allAlleles.get(1), allAlleles.get(1)));
            } else if (zygosity.equals(HEMIZYGOUS)) {
                genotypeBuilder.alleles(Collections.singletonList(allAlleles.get(1)));
            } else {
                LOGGER.warn("Unknown genotype '{}'. Tried HET, HOM_ALT, HEMIZYGOUS", zygosity);
                continue;
            }

            // INFO fields

            Map<String, Object> infoFields = new HashMap<>();
            for (String s : vcfAllele.getInfo().split(";")) {
                Matcher bifield = INFO_BIFIELD.matcher(s);
                if (bifield.matches()) {
                    infoFields.put(bifield.group(1), bifield.group(2));
                } else {
                    infoFields.put(s, null);
                }
            }

            VariantContext vc = new VariantContextBuilder()
                    // we are working with hg19 usually. Contigs are prepended with 'chr' there
                    .chr(vcfAllele.getChr().startsWith("chr") ? vcfAllele.getChr() : "chr" + vcfAllele.getChr())
                    .start(vcfAllele.getPos())
                    .computeEndFromAlleles(allAlleles, vcfAllele.getPos())
                    .alleles(allAlleles)
                    .genotypes(genotypeBuilder.make())
                    .attributes(infoFields)
                    .noID()
                    .make();

            vcs.add(vc);
        }
        return vcs;
    }

    /**
     *
     * @param subjectId identifier of the proband in the VCF file
     * @param variants List of variants we will add to the VCF file
     * @return HtsFile object
     * @throws IOException if the template VCF file cannot be read
     */
    public HtsFile simulateVcf(String subjectId, List<Variant> variants, String genomeAssembly) throws IOException {
        Objects.requireNonNull(subjectId, "Subject ID must not be null");
        if (subjectId.isEmpty()) {
            //throw new LiricalRuntimeException("Subject ID must not be empty");
            System.err.println("[WARNING] Subject ID not found for ");
            subjectId = "n/a";
        }

        // we create a temporary VCF file for LIRICAL analysis
        final File outPath = File.createTempFile("single-vcf-simulator-" + subjectId.replaceAll(" ","_") + "-", ".vcf");
        outPath.deleteOnExit();



        try (VCFFileReader reader = new VCFFileReader(templateVcfPath, false);
             VariantContextWriter writer = new VariantContextWriterBuilder()
                     .setOutputFile(outPath)
                     .setOutputFileType(VariantContextWriterBuilder.OutputType.VCF)
                     .unsetOption(Options.INDEX_ON_THE_FLY)
                     .setOption(Options.ALLOW_MISSING_FIELDS_IN_HEADER) // important for
                     .build()) {
            LOGGER.info("Reading file {}", templateVcfPath);
            VCFHeader fileHeader = reader.getFileHeader();
            fileHeader = updateHeaderWithPhenopacketSample(fileHeader, subjectId);
            writer.writeHeader(fileHeader);

            List<VariantContext> injected = phenopacketToVariantContexts(subjectId, variants);

            AtomicInteger cnt = new AtomicInteger();
            Stream.concat(reader.iterator().stream(), injected.stream())
                    .map(changeSampleNameInGenotypes(subjectId))
                    .sorted(new VariantContextComparator(fileHeader.getContigLines()))
                    .peek(vc -> cnt.incrementAndGet())
                    .forEach(writer::add);
            LOGGER.info("Created VCF containing {} variants", cnt.get());
        }

        // make description
        String description = String.format("Simulated VCF file based on a template VCF at '%s'file.", templateVcfPath);

        return HtsFile.newBuilder()
                .setHtsFormat(HtsFile.HtsFormat.VCF)
                .setGenomeAssembly(genomeAssembly)
//                 individual_to_sample_identifiers not set here, this is a naive thing for now
//                .putAllIndividualToSampleIdentifiers()
                .setFile(org.phenopackets.schema.v1.core.File.newBuilder()
                        .setPath(outPath.getAbsolutePath())
                        .setDescription(description)
                        .build())
                .build();
    }
}
