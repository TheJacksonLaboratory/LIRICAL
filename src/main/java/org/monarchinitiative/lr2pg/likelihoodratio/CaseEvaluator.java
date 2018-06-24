package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.io.HpoDataIngestor;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Likelihood ratio evaluator. This class coordinates the performance of the likelihood ratio test on
 * an {@link HpoCase}.
 */
public class CaseEvaluator {
    private static final Logger logger = LogManager.getLogger();
    private final HpoCase hpocase;
    /** key: a disease CURIE, e.g., OMIM:600100; value-corresponding disease object.*/
    private final Map<TermId,HpoDisease> diseaseMap;
    /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
    private final Multimap<TermId,TermId> disease2geneMultimap;

    /** Results of the LR calculations. */
    private final Map<TermId,TestResult> disease2resultMap;
    /** Probability of diseases before testing (e.g., prevalence or 1/N).*/
    private final Map<TermId,Double> pretestProbabilityMap;
    /** Object used to calculate phenotype likelihood ratios. */
    private final PhenotypeLikelihoodRatio phenotypeLRevaluator;
    /** Object used to calculate genotype-based likelihood ratio. */
    private final GenotypeLikelihoodRatio genotypeLrEvalutator;
    /** Reference to the Human Phenotype Ontology object. */
    private final HpoOntology ontology;
    /** Map of the observed genotypes in the VCF file. Key is an EntrezGene is, and the value is the average pathogenicity score times the
     * count of all variants in the pathogenic bin.*/
    private final Map<TermId,Double> genotypeMap;



    private CaseEvaluator(List<TermId> hpoTerms,
                          HpoOntology ontology,
                          Map<TermId,HpoDisease> diseaseMap,
                          Multimap<TermId,TermId> disease2geneMultimap,
                          PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                          GenotypeLikelihoodRatio genotypeLrEvalutator,
                          Map<TermId,Double> genotypeMap) {
        ImmutableList.Builder<TermId> termIdBuilder = new ImmutableList.Builder<>();
        termIdBuilder.addAll(hpoTerms);
        ImmutableList<TermId> termlist = termIdBuilder.build();
        this.hpocase = (new HpoCase.Builder(termlist)).build();

        this.diseaseMap=diseaseMap;
        this.disease2geneMultimap=disease2geneMultimap;
        this.phenotypeLRevaluator =phenotypeLrEvaluator;
        this.genotypeLrEvalutator=genotypeLrEvalutator;
        this.ontology=ontology;
        this.disease2resultMap = new HashMap<>();
        // For now, assume equal pretest probabilities
        this.pretestProbabilityMap =new HashMap<>();
        int n=diseaseMap.size();
        double prob=1.0/(double)n;
        for (TermId tid : diseaseMap.keySet()) {
            pretestProbabilityMap.put(tid,prob);
        }
        this.genotypeMap=genotypeMap;

    }



    /** This method evaluates the likilihood ratio for each disease in
     * {@link #diseaseMap}. After this, it sorts the results (the best hit is then at index 0, etc).
     */
    public void evaluate()  {
        assert diseaseMap.size()== pretestProbabilityMap.size();
        for (TermId diseaseId : diseaseMap.keySet()) {
            HpoDisease disease = this.diseaseMap.get(diseaseId);
            double pretest = pretestProbabilityMap.get(diseaseId);
            // 1. get phenotype LR
            ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
            for (TermId tid : this.hpocase.getObservedAbnormalities()) {
                double LR = phenotypeLRevaluator.getLikelihoodRatio(tid, diseaseId);
                builder.add(LR);
            }
            // 2. get genotype LR if available
            Collection<TermId> associatedGenes = disease2geneMultimap.get(diseaseId);
            Double LR = null;
            TermId geneId = null;
            if (associatedGenes != null && associatedGenes.size() > 0) {
                for (TermId entrezGeneId : associatedGenes) {
                    double observedWeightedPathogenicVariantCount = this.genotypeMap.getOrDefault(entrezGeneId, 0.0);
                    List<TermId> inheritancemodes = disease.getModesOfInheritance();
                    Optional<Double> opt = this.genotypeLrEvalutator.evaluateGenotype(observedWeightedPathogenicVariantCount,
                            inheritancemodes,
                            entrezGeneId);
                    if (opt.isPresent()) {
                        if (LR == null) {
                            LR = opt.get();
                            geneId = entrezGeneId;
                        } else if (LR > opt.get()) {
                            LR = opt.get();
                            geneId = entrezGeneId;
                        }
                    }
                }
                TestResult result;
                if (LR != null) {
                    result = new TestResult(builder.build(), diseaseId, LR, geneId, pretest);
                } else {
                    result = new TestResult(builder.build(), diseaseId, pretest);
                }
                disease2resultMap.put(diseaseId, result);
            }
        }
        // The following will store the rank of each result in the result objects
        List<TestResult> resultlist = new ArrayList<>(disease2resultMap.values());
        resultlist.sort(Collections.reverseOrder());
        for (int i=0;i<resultlist.size();i++) {
            TestResult tr = resultlist.get(i);
            tr.setRank(i+1);
        }
    }


