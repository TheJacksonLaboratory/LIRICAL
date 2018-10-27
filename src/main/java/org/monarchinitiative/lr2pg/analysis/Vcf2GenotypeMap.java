package org.monarchinitiative.lr2pg.analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import de.charite.compbio.jannovar.annotation.VariantAnnotator;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.annotation.builders.AnnotationBuilderOptions;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.htsjdk.VariantContextAnnotator;
import de.charite.compbio.jannovar.progress.GenomeRegionListFactoryFromSAMSequenceDictionary;
import de.charite.compbio.jannovar.progress.ProgressReporter;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.CloseableIterator;
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
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.File;
import java.util.*;

public class Vcf2GenotypeMap {

    private final String vcfPath;

    private final TermPrefix NCBI_ENTREZ_GENE_PREFIX=new TermPrefix("NCBIGene");

    /** We will assume a frequency of 1:100,000 if no frequency data is available. */
    private final float DEFAULT_FREQUENCY = 0.00001F;

    /** We will assume a frequency of 1:100,000 if no frequency data is available. */
    private final float DEFAULT_PATHOGENICITY = 0.0F;

    private final JannovarData jannovarData;

    private final ReferenceDictionary referenceDictionary;
    private final VariantAnnotator variantAnnotator;
    /** Map of Chromosomes, used in the annotation. */
    private final ImmutableMap<Integer, Chromosome> chromosomeMap;
    /** A Jannovar object to report progress of VCF parsing. */
    private ProgressReporter progressReporter = null;

    private final GenomeAssembly genomeAssembly;

    private Map<TermId, Gene2Genotype> gene2genotypeMap;


    private final MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap;

    private boolean verbose=true;

    /** A set of interpretation classes from ClinVar that we will regard as pathogenic. */
    private static final Set<ClinVarData.ClinSig> PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS =
            Sets.immutableEnumSet(ClinVarData.ClinSig.PATHOGENIC,
                    ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC,
                    ClinVarData.ClinSig.LIKELY_PATHOGENIC);


    public Vcf2GenotypeMap(String vcf, JannovarData jannovar, MVStore mvs, GenomeAssembly ga) {
        this.vcfPath=vcf;
        this.jannovarData=jannovar;
        this.alleleMap=MvStoreUtil.openAlleleMVMap(mvs);
        this.referenceDictionary = jannovarData.getRefDict();
        this.chromosomeMap=jannovarData.getChromosomes();
        this.genomeAssembly=ga;
        this.variantAnnotator = new VariantAnnotator(jannovarData.getRefDict(), jannovarData.getChromosomes(), new AnnotationBuilderOptions());

    }


