package org.monarchinitiative.lr2pg.hpo;



import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.formats.hpo.*;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;

/**
 * A simulator that simulates cases from the {@link HpoDisease} objects by choosing a subset of terms
 * and adding noise terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2
 */
public class HpoCaseSimulator {
    private static final Logger logger = LogManager.getLogger();
    /** The subontology of the HPO with all the phenotypic abnormality terms. */
    private HpoOntology ontology =null;
    /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
    private BackgroundForegroundTermFrequency bftfrequency;
    /** A list of all HPO term ids in the Phenotypic abnormality subontology. */
    private final ImmutableList<TermId> phenotypeterms;
    /** The file name of the HPO ontology file. */
    private static final String HP_OBO="hp.obo";
    /** The file name of the HPO annotation file. */
    private static final String HP_PHENOTYPE_ANNOTATION="phenotype.hpoa";
    /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
    private static Map<String,HpoDisease> diseaseMap;
    /** todo put this in the command line interface. */
    private static final int DEFAULT_N_TERMS_PER_CASE=4;

    private int n_terms_per_case = DEFAULT_N_TERMS_PER_CASE;
    /** Number of random "noise" terms that get added to each simulated case. */
    private static final int DEFAULT_N_RANDOM_TERMS=1;

    private int n_random_terms_per_case =DEFAULT_N_RANDOM_TERMS;
    /** Root term id in the phenotypic abnormality subontology. */
    private final static TermId PHENOTYPIC_ABNORMALITY = ImmutableTermId.constructWithPrefix("HP:0000118");

