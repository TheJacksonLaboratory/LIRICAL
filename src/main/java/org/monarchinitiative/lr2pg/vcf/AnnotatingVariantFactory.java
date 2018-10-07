package org.monarchinitiative.lr2pg.vcf;
/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2018 Queen Mary University of London.
 * Copyright (c) 2012-2016 Charité Universitätsmedizin Berlin and Genome Research Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.VariantAnnotator;
import org.monarchinitiative.exomiser.core.genome.VariantDataService;
import org.monarchinitiative.exomiser.core.genome.VariantFactory;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.VariantAnnotation;
import org.monarchinitiative.exomiser.core.model.VariantEvaluation;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class AnnotatingVariantFactory implements VariantFactory {

    private static final Logger logger = LoggerFactory.getLogger(AnnotatingVariantFactory.class);
    private static final Set<FrequencySource> ALL_EXTERNAL_FREQ_SOURCES = FrequencySource.ALL_EXTERNAL_FREQ_SOURCES;
//    private static final ImmutableSet<PathogenicitySource> ALL_PATHOGENICITY_SOURCES = Sets.immutableEnumSet(Arrays
//            .asList(PathogenicitySource.values()));
private static final ImmutableSet<PathogenicitySource> DEFAULT_SOURCES = Sets.immutableEnumSet(Arrays.asList(PathogenicitySource.MUTATION_TASTER, PathogenicitySource.POLYPHEN, PathogenicitySource.SIFT));

    private final VariantAnnotator variantAnnotator;
    private final VariantDataService variantDataService;

    public AnnotatingVariantFactory(VariantAnnotator variantAnnotator, VariantDataService variantDataService) {
        this.variantAnnotator = variantAnnotator;
        this.variantDataService = variantDataService;
    }

    @Override
    public Stream<VariantEvaluation> createVariantEvaluations(Stream<VariantContext> variantContextStream) {
        logger.info("Annotating variant records, trimming sequences and normalising positions...");
        return variantContextStream
                .flatMap(annotateVariantEvaluations());
    }

    private Function<VariantContext, Stream<VariantEvaluation>> annotateVariantEvaluations() {
        return variantContext -> variantContext.getAlternateAlleles().stream()
                .map(buildAlleleVariantEvaluation(variantContext))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Function<Allele, Optional<VariantEvaluation>> buildAlleleVariantEvaluation(VariantContext variantContext) {
        return allele -> {
            //alternate Alleles are always after the reference allele, which is 0
            int altAlleleId = variantContext.getAlleleIndex(allele) - 1;
            return Optional.of(buildVariantEvaluation(variantContext, altAlleleId));
        };
    }

    /**
     * Creates a VariantEvaluation made from all the relevant bits of the
     * VariantContext and VariantAnnotations for a given alternative allele.
     *
     * @param variantContext
     * @param altAlleleId
     * @return
     */
    //This is package-private as it is used by the TestVariantFactory
    VariantEvaluation buildVariantEvaluation(VariantContext variantContext, int altAlleleId) {
        VariantAnnotation variantAnnotation = annotateVariantAllele(variantContext, altAlleleId);
        return buildVariantEvaluation(variantContext, altAlleleId, variantAnnotation);
    }

    private VariantAnnotation annotateVariantAllele(VariantContext variantContext, int altAlleleId) {
        String contig = variantContext.getContig();
        int pos = variantContext.getStart();
        String ref = variantContext.getReference().getBaseString();
        String alt = variantContext.getAlternateAllele(altAlleleId).getBaseString();
        return variantAnnotator.annotate(contig, pos, ref, alt);
    }

    private VariantEvaluation buildVariantEvaluation(VariantContext variantContext, int altAlleleId, VariantAnnotation variantAnnotation) {

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

        FrequencyData frequencyData = variantDataService.getVariantFrequencyData(variantAnnotation, ALL_EXTERNAL_FREQ_SOURCES);
        PathogenicityData pathogenicityData = variantDataService.getVariantPathogenicityData(variantAnnotation, DEFAULT_SOURCES);

        return VariantEvaluation.builder(chr, pos, ref, alt)
                .genomeAssembly(genomeAssembly)
                //HTSJDK derived data are used for writing out the
                //HTML (VariantEffectCounter) VCF/TSV-VARIANT formatted files
                //can be removed from InheritanceModeAnalyser as Jannovar 0.18+ is not reliant on the VariantContext
                //need most/all of the info in order to write it all out again.
                //If we could remove this direct dependency the RAM usage can be halved such that a SPARSE analysis of the POMP sample can be held comfortably in 8GB RAM
                //To do this we could just store the string value here - it can be re-hydrated later. See TestVcfParser
                .variantContext(variantContext)
                .altAlleleId(altAlleleId)
                .numIndividuals(variantContext.getNSamples())
                //quality is the only value from the VCF file directly required for analysis
                .quality(variantContext.getPhredScaledQual())
                //jannovar derived data
                .chromosomeName(chromosomeName)
                .geneSymbol(geneSymbol)
                //This used to be an ENTREZ gene identifier, but could now be anything.
                .geneId(geneId)
                .variantEffect(variantEffect)
                .annotations(annotations)
                .frequencyData(frequencyData)
                .pathogenicityData(pathogenicityData)
                .build();
    }
}