package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;
import util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Creates a map from the {@code phenotype_annotation.tab} file that relates the
 * an HPO feature to the frequency of the HPO in a disease. This is also the central
 * class that is used to calculate the likelihood ratio. We also use this to calculate
 * the default frequency of terms if a disease is NOT annotated to the term. We will
 * assume that the frequency is non-zero for any combination of term and disease, because
 * the annotation files can have false negatives (i.e., our data is incomplete), or the
 * patient simply has an additional feature.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.1 (2017-11-16)
 */
public class Disease2TermFrequency {
    private static final Logger logger = LogManager.getLogger();
    /** Path to the {@code hp.obo} file. */
     private String hpoOboFilePath;
    /** Path to the {@code phenotype_annotation.tab} file. */
     private String hpoPhenotypeAnnotationPath;
    /** The subontology of the HPO with all the phenotypic abnormality terms. */
    private  Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology =null;
    /** The subontology of the HPO with all the inheritance terms. */
    private  Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology=null;
    /** This map has one entry for each disease in our database. */
    private  Map<String,HpoDiseaseWithMetadata> diseaseMap;

    private ImmutableMap<TermId, Double> hpoTerm2OverallFrequency = null;


    public Disease2TermFrequency(Ontology<HpoTerm, HpoTermRelation> pheno,
                                 Ontology<HpoTerm, HpoTermRelation> inheri,
                                 Map<String,HpoDiseaseWithMetadata> diseases) {
        phenotypeSubOntology=pheno;
        inheritanceSubontology=inheri;
        this.diseaseMap=diseases;
        initializeFrequencyMap();
    }

    /**
//     * @param hpoPath path to {@code hp.obo}
//     * @param hpoAnnotationPath path to {@code phenotype_annotation.tab}
//     */
//    public Disease2TermFrequency(String hpoPath, String hpoAnnotationPath){
//        hpoOboFilePath=hpoPath;
//        hpoPhenotypeAnnotationPath=hpoAnnotationPath;
//        try {
//            inputHpoData();
//        } catch (IOException e) {
//            logger.fatal(String.format("could not input data: %s",e.toString()));
//            System.exit(1);
//        }
//        initializeFrequencyMap();
//    }

    /**
     * Parse the {@code hp.obo} and the {@code phenotype_annotation.tab} files.
     * @throws IOException
     */
//    private void inputHpoData() throws IOException {
////        HpoOntologyParser parser = new HpoOntologyParser(this.hpoOboFilePath);
////        parser.parseOntology();
////        phenotypeSubOntology = parser.getPhenotypeSubontology();
////        inheritanceSubontology = parser.getInheritanceSubontology();
//        HpoAnnotation2DiseaseParser annParser =
//                new HpoAnnotation2DiseaseParser(this.hpoPhenotypeAnnotationPath,
//                        phenotypeSubOntology,
//                        inheritanceSubontology);
//        diseaseMap=annParser.getDiseaseMap();
//    }
    /** @return iterator over the diseases in the database. TODO just return the disease objects. */
    public Iterator<String> getDiseaseNameIterator() {
        return this.diseaseMap.keySet().iterator();
    }

    /**
     * Initialize the {@link #hpoTerm2OverallFrequency} object that has the background frequencies of each of the
     * HPO terms in the ontology.
     */
    private void initializeFrequencyMap() {
        Map<TermId, Double> mp = new HashMap<>();
        ImmutableMap.Builder<TermId, Double> imb = new ImmutableMap.Builder<>();
        for (HpoDiseaseWithMetadata dis: this.diseaseMap.values()) {
            for (TermIdWithMetadata tidm : dis.getPhenotypicAbnormalities()) {
                TermId tid=tidm.getTermId();
                if (! mp.containsKey(tid)) {
                    mp.put(tid,0.0);
                }
                Double cumulativeFreq = mp.get(tid) + tidm.getFrequency().upperBound();
                mp.put(tid,cumulativeFreq);
            }
        }
        // Now we need to normalize by the number of diseases.
        // We also divide by 100 since the HpoFrequency objects are returning a percentage and not
        // a probability
        double N = (double) getNumberOfDiseases() * 100;
        for (Map.Entry<TermId,Double> me : mp.entrySet()) {
            double f = me.getValue()/N;
            imb.put(me.getKey(),f);
        }
        hpoTerm2OverallFrequency=imb.build();
    }

    public double getFrequencyIfNotAnnotated(TermId tid) {
        double bf = getBackgroundFrequency(tid);
        return bf/10;
    }

    public int getNumberOfDiseases() {
        return diseaseMap.size();
    }

    public double getFrequencyOfTermInDisease(String diseaseName, TermId term) {
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);
        if (disease==null) {
            logger.fatal("Could not find disease %s in diseaseMap. Terminating...",diseaseName);
            System.exit(1);
        }
        TermIdWithMetadata timd = disease.getTermIdWithMetadata(term);
        if (timd==null) {
            // this disease does not have the Hpo term in question
            return getFrequencyIfNotAnnotated(term);
        } else {
            return timd.getFrequency().upperBound()/100.0;
        }
    }

    /**
     * This function estimates the probability of a test finding (the HP term is present) given that the
     * disease is not present -- we call this the background frequency.
     * @return the estimate background frequency (note: bf \in [0,1])
     */
    public double getBackgroundFrequency(TermId term) {
        if (! hpoTerm2OverallFrequency.containsKey(term)) {
            logger.fatal(String.format("Map did not contain data for term %s",term.getIdWithPrefix() ));
            System.exit(1);
        }
        return hpoTerm2OverallFrequency.get(term);
    }


    public Ontology<HpoTerm, HpoTermRelation> getPhenotypeSubOntology() {
        return phenotypeSubOntology;
    }

    public double getLikelihoodRatio(TermId tid, String diseaseName) {
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);
        if (disease==null) {
            logger.fatal("Could not find disease %s in diseaseMap. Terminating...",diseaseName);
            System.exit(1);
        }
        Pair<TermIdWithMetadata, Integer> pair = disease.getMICAandPathLength(tid,phenotypeSubOntology);
        if (pair==null) {
            // There was no ancestor ??
            return 1.0/(double)diseaseMap.size();
        }
        return pair.first.getFrequency().upperBound() /(100.0*pair.second); // see readme!
    }



}
