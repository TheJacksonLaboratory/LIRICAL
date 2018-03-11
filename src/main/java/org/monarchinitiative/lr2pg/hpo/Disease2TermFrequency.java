package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseWithMetadata;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.formats.hpo.TermIdWithMetadata;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;
import util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.github.phenomics.ontolib.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * Creates a map from the {@code phenotype_annotation.tab} file that relates the
 * an HPO feature to the frequency of the HPO in a disease. This is also the central
 * class that is used to calculate the likelihood ratio. We also use this to calculate
 * the default frequency of terms if a disease is NOT annotated to the term. We will
 * assume that the frequency is non-zero for any combination of term and disease, because
 * the annotation files can have false negatives (i.e., our data is incomplete), or the
 * patient simply has an additional feature.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.2 (2017-11-16)
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
        initializeFrequencyMapOLD();
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
     * todo refactor
     */
    private void initializeFrequencyMapOLD() {
        Map<TermId, Double> mp = new HashMap<>();
        for (TermId tid : this.phenotypeSubOntology.getTermMap().keySet()) {
            mp.put(tid,0.0D);
        }
        ImmutableMap.Builder<TermId, Double> imb = new ImmutableMap.Builder<>();
        for (HpoDiseaseWithMetadata dis: this.diseaseMap.values()) {
            for (TermIdWithMetadata tidm : dis.getPhenotypicAbnormalities()) {
                TermId tid=tidm.getTermId();
               // tid = phenotypeSubOntology.getTermMap().get(tid).getId(); //replace with current id if needed
                if (! mp.containsKey(tid)) {
                    mp.put(tid,0.0);
                }
                double delta = tidm.getFrequency().upperBound();
                Set<TermId> ancs =this.phenotypeSubOntology.getAncestorTermIds(tid);
                for (TermId at : ancs) {
                    if (! mp.containsKey(at)) mp.put(at,0.0);
                    double cumulativeFreq = mp.get(at) + delta;
                    mp.put(at, cumulativeFreq);
                }
            }
        }
        // Now we need to normalize by the number of diseases.
        double N = (double) getNumberOfDiseases();
        for (Map.Entry<TermId,Double> me : mp.entrySet()) {
            double f = me.getValue()/N;
            imb.put(me.getKey(),f);
        }
        hpoTerm2OverallFrequency=imb.build();
        logger.trace("Got data on background frequency for " + hpoTerm2OverallFrequency.size() + " terms");
    }

//    /*If the term is in the same subhierarchy, the frequency of a term is calculated based on the depth of the term and the level (distance) that it has from one of the
//    HPO terms of the disease. The larger distance, the smaller frequency. The frequency decreases by a factor of (1/log(level)).
//    If the term is not in the same subhierarchy, the frequency of term is calculated using backgroundfrequency/10.
//    */
//    public double getFrequencyIfNotAnnotatedOLD(TermId tid, String diseaseName) {
//        // double bf = getBackgroundFrequency(tid);
//        // return bf/10;
//        Set<TermId> ancs =this.phenotypeSubOntology.getAncestorTermIds(tid);
//        int level = 0;
//        double prob = 0;
//        do{
//            level ++;
//            //Check if the term is a root,(a term is a root if its ancestors is empty)
//            for (TermId at:ancs) {
//
//                Set<TermId> ances = this.phenotypeSubOntology.getAncestorTermIds(at);
//                if(ances.isEmpty()){
//                    continue;
//                }
//               // phenotypeSubOntology.isRootTerm(at)
//                HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);
//                if (disease==null) {
//                    logger.fatal("Could not find disease %s in diseaseMap. Terminating...",diseaseName);
//                    System.exit(1);
//                }
//                TermIdWithMetadata timd = disease.getTermIdWithMetadata(at);
//                if (timd !=null){ //Disease have the HPO term
//                    prob = timd.getFrequency().upperBound() / ((100)*(1 + Math.log(level))); //penalty for imprecision,
//                    break;
//                }
//            }
//        }
//        while(!ancs.isEmpty());
//        if (prob != 0)
//            return prob;
//        else //if the HPO term is not in this subhierarchy, then we assign a value for prob based on the background freq of the term.
//            return getBackgroundFrequency(tid)/10;
//    }


    private final static double DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY = 0.000_005; // 1:20,000

    private double getFrequencyIfNotAnnotated(TermId tid, String diseaseName) {
        if (phenotypeSubOntology.getTermMap().get(tid)==null) {
            logger.error("Could not get term for "+tid.getIdWithPrefix());
            logger.error("phenotypeSubOntology size "+phenotypeSubOntology.getTermMap().size());
            //System.exit(1);
            return 0.001;
        }
        tid = phenotypeSubOntology.getTermMap().get(tid).getId();
        int level = 0;
        double prob = 0;
        boolean foundannotation=false;
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);
       Set<TermId> currentlevel=new HashSet<>();
        currentlevel.add(tid);
        while (! currentlevel.isEmpty()) {
            level++;
            Set<TermId> parents =  getParentTerms(phenotypeSubOntology,currentlevel);
            for (TermId id : parents) {
                if (phenotypeSubOntology.isRootTerm(id)) {
                    break;
                }
                TermIdWithMetadata timd = disease.getTermIdWithMetadata(id);
                if (timd != null) {
                    prob += timd.getFrequency().upperBound() / (1 + Math.log(level)); //penalty for imprecision,
                    foundannotation=true;
                }
            }
            if (foundannotation) {
                return prob;
            } else {
                currentlevel=parents;
            }
        }
        // if we get here, then we are at the root.
        // this means that the disease has no annotations in the same section (e.g., cardiology) of the HPO
        // Therefore, the current term is not at all typical for the disease and is probably just a false positive
        // (there is a smaller chance that our annotations are incomplete)
        return DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;

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
            return getFrequencyIfNotAnnotated(term,diseaseName);
        } else {
            return timd.getFrequency().upperBound();
        }
    }

    /**
     * This function estimates the probability of a test finding (the HP term is present) given that the
     * disease is not present -- we call this the background frequency.
     * @return the estimate background frequency (note: bf \in [0,1])
     */
    double getBackgroundFrequency(TermId termId) {
        if (termId instanceof TermIdWithMetadata) {
           termId= ((TermIdWithMetadata) termId).getTermId();
        }
        if (! hpoTerm2OverallFrequency.containsKey(termId)) {
            logger.fatal(String.format("Map did not contain data for term %s",termId.getIdWithPrefix() ));
            // todo throw error
            System.exit(1);
        }
        return hpoTerm2OverallFrequency.get(termId);
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
        double numerator=getFrequencyOfTermInDisease(diseaseName,tid);
        double denominator=getBackgroundFrequency(tid);
               return numerator/denominator;

    }



}
