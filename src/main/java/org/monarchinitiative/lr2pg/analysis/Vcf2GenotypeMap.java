package org.monarchinitiative.lr2pg.analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.htsjdk.VariantContextAnnotator;
import de.charite.compbio.jannovar.progress.ProgressReporter;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.JannovarVariantAnnotator;
import org.monarchinitiative.exomiser.core.genome.dao.serialisers.MvStoreUtil;
import org.monarchinitiative.exomiser.core.model.*;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.VariantEffectPathogenicityScore;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.util.*;

/**
 * This class is responsible for parsing the VCF file and extracting variants and genotypes. Its
 * main org.monarchinitiative.lr2pg.output is the map in {@link #gene2genotypeMap}.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Vcf2GenotypeMap {
    private static final Logger logger = LoggerFactory.getLogger(Vcf2GenotypeMap.class);
    /**
     * Path to the VCF file with the exome/genome of the proband.
     */
    private final String vcfPath;
    /**
     * Prefix for the NCBI Entrez Gene data.
     */
    private final String NCBI_ENTREZ_GENE_PREFIX = "NCBIGene";
    /**
     * We will assume a frequency of 1:100,000 if no frequency data is available.
     */
    private final float DEFAULT_FREQUENCY = 0.00001F;
    /**
     * Reference to the Jannovar transcript file data for annotating the VCF file.
     */
    private final JannovarData jannovarData;
    /**
     * Reference dictionary that is part of {@link #jannovarData}.
     */
    private final ReferenceDictionary referenceDictionary;
    /**
     * Map of Chromosomes, used in the annotation.
     */
    private final ImmutableMap<Integer, Chromosome> chromosomeMap;
    /**
     * A Jannovar object to report progress of VCF parsing.
     */
    private ProgressReporter progressReporter = null;
    /**
     * Should be hg37 or hg38
     */
    private final GenomeAssembly genomeAssembly;
    /** If true, filter VCF lines by the FILTER column (variants pass if there is no entry, i.e., ".",
     * or if the value of the field is FALSE. Variant also fail if a reason for the not passing the
     * filter is given in the column, i.e., for allelic imbalance. This is true by default. Filtering
     * can be turned off by entering {@code -q false} or {@code --quality} false. */
    private final boolean filterOnFILTER;
    /** Number of variants that were not removed because of the quality filter. */
    private int n_good_quality_variants=0;
    /** Number of variants that were removed because of the quality filter. */
    private int n_filtered_variants=0;

   // private final Map<String,String> vcfMetaData=new HashMap<>();
    /**
     * Key: an EntrezGene gene id; value a {@link Gene2Genotype} obhject with variants/genotypes in this gene.
     */
    private Map<TermId, Gene2Genotype> gene2genotypeMap;
    /** Number of samples in the VCF file. */
    private int n_samples;
    /** Name of the proband in the VCF file. */
    private String samplename;
    /** List of all names in the VCF file */
    private List<String> samplenames;


    /**
     * A map with data from the Exomiser database.
     */
    private final MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap;
    /**
     * A set of interpretation classes from ClinVar that we will regard as pathogenic.
     */
    private static final Set<ClinVarData.ClinSig> PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS =
            Sets.immutableEnumSet(ClinVarData.ClinSig.PATHOGENIC,
                    ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC,
                    ClinVarData.ClinSig.LIKELY_PATHOGENIC);


    public Vcf2GenotypeMap(String vcf, JannovarData jannovar, MVStore mvs, GenomeAssembly ga, boolean filter) {
        this.vcfPath = vcf;
        this.jannovarData = jannovar;
        this.alleleMap = MvStoreUtil.openAlleleMVMap(mvs);
        this.referenceDictionary = jannovarData.getRefDict();
        this.chromosomeMap = jannovarData.getChromosomes();
        this.genomeAssembly = ga;
        this.filterOnFILTER=filter;
    }

    /** map with some information about the VCF file that will be shown on the hTML org.monarchinitiative.lr2pg.output. */
