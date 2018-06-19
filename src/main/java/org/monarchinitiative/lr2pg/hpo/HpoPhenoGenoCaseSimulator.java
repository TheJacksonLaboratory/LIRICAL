package org.monarchinitiative.lr2pg.hpo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.likelihoodratio.LrEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.lr2pg.model.Model;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.util.*;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;

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
    private final Multimap<TermId,TermId> disease2geneMultimap;

    private final Map<TermId,Double> genotypeMap;

    /** List of HPO terms representing phenoytpic abnormalities. */
    private final List<TermId> hpoTerms;

    /** Object to evaluate the results of differential diagnosis by LR analysis. */
    private LrEvaluator evaluator;

    private Model model;


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
    private final TermId entrezGeneId;

    private Map<TermId,Double> gene2backgroundFrequency;


    /** Root term id in the phenotypic abnormality subontology. */
    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.constructWithPrefix("HP:0000118");

    /**
     * The constructor initializes {@link #ontology} and {@link #diseaseMap} and {@link #phenotypeterms}.
     */
    public HpoPhenoGenoCaseSimulator(HpoOntology ontology,
                                     Map<TermId,HpoDisease> diseaseMap,
                                     Multimap<TermId,TermId> disease2geneMultimap,
                                     String entrezGeneNumber,
                                     int varcount,
                                     double varpath,
                                     List<TermId> hpoTerms,
                                     Map<TermId,Double> gene2backgroundFrequency)  {
        this.variantCount = varcount;
        this.meanVariantPathogenicity = varpath;
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.disease2geneMultimap =disease2geneMultimap;
        this.entrezGeneId = new TermId(NCBI_GENE_PREFIX,entrezGeneNumber);
        this.bftfrequency = new BackgroundForegroundTermFrequency(ontology,diseaseMap);
        GenotypeCollection genotypes = new GenotypeCollection(disease2geneMultimap.keySet(), entrezGeneId,varcount,varpath);
        this.genotypeMap=genotypes.getGenotypeMap();
        this.hpoTerms=hpoTerms;

        this.model = new Model(varcount, varpath, entrezGeneId,hpoTerms, ontology,diseaseMap,disease2geneMultimap);
        this.model.setGenotypeCollection(genotypes);

        Set<TermId> descendents=getDescendents(ontology,PHENOTYPIC_ABNORMALITY);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (TermId t: descendents) {
            builder.add(t);
        }
        this.phenotypeterms=builder.build();
        this.gene2backgroundFrequency=gene2backgroundFrequency;
        this.model.setBackgroundFrequency(gene2backgroundFrequency);
    }



    public HpoOntology getOntology() {
        return ontology;
    }

    private void createCase(HpoDisease disease) throws Lr2pgException {
        if (disease==null) {
            throw new Lr2pgException("Attempt to create case from Null-value for disease");
        } else {
            this.model.setDiseaseCurie(disease.getDiseaseDatabaseId());
        }

        ImmutableList.Builder<TermId> termIdBuilder = new ImmutableList.Builder<>();
        termIdBuilder.addAll(this.hpoTerms);
        ImmutableList<TermId> termlist = termIdBuilder.build();
        HpoCase c = (new HpoCase.Builder(termlist)).build();
        this.model.setHpoCase(c);
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
        createCase(disease);
        // the following evaluates the case for each disease with equal pretest probabilities.
        this.evaluator = new LrEvaluator(this.model);
        evaluator.evaluate();
        TestResult result = evaluator.getResult( diseaseId);
        Lr2Svg l2svg = new Lr2Svg(this.currentCase,result,this.ontology);
        String fname = String.format("%s.svg",diseaseId.getIdWithPrefix().replace(':','_') );
        l2svg.writeSvg(fname);
        return evaluator.getRank(disease);
    }




    public void debugPrint() {
        logger.trace(String.format("Got %d terms and %d diseases",ontology.getAllTermIds().size(),
                diseaseMap.size()));
    }


}