    public TestResult getResult(TermId diseaseId) {
        return disease2resultMap.get(diseaseId);
    }


    /**
     * @param diseaseId CURIE (e.g., OMIM:600100) of the disease whose rank we want to know
     * @return the rank of the disease within all of the test results
     */
    public int getRank(TermId diseaseId){
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        if (disease==null) {
            logger.error("null disease in getRank");
            return 0;
        }
        TestResult tr = this.disease2resultMap.get(diseaseId);
        if (tr==null) {
            // should never happen
            System.err.print("[ERROR] Attempt to get rank on non-tested disease " + disease);
            return Integer.MAX_VALUE;
        } else {
            outputLR(tr);
            return tr.getRank();
        }
    }

    /** Output the results for a specific HPO disease. */

    private void outputLR(TestResult r) {
        TermId diseaseId = r.getDiseaseCurie();
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        int rank = r.getRank();
        System.err.println("Likelihood ratios for " + disease.getName() + "[" + diseaseId +
                "]\tRank=" + rank);

        DecimalFormat df = new DecimalFormat("0.000E0");
        System.err.println(String.format("Pretest probability: %s; Composite LR: %.2f; Posttest probability: %s ",
                niceFormat(r.getPretestProbability()),
                r.getCompositeLR(),
                niceFormat(r.getPosttestProbability())));
        for (int i = 0; i < r.getNumberOfTests(); i++) {
            double ratio = r.getRatio(i);
            TermId tid = hpocase.getObservedAbnormalities().get(i);
            String term = String.format("%s [%s]", ontology.getTermMap().get(tid).getName(), tid.getIdWithPrefix());
            System.err.println(String.format("%s: ratio=%s", term, niceFormat(ratio)));
        }
        if (r.hasGenotype()) {
            System.err.println(String.format("Genotype LR for %s: %f", r.getEntrezGeneId(), r.getGenotypeLR()));
        }
        System.err.println();
    }


    public void writeSvg(TestResult result){
        Lr2Svg l2svg = new Lr2Svg(this.hpocase,result,this.ontology);
        String fname = String.format("%s.svg",result.getDiseaseCurie().getIdWithPrefix().replace(':','_') );
        l2svg.writeSvg(fname);
    }


    public HpoDisease id2disease(TermId diseaseCurie) {
         return this.diseaseMap.get(diseaseCurie);
    }

    public String i2diseaseName(TermId diseaseCurie) {
        return this.diseaseMap.get(diseaseCurie).getName();
    }


    private String niceFormat(double d) {
        DecimalFormat df = new DecimalFormat("0.000E0");
        if (d > 1.0) {
            return String.format("%.2f", d);
        } else if (d > 0.005) {
            return String.format("%.4f", d);
        } else {
            return df.format(d);
        }
    }

    /**
     * Convenience class for building a {@link CaseEvaluator} object--mainly to avoid having
     * a constructor with an extremely long list of arguments.
     */
    public static class Builder {
        /** The abnormalities observed in the individual being investigated. */
        private final List<TermId> hpoTerms;
        private HpoOntology ontology;
        /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
        private Map<TermId,HpoDisease> diseaseMap;
        /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
        private Multimap<TermId,TermId> disease2geneMultimap;
        /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
        private PhenotypeLikelihoodRatio phenotypeLR;

        private GenotypeLikelihoodRatio genotypeLR;

        private Map<TermId,Double> genotypeMap;

        public Builder(List<TermId> hpoTerms){ this.hpoTerms=hpoTerms; }

        public Builder ontology(HpoOntology hont) { this.ontology=hont; return this;}

        public Builder diseaseMap(Map<TermId,HpoDisease> dmap) { this.diseaseMap=dmap; return this;}

        public Builder disease2geneMultimap(Multimap<TermId,TermId> d2gmmap) { this.disease2geneMultimap=d2gmmap; return this;}

        public Builder genotypeMap(Map<TermId,Double> gtmap) { this.genotypeMap=gtmap; return this;}

        public Builder genotypeLr(GenotypeLikelihoodRatio glr) { this.genotypeLR=glr; return this; }


        public CaseEvaluator build() {
            Objects.requireNonNull(hpoTerms);
            Objects.requireNonNull(ontology);
            Objects.requireNonNull(diseaseMap);
            Objects.requireNonNull(disease2geneMultimap);
            return new CaseEvaluator(hpoTerms,ontology,diseaseMap,disease2geneMultimap,phenotypeLR,genotypeLR,genotypeMap);
        }



    }
    

}
