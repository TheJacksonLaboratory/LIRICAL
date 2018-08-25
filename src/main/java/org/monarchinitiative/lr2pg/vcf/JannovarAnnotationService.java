/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2017 Queen Mary University of London.
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

package org.monarchinitiative.lr2pg.vcf;

import de.charite.compbio.jannovar.annotation.VariantAnnotations;
import de.charite.compbio.jannovar.annotation.VariantAnnotator;
import de.charite.compbio.jannovar.annotation.builders.AnnotationBuilderOptions;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.GenomeVariant;
import de.charite.compbio.jannovar.reference.PositionType;
import de.charite.compbio.jannovar.reference.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper to build Jannovar annotations for variants. CAUTION! This class returns native Jannovar objects which use zero-based
 * coordinates.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class JannovarAnnotationService {

    private static final Logger logger = LoggerFactory.getLogger(JannovarAnnotationService.class);

    //in cases where a variant cannot be positioned on a chromosome we're going to use 0 in order to fulfil the
    //requirement of a variant having an integer chromosome
    private static final int UNKNOWN_CHROMOSOME = 0;

    private final ReferenceDictionary referenceDictionary;
    private final VariantAnnotator variantAnnotator;


    public JannovarAnnotationService(JannovarData jannovarData) {
        this.referenceDictionary = jannovarData.getRefDict();
        this.variantAnnotator = new VariantAnnotator(jannovarData.getRefDict(), jannovarData.getChromosomes(), new AnnotationBuilderOptions());

    }

    /**
     * Takes VCF (forward-strand, one-based) style variants and returns a set of Jannovar {@link VariantAnnotations}.
     *
     * @param contig
     * @param pos
     * @param ref
     * @param alt
     * @return a set of {@link VariantAnnotations} for the given variant coordinates. CAUTION! THE RETURNED ANNOTATIONS
     * WILL USE ZERO-BASED COORDINATES AND WILL BE TRIMMED LEFT SIDE FIRST, ie. RIGHT SHIFTED. This is counter to VCF
     * conventions.
     */
    public VariantAnnotations annotateVariant(String contig, int pos, String ref, String alt) {
        GenomeVariant genomeVariant = buildOneBasedFwdStrandGenomicVariant(contig, pos, ref, alt);
        return annotateGenomeVariant(genomeVariant);
    }

    private GenomeVariant buildOneBasedFwdStrandGenomicVariant(String contig, int pos, String ref, String alt) {
        int chr = getIntValueOfChromosomeOrZero(contig);
        GenomePosition genomePosition = new GenomePosition(referenceDictionary, Strand.FWD, chr, pos, PositionType.ONE_BASED);
        return new GenomeVariant(genomePosition, ref, alt);
    }

    private Integer getIntValueOfChromosomeOrZero(String contig) {
        return referenceDictionary.getContigNameToID().getOrDefault(contig, UNKNOWN_CHROMOSOME);
    }

    private VariantAnnotations annotateGenomeVariant(GenomeVariant genomeVariant) {
        if (genomeVariant.getChr() == UNKNOWN_CHROMOSOME) {
            //Need to check this here and return otherwise the variantAnnotator will throw a NPE.
            return VariantAnnotations.buildEmptyList(genomeVariant);
        }
        try {
            return variantAnnotator.buildAnnotations(genomeVariant);
        } catch (Exception e) {
            logger.debug("Unable to annotate variant {}-{}-{}-{}",
                    genomeVariant.getChrName(),
                    genomeVariant.getPos(),
                    genomeVariant.getRef(),
                    genomeVariant.getAlt(),
                    e);
        }
        return VariantAnnotations.buildEmptyList(genomeVariant);
    }

}