    public Map<TermId, Gene2Genotype> vcf2genotypeMap() {
        // whether or not to require availability of an index
        final boolean useInterval = false;
        this.gene2genotypeMap=new HashMap<>();
        verbose=true;
        try (VCFFileReader vcfReader = new VCFFileReader(new File(vcfPath), useInterval)) {
            if (verbose) {
                final SAMSequenceDictionary seqDict = VCFFileReader.getSequenceDictionary(new File(vcfPath));
                if (seqDict != null) {
                    final GenomeRegionListFactoryFromSAMSequenceDictionary factory = new GenomeRegionListFactoryFromSAMSequenceDictionary();
                    this.progressReporter = new ProgressReporter(factory.construct(seqDict), 60);
                    this.progressReporter.printHeader();
                    this.progressReporter.start();
                } else {
                    System.err.println("Progress reporting does not work because VCF file is missing the contig "
                            + "lines in the header.");
                }
            }

            VCFHeader vcfHeader = vcfReader.getFileHeader();

            System.err.println("Annotating VCF...");
            final long startTime = System.nanoTime();

            // Jump to interval if given, otherwise start at beginning
            CloseableIterator<VariantContext> iter;
            System.err.println("Will read full input file");
            iter = vcfReader.iterator();

            // Add step for annotating with variant effect
//            VariantEffectHeaderExtender extender = new VariantEffectHeaderExtender();
//            extender.addHeaders(vcfHeader);
            VariantContextAnnotator variantEffectAnnotator =
                    new VariantContextAnnotator(this.referenceDictionary, this.chromosomeMap,
                            new VariantContextAnnotator.Options());
            VariantAnnotator van = variantEffectAnnotator.getAnnotator();

            //Lr2pgVariantAnnotator lpgannotator = new Lr2pgVariantAnnotator(genomeAssembly, jannovarData);
            List<RegulatoryFeature> emtpylist = ImmutableList.of();
            ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
            JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly,jannovarData,emptyRegionIndex);
            while (iter.hasNext()) {
                VariantContext vc = iter.next();
                vc = variantEffectAnnotator.annotateVariantContext(vc);
                // todo -- what about multiple alleles on one position?
                VariantAnnotation va = jannovarVariantAnnotator.annotate(vc.getContig(), vc.getStart(), vc.getReference().getBaseString(), vc.getAlternateAllele(0).getBaseString());
                VariantEffect variantEffect = va.getVariantEffect();
                if (!variantEffect.isOffExome()) {
                    String genIdString = va.getGeneId(); // for now assume this is an Entrez Gene ID
                    String symbol=va.getGeneSymbol();
                    TermId geneId=new TermId(NCBI_ENTREZ_GENE_PREFIX,genIdString);
                    gene2genotypeMap.putIfAbsent(geneId,new Gene2Genotype(geneId,symbol));
                    Gene2Genotype genotype = gene2genotypeMap.get(geneId);
                    VariantEvaluation veval = buildVariantEvaluation(vc,  va );
                    AlleleProto.AlleleKey alleleKey =AlleleProtoAdaptor.toAlleleKey(veval);
                    AlleleProto.AlleleProperties alleleProp = alleleMap.get(alleleKey);
                    int chrom = veval.getChromosome();
                    int pos = veval.getPosition();
                    String ref = veval.getRef();
                    String alt = veval.getAlt();
                    List<TranscriptAnnotation> transcriptAnnotationList = veval.getTranscriptAnnotations();
                    String genotypeString = veval.getGenotypeString();
                    float freq;
                    float path;
                    boolean isClinVarPath=false;
                    ClinVarData.ClinSig clinvarSig=null;
                    if (alleleProp==null) {
                        System.out.println("Allele prop is NULL for " + veval);
                        freq=DEFAULT_FREQUENCY;
                        path=VariantEffectPathogenicityScore.getPathogenicityScoreOf(variantEffect);
                        genotype.addVariant(chrom,pos,ref,alt,transcriptAnnotationList,genotypeString,path,freq);
                    } else {
                        FrequencyData frequencyData = AlleleProtoAdaptor.toFrequencyData(alleleProp);
                        PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProp);
                        freq=frequencyData.getMaxFreq();
                        float pathogenicity = calculatePathogenicity(variantEffect, pathogenicityData);
                        ClinVarData cVarData = pathogenicityData.getClinVarData();
                        genotype.addVariant(chrom,pos,ref,alt,transcriptAnnotationList,genotypeString,pathogenicity,freq, cVarData.getPrimaryInterpretation());
                    }

                }
            }

        }
        // now sort the variants by pathogenicity
        for (Gene2Genotype genot : this.gene2genotypeMap.values()) {
            genot.sortVariants();
        }
        debugPrintGenotypes();
        if (progressReporter != null)
            progressReporter.done();
        return gene2genotypeMap;
    }


    private void debugPrintGenotypes() {
        int i=0;
        for (TermId geneId : this.gene2genotypeMap.keySet()) {
            i++;
            Gene2Genotype gtype = gene2genotypeMap.get(geneId);
            if (gtype.hasPathogenicClinvarVar()) { System.err.print("CLINVAR"); }
            if (gtype.hasPredictedPathogenicVar()) {
                System.err.println(i + ") " + gtype);

            }
        }
        System.err.println("Number of genotypes " + gene2genotypeMap.size());

    }


    /**
     * Calculate a pathogenicity score for the current variant in the same way that the Exomiser does.
     * @param variantEffect class of variant such as Missense, Nonsense, Synonymous, etc.
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
