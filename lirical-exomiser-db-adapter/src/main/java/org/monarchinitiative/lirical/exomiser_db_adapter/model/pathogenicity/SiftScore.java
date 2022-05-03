/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2019 Queen Mary University of London.
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
package org.monarchinitiative.lirical.exomiser_db_adapter.model.pathogenicity;

/**
 * The score is the normalized probability that the amino acid change is
 * tolerated. SIFT predicts substitutions with scores less than 0.05 as
 * deleterious. Some SIFT users have found that substitutions with scores less
 * than 0.1 provide better sensitivity for detecting deleterious SNPs.
 *
 * Single nucleotide polymorphism (SNP) studies and random mutagenesis projects
 * identify amino acid substitutions in protein-coding regions. Each
 * substitution has the potential to affect protein function. SIFT (Sorting
 * Intolerant From Tolerant) is a program that predicts whether an amino acid
 * substitution affects protein function so that users can prioritize
 * substitutions for further study. It has been shown that SIFT can distinguish
 * between functionally neutral and deleterious amino acid changes in
 * mutagenesis studies and on human polymorphisms. SIFT is available <a href="http://blocks.fhcrc.org/sift/SIFT.html">here</a>.
 *
 *
 * <a href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC168916/">Publication</a>.
 *
 * @author Jules Jacobsen
 */
public class SiftScore extends ScaledPathogenicityScore {

    /**
     * A SIFT score below this threshold is considered to be pathogenic
     */
    public static final float SIFT_THRESHOLD = 0.06f;

    public static SiftScore of(float score) {
        return new SiftScore(score);
    }

    private SiftScore(float score) {
        super(PathogenicitySource.SIFT, score, 1 - score);
    }

    @Override
    public String toString() {
        if (rawScore < SiftScore.SIFT_THRESHOLD) {
            return String.format("SIFT: %.3f (D)", rawScore);
        } else {
            return String.format("SIFT: %.3f (T)", rawScore);
        }
    }

}
