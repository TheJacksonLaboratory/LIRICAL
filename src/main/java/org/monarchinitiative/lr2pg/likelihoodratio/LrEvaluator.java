package org.monarchinitiative.lr2pg.likelihoodratio;

import org.monarchinitiative.lr2pg.hpo.HpoCaseOld;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;

import java.util.List;

/**
 * Likelihood ratio evaluator. This class coordinates the performance of the likelihood ratio test on
 * an {@link HpoCaseOld}.
 */
public class LrEvaluator {

    private final HpoCaseOld hpocase;
    private final List<HpoDisease> diseaselist;
    private final List<Double> pretestProbabilities;
    /** Reference to the Human Phenotype Ontology object. */
    private final HpoOntology ontology;


    public LrEvaluator(HpoCaseOld hpcase, List<HpoDisease> diseases, List<Double> pretestProb, HpoOntology ont) {
        this.hpocase=hpcase;
        this.diseaselist=diseases;
        this.pretestProbabilities=pretestProb;
        this.ontology=ont;
    }


}
