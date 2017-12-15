package org.monarchinitiative.lr2pg.hpo;


import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.*;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simulator that similates cases from the {@link HpoDiseaseWithMetadata} objects by choosing a subset of terms
 * and adding noise terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.1
 */
public class HpoCaseSimulator {
    private static final Logger logger = LogManager.getLogger();
    /** The subontology of the HPO with all the phenotypic abnormality terms. */
    private static Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology =null;
    /** The subontology of the HPO with all the inheritance terms. */
    private static Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology=null;

    Disease2TermFrequency d2termFreqMap;

    /** THe file name of the HPO ontology file. */
    private static final String HP_OBO="hp.obo";
    /** The file name of the HPO annotation file. */
    private static final String HP_PHENOTYPE_ANNOTATION="phenotype_annotation.tab";

    private static Map<String,HpoDiseaseWithMetadata> diseaseMap;

    private static TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");

    private static HpoFrequency defaultFrequency=null;

    private static final int DEFAULT_N_TERMS_PER_CASE=4;

    private int n_terms_per_case = DEFAULT_N_TERMS_PER_CASE;

    private static final int DEFAULT_N_RANDOM_TERMS=2;

    private int n_n_random_terms_per_case=DEFAULT_N_RANDOM_TERMS;

    private static final HpoFrequency[] FREQUENCYARRAY={
            HpoFrequency.ALWAYS_PRESENT,
            HpoFrequency.VERY_FREQUENT,
            HpoFrequency.FREQUENT,
            HpoFrequency.OCCASIONAL,
            HpoFrequency.VERY_RARE,
            HpoFrequency.EXCLUDED};

    private static final HpoOnset[] ONSETARRAY={
            HpoOnset.ANTENATAL_ONSET,
            HpoOnset.EMBRYONAL_ONSET,
            HpoOnset.FETAL_ONSET,
            HpoOnset.CONGENITAL_ONSET,
            HpoOnset.NEONATAL_ONSET,
            HpoOnset.INFANTILE_ONSET,
            HpoOnset.CHILDHOOD_ONSET,
            HpoOnset. JUVENILE_ONSET,
            HpoOnset.ADULT_ONSET,
            HpoOnset.YOUNG_ADULT_ONSET,
            HpoOnset.MIDDLE_AGE_ONSET,
            HpoOnset.LATE_ONSET};



    public HpoCaseSimulator(String datadir)  {
        try {
            inputHpoOntologyAndAnnotations(datadir);
        } catch (Exception e) {
            logger.trace("Could not input HPO data");
            e.printStackTrace();
            System.exit(1); // todo exception
        }
    }


    public void simulateCases() {
        int c=0;
        for (String diseasename : diseaseMap.keySet()) {
            simulateCase(diseasename);
            if (c++>200) break; // for testing just simulate one disease
        }
    }


    private HpoFrequency getRandomFrequency() {
        int n=FREQUENCYARRAY.length;
        int r=(int)Math.floor(Math.random()*n);
        return FREQUENCYARRAY[r];
    }

    private HpoOnset getRandomOnset() {
        int n=ONSETARRAY.length;
        int r=(int)Math.floor(Math.random()*n);
        return ONSETARRAY[r];
    }


    private TermIdWithMetadata getRandomTerm() {
        int n = phenotypeSubOntology.countAllTerms();
        int r = (int)Math.floor(n*Math.random());
        TermId tid = (TermId)phenotypeSubOntology.getAllTermIds().toArray()[r];
        HpoFrequency randomFrequency=getRandomFrequency();
        HpoOnset randomOnset=getRandomOnset();
       // logger.trace(String.format("get random term freq=%s and onset=%s",randomFrequency.toTermId().getIdWithPrefix(),randomOnset.toString()));
        TermIdWithMetadata tidm = new ImmutableTermIdWithMetadata(tid, randomFrequency, randomOnset);
        return tidm;
    }

    private Set<TermIdWithMetadata> getNTerms( int desiredsize,List<TermIdWithMetadata> abnormalities) {
        Set<TermIdWithMetadata> rand=new HashSet<>();
        int maxindex = abnormalities.size();
        do {
            int r = (int)Math.floor(maxindex*Math.random());
            TermIdWithMetadata t=abnormalities.get(r);
            rand.add(t);
        } while (rand.size()<desiredsize);
        return rand;
    }


    public void simulateCase(String diseasename) {
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseasename);
        if (disease==null) {
            logger.error("Should never happen -- could not retrieve disease for " + diseasename);
            return;
        }

        int n_terms=Math.min(disease.getNumberOfPhenotypeAnnotations(),n_terms_per_case);
        int n_random=Math.min(n_terms,n_n_random_terms_per_case);
//        logger.trace(String.format("Performing simulation on %s with %d randomly chosen terms and %d noise terms",disease.getName(),
//                n_terms,n_random));

        List<TermIdWithMetadata> abnormalities = disease.getPhenotypicAbnormalities();

        ImmutableList.Builder<TermIdWithMetadata> builder = new ImmutableList.Builder<>();
        builder.addAll(getNTerms(n_terms,abnormalities));

        for (int i=0;i<n_terms;i++) {
            TermIdWithMetadata t = getRandomTerm();
            builder.add(t);
        }
        ImmutableList<TermIdWithMetadata> termlist = builder.build();
//        for (TermIdWithMetadata t : termlist) {
//            System.out.println(t.toString());
//        }

        HpoCase hpocase = new HpoCase(phenotypeSubOntology,d2termFreqMap,diseasename,termlist);
        hpocase.calculateLikelihoodRatios();
        System.out.println(String.format("Rank of %s was %d/%d",diseasename,hpocase.getRank(diseasename),hpocase.getTotalResultCount()));
    }









    public void debugPrint() {
        logger.trace(String.format("Got %d terms and %d diseases",phenotypeSubOntology.getAllTermIds().size(),
                diseaseMap.size()));
    }




    private void inputHpoOntologyAndAnnotations(String datadir) throws Exception {
        String hpopath=String.format("%s%s%s",datadir, File.separator,HP_OBO);
        String annotationpath=String.format("%s%s%s",datadir,File.separator,HP_PHENOTYPE_ANNOTATION);
        HpoOntologyParser parser = new HpoOntologyParser(hpopath);
        parser.parseOntology();
        phenotypeSubOntology = parser.getPhenotypeSubontology();
        inheritanceSubontology = parser.getInheritanceSubontology();
        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationpath,phenotypeSubOntology,inheritanceSubontology);
        diseaseMap=annotationParser.getDiseaseMap();
        String DEFAULT_FREQUENCY="0040280";
        final TermId DEFAULT_FREQUENCY_ID = new ImmutableTermId(HP_PREFIX,DEFAULT_FREQUENCY);
        defaultFrequency=HpoFrequency.fromTermId(DEFAULT_FREQUENCY_ID);
        this.d2termFreqMap = new Disease2TermFrequency(hpopath,annotationpath); //todo pass in the other objects
    }
}
