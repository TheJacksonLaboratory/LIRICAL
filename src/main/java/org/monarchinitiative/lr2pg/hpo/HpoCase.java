package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import com.google.common.collect.ImmutableList;
import com.sun.org.apache.bcel.internal.generic.IMUL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.monarchinitiative.lr2pg.likelihoodratio.LikelihoodRatio.HpoFrequencies2LR;

/**
 * Represents a single case and the HPO terms assigned to the case (patient), as well as the results of the
 * likelihood ratio analysis.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.1 (2017-11-24)
 */
public class HpoCase {
    private static final Logger logger = LogManager.getLogger();

    private static TermPrefix HP_PREFIX = new ImmutableTermPrefix("HP");
    private Disease2TermFrequency disease2TermFrequencyMap=null;

    private String disease=null;
    /** List of Hpo terms for our case. TODO add negative annotations. */
    private List<TermId> hpoTerms;

    private Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;

    private List<TestResult> results;



    public HpoCase(String hpoPath, String annotationPath, String caseData) {
        this.disease2TermFrequencyMap= new Disease2TermFrequency(hpoPath,annotationPath);
        this.hpoOntology=disease2TermFrequencyMap.getPhenotypeSubOntology();
        hpoTerms = new ArrayList<>();
        parseCasedata(caseData);
        results=new ArrayList<>();
    }

    public void calculateLikelihoodRatios() {
        Iterator<String> it = disease2TermFrequencyMap.getDiseaseNameIterator();
        while (it.hasNext()) {
            String diseasename = it.next();
            ImmutableList.Builder builder = new ImmutableList.Builder();
            for (TermId tid : this.hpoTerms) {
                double f = disease2TermFrequencyMap.getFrequencyOfTermInDisease(diseasename,tid);
                double backgroundf = disease2TermFrequencyMap.getBackgroundFrequency(tid);
                double LR=HpoFrequencies2LR(f,backgroundf);
                builder.add(LR);
            }
            TestResult result = new TestResult(builder,diseasename);
            results.add(result);
        }
    }


    public void outputResults() {
        Collections.sort(results);
        for (TestResult r: results){
            System.out.println(r.toString());
        }
    }

    /**
     * In this prototype stage, we expect the input file to be like this
     * <pre>
     *  OMIM:108500
     * HP:0006855
     * HP:0000651
     * HP:0010545
     * HP:0001260
     * HP:0001332
     * </pre>
     * @param path
     */
    private void parseCasedata(String path) {
         Map<TermId,HpoTerm> mp= hpoOntology.getTermMap();
         Map<String,TermId> string2termMap=new HashMap<>();
         for (TermId tid : mp.keySet()) {
             string2termMap.put(tid.getIdWithPrefix(),tid);
         }
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line=br.readLine())!= null) {
                //System.out.println(line);
                if (line.startsWith("OMIM")) {
                    this.disease=line.trim();
                } else if (line.startsWith("HP")) {
                    if (string2termMap.containsKey(line.trim())) {
                        hpoTerms.add(string2termMap.get(line.trim()));
                    } else {
                        logger.error(String.format("Could not identify term \"%s\" ",line.trim()));
                        continue;
                    }
                }
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int getNumberOfAnnotations() {
        return hpoTerms.size();
    }



}