    private static int n_cases_to_simulate;

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
            HpoOnset.JUVENILE_ONSET,
            HpoOnset.ADULT_ONSET,
            HpoOnset.YOUNG_ADULT_ONSET,
            HpoOnset.MIDDLE_AGE_ONSET,
            HpoOnset.LATE_ONSET};


    /**
     * The constructor initializes {@link #ontology} and {@link #diseaseMap} and {@link #phenotypeterms}.
     * @param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    public HpoCaseSimulator(String datadir) throws Lr2pgException {
        try {
            inputHpoOntologyAndAnnotations(datadir);
        } catch (Exception e) {
            throw new Lr2pgException(e.getMessage());
        }
        Set<TermId> descendents=getDescendents(ontology,PHENOTYPIC_ABNORMALITY);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (TermId t: descendents) {
            builder.add(t);
        }
        this.phenotypeterms=builder.build();
    }


    public void simulateCases() {
        int c=0;
        int LIMIT=1000;
        Map<Integer,Integer> ranks=new HashMap<>();
        logger.trace(String.format("Will simulate %d diseases.",diseaseMap.size() ));
        for (String diseasename : diseaseMap.keySet()) {
            HpoDisease disease = diseaseMap.get(diseasename);
            logger.trace("Simulating disease "+diseasename);
            if (disease.getNumberOfPhenotypeAnnotations() == 0) {
                logger.trace(String.format("Skipping disease %s because it has no phenotypic annotations", disease.getName()));
                continue;
            }
            int rank = simulateCase(diseasename);
            if (!ranks.containsKey(rank)) {
                ranks.put(rank, 0);
            }
            ranks.put(rank, ranks.get(rank) + 1);
            if (++c>LIMIT)break;
        }
        int N=LIMIT;
        int rank11_20=0;
        int rank21_30=0;
        int rank31_100=0;
        int rank101_up=0;
        for (int r:ranks.keySet()) {
            if (r<11) {
                System.out.println(String.format("Rank=%d: count:%d (%.1f%%)", r, ranks.get(r), (double) 100.0 * ranks.get(r) / N));
            } else if (r<21) {
                rank11_20+=ranks.get(r);
            } else if (r<31) {
                rank21_30+=ranks.get(r);
            } else if (r<101) {
                rank31_100+=ranks.get(r);
            } else {
                rank101_up+=ranks.get(r);
            }
        }
        System.out.println(String.format("Rank=11-20: count:%d (%.1f%%)", rank11_20, (double) 100.0 * rank11_20 / N));
        System.out.println(String.format("Rank=21-30: count:%d (%.1f%%)", rank21_30, (double) 100.0 * rank21_30 / N));
        System.out.println(String.format("Rank=31-100: count:%d (%.1f%%)", rank31_100, (double) 100.0 * rank31_100 / N));
        System.out.println(String.format("Rank=101-...: count:%d (%.1f%%)", rank101_up, (double) 100.0 * rank101_up / N));
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

    /**
     * @return a random term from the phenotype subontology.
     */
    private HpoTermId getRandomPhenotypeTerm() {
        int n=phenotypeterms.size();
        int r = (int)Math.floor(n*Math.random());
        TermId tid = phenotypeterms.get(r);
        HpoFrequency randomFrequency=getRandomFrequency();
        HpoOnset randomOnset=getRandomOnset();
        return new ImmutableHpoTermId.Builder(tid).frequency(randomFrequency.mean()).onset(randomOnset).build();
    }

    private Set<HpoTermId> getNTerms( int desiredsize,List<HpoTermId> abnormalities)  {
        Set<HpoTermId> rand=new HashSet<>();
        if (abnormalities.size()==0) return rand; // should never happen
        if (abnormalities.size()==1) return new HashSet<>(abnormalities);
        int maxindex = abnormalities.size()-1;
        int nTerms=Math.min(maxindex,desiredsize);
        // get maxindex distinct random integers that will be our random index values.
        int[] rdmidx =  ThreadLocalRandom.current().ints(0, maxindex).distinct().limit(nTerms).toArray();
        Arrays.stream(rdmidx).forEach( i -> rand.add(abnormalities.get(i)));
        return rand;
    }


    private int simulateCase(String diseasename) {
        HpoDisease disease = diseaseMap.get(diseasename);
        if (disease==null) {
            logger.error("Should never happen -- could not retrieve disease for " + diseasename);
            return -1;
        }

        int n_terms=Math.min(disease.getNumberOfPhenotypeAnnotations(),n_terms_per_case);
        int n_random=Math.min(n_terms, n_random_terms_per_case);
        //logger.trace(String.format("Performing simulation on %s with %d randomly chosen terms and %d noise terms",disease.getName(), n_terms,n_random));
        List<HpoTermId> abnormalities = disease.getPhenotypicAbnormalities();
        ImmutableList.Builder<HpoTermId> builder = new ImmutableList.Builder<>();
        try {
            builder.addAll(getNTerms(n_terms, abnormalities));
        } catch (Exception e) {
            logger.error("exception with diseases " + diseasename);
            logger.error(disease.toString());
            logger.error(e.toString());
        }

       /* int maxSize = phenotypeSubOntology.countAllTerms();
         int [] rndNumbers = ThreadLocalRandom.current().ints(0, maxSize).distinct().limit(n_random).toArray();
            for (int i=0;i<rndNumbers.length;++i){
                TermId tid = (TermId) phenotypeSubOntology.getAllTermIds().toArray()[rndNumbers[i]];
                HpoFrequency randomFrequency=getRandomFrequency();
                HpoOnset randomOnset=getRandomOnset();
                // logger.trace(String.format("get random term freq=%s and onset=%s",randomFrequency.toTermId().getIdWithPrefix(),randomOnset.toString()));
                TermIdWithMetadata tidm = new ImmutableTermIdWithMetadata(tid, randomFrequency, randomOnset);
                builder.add( tidm);
            }*/
          for(int i=0;i<n_random;i++){
              HpoTermId t = getRandomPhenotypeTerm();
                 builder.add(t);
          }
        ImmutableList<HpoTermId> termlist = builder.build();
//        for (TermIdWithMetadata t : termlist) {
//            System.out.println(t.toString());
//        }

        HpoCase hpocase = new HpoCase(ontology,bftfrequency,diseasename,termlist);
        try {
            hpocase.calculateLikelihoodRatios();
        } catch (Lr2pgException e) {
            e.printStackTrace();
            return 0;
        }
        int rank=hpocase.getRank(diseasename);
        System.out.println(String.format("Rank of %s was %d/%d",diseasename,rank,hpocase.getTotalResultCount()));
        return rank;
    }




    public void debugPrint() {
        logger.trace(String.format("Got %d terms and %d diseases",ontology.getAllTermIds().size(),
                diseaseMap.size()));
    }


    private void inputHpoOntologyAndAnnotations(String datadir) throws Exception {
        String hpopath=String.format("%s%s%s",datadir, File.separator,HP_OBO);
        String annotationpath=String.format("%s%s%s",datadir,File.separator,HP_PHENOTYPE_ANNOTATION);
        HpoOboParser parser = new HpoOboParser(new File(hpopath));
        this.ontology= parser.parse();
        HpoDiseaseAnnotationParser annotationParser=new HpoDiseaseAnnotationParser(annotationpath,ontology);
        diseaseMap=annotationParser.parse();
        bftfrequency=new BackgroundForegroundTermFrequency(ontology,diseaseMap);
    }
}
