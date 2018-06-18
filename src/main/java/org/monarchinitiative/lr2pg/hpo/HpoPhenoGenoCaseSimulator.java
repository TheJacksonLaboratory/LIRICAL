package org.monarchinitiative.lr2pg.hpo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.likelihoodratio.LrEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.util.*;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * This class is intended to try out some architectures for the genotype-phenotype
 * LR test. Much of the code is copied from {@link HpoCaseSimulator}
 */
public class HpoPhenoGenoCaseSimulator {
    private static final Logger logger = LogManager.getLogger();
    /** An object representing the Human Phenotype Ontology */
    private HpoOntology ontology;
    /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
    private BackgroundForegroundTermFrequency bftfrequency;
    /** A list of all HPO term ids in the Phenotypic abnormality subontology. */
    private final ImmutableList<TermId> phenotypeterms;
    /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
    private final Map<TermId,HpoDisease> diseaseMap;
    /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
    private final Multimap<TermId,TermId> gene2diseaseMultimap;

    private final Map<TermId,Double> genotypeMap;

    /** List of HPO terms representing phenoytpic abnormalities. */
    private final List<TermId> hpoTerms;

    /** Object to evaluate the results of differential diagnosis by LR analysis. */
    private LrEvaluator evaluator;

    private final String geneSymbol;

    private final int variantCount;

    private final double meanVariantPathogenicity;
    /** If true, we exchange each of the non-noise terms with a direct parent except if that would mean going to
     * the root of the phenotype ontology.
     */
    private boolean addTermImprecision = false;
    /** The proportion of cases at rank 1 in the current simulation */
    private double proportionAtRank1=0.0;

    private HpoCase currentCase;

    private int n_terms_per_case=5;
    private int n_noise_terms=1;

    private TermPrefix NCBI_GENE_PREFIX=new TermPrefix("NCBIGene");


    /** Root term id in the phenotypic abnormality subontology. */
    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.constructWithPrefix("HP:0000118");

    /**
     * The constructor initializes {@link #ontology} and {@link #diseaseMap} and {@link #phenotypeterms}.
     */
    public HpoPhenoGenoCaseSimulator(HpoOntology ontology,
                                     Map<TermId,HpoDisease> diseaseMap,
                                     Multimap<TermId,TermId> gene2diseaseMultimap,
                                     String gene,
                                     int varcount,
                                     double varpath,
                                     List<TermId> hpoTerms)  {
        this.geneSymbol = gene;
        this.variantCount = varcount;
        this.meanVariantPathogenicity = varpath;
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.gene2diseaseMultimap=gene2diseaseMultimap;
        TermId geneId = new TermId(NCBI_GENE_PREFIX,gene);
        this.bftfrequency = new BackgroundForegroundTermFrequency(ontology,diseaseMap);
        GenotypeCollection genotypes = new GenotypeCollection(gene2diseaseMultimap.keySet(),geneId,varcount,varpath);
        this.genotypeMap=genotypes.getGenotypeMap();
        this.hpoTerms=hpoTerms;

        Set<TermId> descendents=getDescendents(ontology,PHENOTYPIC_ABNORMALITY);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (TermId t: descendents) {
            builder.add(t);
        }
        this.phenotypeterms=builder.build();
    }







    /**
     * This is a term that was observed in the simulated patient (note that it should not be a HpoTermId, which
     * contains metadata about the term in a disease entity, such as overall frequency. Instead, we are simulating an
     * individual patient and this is a definite observation.
     * @return a random term from the phenotype subontology.
     */
    private TermId getRandomPhenotypeTerm() {
        int n=phenotypeterms.size();
        int r = (int)Math.floor(n*Math.random());
        return phenotypeterms.get(r);
    }

    private TermId getRandomParentTerm(TermId tid) {
        Set<TermId> parents = getParentTerms(ontology,tid,false);
        int r = (int)Math.floor(parents.size()*Math.random());
        int i=0;
        for (TermId t : parents) {
            if (i==r) return t;
            i++;
        }
        // we should never get here
        // the following is to satisfy the compiler that we need to return something.
        return null;
    }



    public HpoOntology getOntology() {
        return ontology;
    }

    private HpoCase createCase(HpoDisease disease) throws Lr2pgException {
        if (disease==null) {
            throw new Lr2pgException("Attempt to create case from Null-value for disease");
        }

        ImmutableList.Builder<TermId> termIdBuilder = new ImmutableList.Builder<>();
        termIdBuilder.addAll(this.hpoTerms);
        ImmutableList<TermId> termlist = termIdBuilder.build();
        return new HpoCase.Builder(termlist).build();
    }


    public TestResult getResults(TermId diseaseId) throws Lr2pgException {
        if (this.evaluator==null) {
            int rank = evaluateCase(diseaseId);
            System.err.println(String.format("Rank for %s was %d", diseaseId.getIdWithPrefix(),rank));
        }
        return evaluator.getResult(diseaseId);
    }

    /**
     * @param diseaseCurie a term id for a disease id such as OMIM:600100
     * @return the corresponding {@link HpoDisease} object.
     */
    public HpoDisease name2disease(TermId diseaseCurie) {
        return diseaseMap.get(diseaseCurie);
    }


    public HpoCase getCurrentCase() {
        return currentCase;
    }

    public int evaluateCase(TermId diseaseId) throws Lr2pgException {
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        this.currentCase = createCase(disease);
        // the following evaluates the case for each disease with equal pretest probabilities.
        this.evaluator = new LrEvaluator(this.currentCase, diseaseMap,ontology,bftfrequency);
        evaluator.evaluate();
        return evaluator.getRank(disease);
    }




    public void debugPrint() {
        logger.trace(String.format("Got %d terms and %d diseases",ontology.getAllTermIds().size(),
                diseaseMap.size()));
    }


}
