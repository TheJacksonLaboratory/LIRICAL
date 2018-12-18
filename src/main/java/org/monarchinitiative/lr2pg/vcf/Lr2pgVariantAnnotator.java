package org.monarchinitiative.lr2pg.vcf;


import de.charite.compbio.jannovar.annotation.Annotation;
import de.charite.compbio.jannovar.annotation.VariantAnnotations;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.reference.GenomeVariant;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.VariantAnnotator;
import org.monarchinitiative.exomiser.core.model.AllelePosition;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.VariantAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lr2pgVariantAnnotator implements VariantAnnotator {
    private static final Logger logger = LoggerFactory.getLogger(Lr2pgVariantAnnotator.class);
    private final GenomeAssembly genomeAssembly;
    private final JannovarAnnotationService jannovarAnnotationService;

    public Lr2pgVariantAnnotator(GenomeAssembly genomeAssembly, JannovarData jannovarData) {
        this.genomeAssembly = genomeAssembly;
        this.jannovarAnnotationService = new JannovarAnnotationService(jannovarData);
    }

    public VariantAnnotation annotate(String contig, int pos, String ref, String alt) {
        AllelePosition trimmedAllele = AllelePosition.trim(pos, ref, alt);
        VariantAnnotations variantAnnotations = this.jannovarAnnotationService.annotateVariant(contig, trimmedAllele.getPos(), trimmedAllele.getRef(), trimmedAllele.getAlt());
        return this.buildVariantAlleleAnnotation(this.genomeAssembly, contig, trimmedAllele, variantAnnotations);
    }

    private VariantAnnotation buildVariantAlleleAnnotation(GenomeAssembly genomeAssembly, String contig, AllelePosition allelePosition, VariantAnnotations variantAnnotations) {
        int chr = variantAnnotations.getChr();
        GenomeVariant genomeVariant = variantAnnotations.getGenomeVariant();
        String chromosomeName = genomeVariant.getChrName() == null ? contig : genomeVariant.getChrName();
        Annotation highestImpactAnnotation = variantAnnotations.getHighestImpactAnnotation();
        String geneSymbol = this.buildGeneSymbol(highestImpactAnnotation);
        String geneId = this.buildGeneId(highestImpactAnnotation);
        VariantEffect variantEffect = variantAnnotations.getHighestImpactEffect();
        List<TranscriptAnnotation> annotations = this.buildTranscriptAnnotations(variantAnnotations.getAnnotations());
        int pos = allelePosition.getPos();
        String ref = allelePosition.getRef();
        String alt = allelePosition.getAlt();
        return VariantAnnotation.builder().genomeAssembly(genomeAssembly).chromosome(chr).chromosomeName(chromosomeName).position(pos).ref(ref).alt(alt).geneId(geneId).geneSymbol(geneSymbol).variantEffect(variantEffect).annotations(annotations).build();
    }

    private List<TranscriptAnnotation> buildTranscriptAnnotations(List<Annotation> annotations) {
        List<TranscriptAnnotation> transcriptAnnotations = new ArrayList<>(annotations.size());
        Iterator iter = annotations.iterator();

        while(iter.hasNext()) {
            Annotation annotation = (Annotation)iter.next();
            transcriptAnnotations.add(this.toTranscriptAnnotation(annotation));
        }

        return transcriptAnnotations;
    }

    private TranscriptAnnotation toTranscriptAnnotation(Annotation annotation) {
        return TranscriptAnnotation.builder().variantEffect(annotation.getMostPathogenicVarType()).accession(this.getTranscriptAccession(annotation)).geneSymbol(this.buildGeneSymbol(annotation)).hgvsGenomic(annotation.getGenomicNTChange() == null ? "" : annotation.getGenomicNTChangeStr()).hgvsCdna(annotation.getCDSNTChangeStr()).hgvsProtein(annotation.getProteinChangeStr()).distanceFromNearestGene(this.getDistFromNearestGene(annotation)).build();
    }

    private String getTranscriptAccession(Annotation annotation) {
        TranscriptModel transcriptModel = annotation.getTranscript();
        return transcriptModel == null ? "" : transcriptModel.getAccession();
    }

    private int getDistFromNearestGene(Annotation annotation) {
        TranscriptModel tm = annotation.getTranscript();
        if (tm == null) {
            return -2147483648;
        } else {
            GenomeVariant change = annotation.getGenomeVariant();
            Set<VariantEffect> effects = annotation.getEffects();
            if (!effects.contains(VariantEffect.INTERGENIC_VARIANT) && !effects.contains(VariantEffect.UPSTREAM_GENE_VARIANT) && !effects.contains(VariantEffect.DOWNSTREAM_GENE_VARIANT)) {
                return -2147483648;
            } else {
                return change.getGenomeInterval().isLeftOf(tm.getTXRegion().getGenomeBeginPos()) ? tm.getTXRegion().getGenomeBeginPos().differenceTo(change.getGenomeInterval().getGenomeEndPos()) : change.getGenomeInterval().getGenomeBeginPos().differenceTo(tm.getTXRegion().getGenomeEndPos());
            }
        }
    }

    private String buildGeneId(Annotation annotation) {
        if (annotation == null) {
            return "";
        } else {
            TranscriptModel transcriptModel = annotation.getTranscript();
            if (transcriptModel == null) {
                return "";
            } else {
                String geneId = transcriptModel.getGeneID();
                return geneId == null ? "" : geneId;
            }
        }
    }

    private String buildGeneSymbol(Annotation annotation) {
        return annotation != null && annotation.getGeneSymbol() != null ? annotation.getGeneSymbol() : ".";
    }
}

