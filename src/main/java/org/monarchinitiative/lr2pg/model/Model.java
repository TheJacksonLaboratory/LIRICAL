package org.monarchinitiative.lr2pg.model;

import com.google.common.collect.Multimap;
import org.monarchinitiative.lr2pg.hpo.BackgroundForegroundTermFrequency;
import org.monarchinitiative.lr2pg.hpo.GenotypeCollection;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

/**
 * This is a convenience class designed to collect togehter all of the data we need to do the calculation.
 * In the final app, we will presumably replace this with Spring magic.
 */
public class Model {

    private final int variantCount;

    private final double meanVariantPathogenicity;

    private TermId diseaseCurie;
    /** List of HPO terms representing phenoytpic abnormalities seen in the patient. */
    private final List<TermId> hpoTerms;

    private HpoOntology ontology;
    private Map<TermId,HpoDisease> diseaseMap;
    /* key: disease CURIEs such as OMIM:600123; value: a collection of gene CURIEs such as NCBIGene:123.  */
    private Multimap<TermId,TermId> disease2geneMultimap;

    private BackgroundForegroundTermFrequency bftfrequency;

    private HpoCase currentCase;

    /** Key: the termId of a gene; double -- count of variants in pathogenic bin
     * multiplied by average pathogenicity score. */
    private Map<TermId,Double> genotypeMap;

    private TermId entrezGeneId;

    private Map<TermId,Double> gene2backgroundFrequency;




    public Model(int varCount, double meanVariantPath, TermId entrezGeneId, List<TermId> terms, HpoOntology ontology, Map<TermId,HpoDisease> diseaseMap, Multimap<TermId,TermId> disease2geneMultimap) {
        this.variantCount=varCount;
        this.meanVariantPathogenicity=meanVariantPath;
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.disease2geneMultimap=disease2geneMultimap;
        this.hpoTerms=terms;
        bftfrequency=new BackgroundForegroundTermFrequency(ontology,diseaseMap);
        this.entrezGeneId=entrezGeneId;

    }



    public TermId getEntrezGeneId() {
        return entrezGeneId;
    }
    public void setDiseaseCurie(TermId d) { this.diseaseCurie=d; }

    public void setHpoCase(HpoCase c) { this.currentCase=c;}

    public void setGenotypeCollection(GenotypeCollection gcoll) { this.genotypeMap=gcoll.getGenotypeMap(); }


    public int getVariantCount() {
        return variantCount;
    }

    public double getMeanVariantPathogenicity() {
        return meanVariantPathogenicity;
    }

    public TermId getDiseaseCurie() {
        return diseaseCurie;
    }

    public List<TermId> getHpoTerms() {
        return hpoTerms;
    }

    public HpoOntology getOntology() {
        return ontology;
    }

    public Map<TermId, HpoDisease> getDiseaseMap() {
        return diseaseMap;
    }

    public Multimap<TermId, TermId> getDisease2geneMultimap() {
        return disease2geneMultimap;
    }

    public BackgroundForegroundTermFrequency getBftfrequency() {
        return bftfrequency;
    }

    public HpoCase getCurrentCase() {
        return currentCase;
    }

    public void setBackgroundFrequency(Map<TermId,Double> g2bFrequency) { this.gene2backgroundFrequency=g2bFrequency;}
    public Map<TermId,Double> getBackgroundFrequency(){ return this.gene2backgroundFrequency;}

    public Map<TermId,Double> getGene2BackgroundFreq() { return this.genotypeMap; }

}
