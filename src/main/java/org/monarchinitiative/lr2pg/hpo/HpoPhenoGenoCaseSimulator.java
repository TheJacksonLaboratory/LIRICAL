package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.util.*;

/**
 * This class is intended to try out some architectures for the genotype-phenotype
 * LR test. Much of the code is copied from {@link HpoCaseSimulator}
 */
public class HpoPhenoGenoCaseSimulator {
    private static final Logger logger = LogManager.getLogger();


    /** Object to evaluate the results of differential diagnosis by LR analysis. */
    private CaseEvaluator evaluator;

     private final int variantCount;

    private final double meanVariantPathogenicity;
    /** If true, we exchange each of the non-noise terms with a direct parent except if that would mean going to
     * the root of the phenotype ontology.
     */
    private boolean addTermImprecision = false;
    /** The proportion of cases at rank 1 in the current simulation */
    private double proportionAtRank1=0.0;




    private TermPrefix NCBI_GENE_PREFIX=new TermPrefix("NCBIGene");
    private final TermId entrezGeneId;



    /** Root term id in the phenotypic abnormality subontology. */
    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.constructWithPrefix("HP:0000118");

    /**
     * The constructor
     */
    public HpoPhenoGenoCaseSimulator(HpoOntology ontology,
                                     Map<TermId,HpoDisease> diseaseMap,
                                     Multimap<TermId,TermId> disease2geneMultimap,
                                     String entrezGeneNumber,
                                     int varcount,
                                     double varpath,
                                     List<TermId> hpoTerms,
                                     Map<TermId,Double> gene2backgroundFrequency)  {
        this.entrezGeneId = new TermId(NCBI_GENE_PREFIX,entrezGeneNumber);
        VcfSimulator genotypes = new VcfSimulator(disease2geneMultimap.keySet(), entrezGeneId,varcount,varpath);

        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoTerms)
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypes.getGenotypeMap())
                .gene2backgroundFrequency(gene2backgroundFrequency);
        this.evaluator = caseBuilder.build();


        this.variantCount = varcount;
        this.meanVariantPathogenicity = varpath;
    }




    public int evaluateCase(TermId diseaseId) throws Lr2pgException {
        evaluator.evaluate();
        TestResult result = evaluator.getResult( diseaseId);
        System.err.println(diseaseId.getIdWithPrefix() +" "+ result);
        return evaluator.getRank(diseaseId);
    }


}