//    public Map<String, String > getVcfMetaData() {
//        return vcfMetaData;
//    }

    public Map<TermId, Gene2Genotype> vcf2genotypeMap() {
        // whether or not to just look at a specific genomic interval
        final boolean useInterval = false;
        this.gene2genotypeMap = new HashMap<>();
        try (VCFFileReader vcfReader = new VCFFileReader(new File(vcfPath), useInterval)) {
            //final SAMSequenceDictionary seqDict = VCFFileReader.getSequenceDictionary(new File(vcfPath));
            VCFHeader vcfHeader = vcfReader.getFileHeader();
            this.samplenames = vcfHeader.getSampleNamesInOrder();
            this.n_samples=samplenames.size();
            this.samplename=samplenames.get(0);
            logger.trace("Annotating VCF at " + vcfPath + " for sample " + this.samplename);
            final long startTime = System.nanoTime();
            CloseableIterator<VariantContext> iter = vcfReader.iterator();

            VariantContextAnnotator variantEffectAnnotator =
                    new VariantContextAnnotator(this.referenceDictionary, this.chromosomeMap,
                            new VariantContextAnnotator.Options());
            // Note that we do not use Genomiser data in this version of LR2PG
            // Therefore, just pass in an empty list to satisfy the API
            List<RegulatoryFeature> emtpylist = ImmutableList.of();
            ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
            JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly, jannovarData, emptyRegionIndex);
            while (iter.hasNext()) {
                VariantContext vc = iter.next();
                if (vc.isFiltered()) {
                    // this is a failing VariantContext
                    n_filtered_variants++;
                    continue;
                } else {
                    n_good_quality_variants++;
                }
                vc = variantEffectAnnotator.annotateVariantContext(vc);
                List<Allele> altAlleles = vc.getAlternateAlleles();
                String contig = vc.getContig();
                int start = vc.getStart();
                String ref = vc.getReference().getBaseString();
                for (Allele allele : altAlleles) {
                    String alt = allele.getBaseString();
                    VariantAnnotation va = jannovarVariantAnnotator.annotate(contig, start, ref, alt);
                    VariantEffect variantEffect = va.getVariantEffect();
                    if (!variantEffect.isOffExome()) {
                        String genIdString = va.getGeneId(); // for now assume this is an Entrez Gene ID
                        String symbol = va.getGeneSymbol();
                        TermId geneId;
                        try {
                            geneId = TermId.of(NCBI_ENTREZ_GENE_PREFIX, genIdString);
                        } catch (PhenolRuntimeException pre) {
                           // logger.error("Could not identify gene \"{}\" with symbol \"{}\" for variant {}", genIdString,symbol,va.toString());
                           // if gene is not included in the Jannovar file then it is not a Mendelian
                            // disease gene, e.g., abParts.
                            // Therefore just skip it
                            continue;
                        }
                        gene2genotypeMap.putIfAbsent(geneId, new Gene2Genotype(geneId, symbol));
                        Gene2Genotype genotype = gene2genotypeMap.get(geneId);
                        VariantEvaluation veval = buildVariantEvaluation(vc, va);
                        AlleleProto.AlleleKey alleleKey = AlleleProtoAdaptor.toAlleleKey(veval);
                        AlleleProto.AlleleProperties alleleProp = alleleMap.get(alleleKey);
                        int chrom = veval.getChromosome();
                        int pos = veval.getPosition();
                        List<TranscriptAnnotation> transcriptAnnotationList = veval.getTranscriptAnnotations();
                        String genotypeString = veval.getGenotypeString();
                        float freq;
                        float path;
                        if (alleleProp == null) {
                            // this means the variant is not represented in the Exomiser data
                            // this is not an error, the variant could be very rare or otherwise not seen before
                            freq = DEFAULT_FREQUENCY;
                            path = VariantEffectPathogenicityScore.getPathogenicityScoreOf(variantEffect);
                            genotype.addVariant(chrom, pos, ref, alt, transcriptAnnotationList, genotypeString, path, freq, ClinVarData.ClinSig.NOT_PROVIDED);
                        } else {
                            FrequencyData frequencyData = AlleleProtoAdaptor.toFrequencyData(alleleProp);
                            PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProp);
                            freq = frequencyData.getMaxFreq();
                            float pathogenicity = calculatePathogenicity(variantEffect, pathogenicityData);
                            ClinVarData cVarData = pathogenicityData.getClinVarData();
                            genotype.addVariant(chrom, pos, ref, alt, transcriptAnnotationList, genotypeString, pathogenicity, freq, cVarData.getPrimaryInterpretation());
                        }

                    }
                }
            }
            final long endTime = System.nanoTime();

            logger.info(String.format("Finished Annotating VCF (time= %.2f sec).", (endTime-startTime)/1_000_000_000.0 ));
            logger.info("Extracted {} non-filtered variants and {} variants that were removed because of a quality filter",
                    n_good_quality_variants,n_filtered_variants);
        }

        return gene2genotypeMap;
    }

    public int getN_samples() {
        return n_samples;
    }

    public String getSamplename() {
        return samplename;
    }

    public List<String> getSamplenames() {
        return samplenames;
    }

    public GenomeAssembly getGenomeAssembly() {
        return genomeAssembly;
    }

    public int getN_good_quality_variants() {
        return n_good_quality_variants;
    }

    public int getN_filtered_variants() {
        return n_filtered_variants;
    }

    /**
     * Calculate a pathogenicity score for the current variant in the same way that the Exomiser does.
     *
     * @param variantEffect     class of variant such as Missense, Nonsense, Synonymous, etc.
     * @param pathogenicityData Object representing the predicted pathogenicity of the data.
     * @return the predicted pathogenicity score.
     */
    private float calculatePathogenicity(VariantEffect variantEffect, PathogenicityData pathogenicityData) {
        float variantEffectScore = VariantEffectPathogenicityScore.getPathogenicityScoreOf(variantEffect);
        if (pathogenicityData.isEmpty()) return variantEffectScore;
        float predictedScore = pathogenicityData.getScore();
        switch (variantEffect) {
            case MISSENSE_VARIANT:
                return pathogenicityData.hasPredictedScore() ? predictedScore : variantEffectScore;
            case SYNONYMOUS_VARIANT:
                // there are cases where synonymous variants have been assigned a high MutationTaster score.
                // These looked to have been wrongly mapped and are therefore probably wrong. So we'll use the default score for these.
                return variantEffectScore;
            default:
                return Math.max(predictedScore, variantEffectScore);
        }
    }


    private VariantEvaluation buildVariantEvaluation(VariantContext variantContext, VariantAnnotation variantAnnotation) {

        GenomeAssembly genomeAssembly = variantAnnotation.getGenomeAssembly();
        int chr = variantAnnotation.getChromosome();
        String chromosomeName = variantAnnotation.getChromosomeName();
        int pos = variantAnnotation.getPosition();
        String ref = variantAnnotation.getRef();
        String alt = variantAnnotation.getAlt();

        String geneSymbol = variantAnnotation.getGeneSymbol();
        String geneId = variantAnnotation.getGeneId();
        VariantEffect variantEffect = variantAnnotation.getVariantEffect();
        List<TranscriptAnnotation> annotations = variantAnnotation.getTranscriptAnnotations();

        return VariantEvaluation.builder(chr, pos, ref, alt)
                .genomeAssembly(genomeAssembly)
                //HTSJDK derived data are used for writing out the
                //HTML (VariantEffectCounter) VCF/TSV-VARIANT formatted files
                //can be removed from InheritanceModeAnalyser as Jannovar 0.18+ is not reliant on the VariantContext
                //need most/all of the info in order to write it all out again.
                //If we could remove this direct dependency the RAM usage can be halved such that a SPARSE analysis of the POMP sample can be held comfortably in 8GB RAM
                //To do this we could just store the string value here - it can be re-hydrated later. See TestVcfParser
                .variantContext(variantContext)
                //.numIndividuals(variantContext.getNSamples())
                //quality is the only value from the VCF file directly required for analysis
                .quality(variantContext.getPhredScaledQual())
                //jannovar derived data
                .chromosomeName(chromosomeName)
                .geneSymbol(geneSymbol)
                //This used to be an ENTREZ gene identifier, but could now be anything.
                .geneId(geneId)
                .variantEffect(variantEffect)
                .annotations(annotations)
                .build();
    }

}
