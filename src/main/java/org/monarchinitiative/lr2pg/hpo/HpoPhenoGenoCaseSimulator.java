package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.util.List;
import java.util.Map;

/**
 * This class is intended to try out some architectures for the genotype-phenotype
 * LR test. Much of the code is copied from {@link PhenotypeOnlyHpoCaseSimulator}
 */
public class HpoPhenoGenoCaseSimulator {
    private static final Logger logger = LogManager.getLogger();
    /** Object to evaluate the results of differential diagnosis by LR analysis. */
    private final CaseEvaluator evaluator;
    private TermPrefix NCBI_GENE_PREFIX=new TermPrefix("NCBIGene");
    /** This simulation assumes there is a pathogenic mutation in one gene (this one). The EntrezId is initialized in the
     * constructor.
     */
    private final TermId entrezGeneId;

    private HpoCase hpocase;

    /**
     * @param ontology reference to the {@link HpoOntology} object
     * @param diseaseMap key: a disease CURIE (e.g., OMIM:600100); value-corresponding disease object
     * @param disease2geneMultimap key: id of a disease; value: COllection of EntrezGene ids that correspond to the disease (zero, one, or many, but not null)
     * @param entrezGeneNumber String representing the EntrezGene id, e.g., "2200" for NCBIGene:2200.
     * @param varcount Count of called pathogenic variants in this gene
     * @param varpath Mean pathgenicity score of all pathogenic variants in this gene
     * @param hpoTerms List of phenotypic abnormalities seen in this disease
     * @param gene2backgroundFrequency Background (population) frequencies of called pathogenic variants in genes.
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

        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        GenotypeLikelihoodRatio genoLr = new GenotypeLikelihoodRatio(gene2backgroundFrequency);

        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoTerms)
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypes.getGenotypeMap())
                .phenotypeLr(phenoLr)
                .genotypeLr(genoLr);

        this.evaluator = caseBuilder.build();


    }




    public HpoCase evaluateCase() {
        this.hpocase = evaluator.evaluate();
        return hpocase;
    }


    public void outputSvg(TermId diseaseCurie,String diseaseName,HpoOntology ontology, Map<TermId,String> geneId2SymbolMap) {
        hpocase.outputLrToShell(diseaseCurie,ontology);
        TermId geneId = this.hpocase.getResult(diseaseCurie).getEntrezGeneId();
        String symbol;
        if (geneId==null) {
            symbol="n/a";
        } else {
            symbol = geneId2SymbolMap.get(geneId);
            symbol = String.format("%s [%s]", symbol, geneId.getIdWithPrefix());
        }
        Lr2Svg lr2svg = new Lr2Svg(hpocase,diseaseCurie,diseaseName,ontology, symbol);
        // Output file
        String fname = diseaseCurie.getIdWithPrefix().replace(":","_") + ".svg";
        lr2svg.writeSvg(fname);
    }


}
