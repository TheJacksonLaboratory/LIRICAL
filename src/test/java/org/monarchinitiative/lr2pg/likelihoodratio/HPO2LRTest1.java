package org.monarchinitiative.lr2pg.likelihoodratio;

/**
 * Created by ravanv on 9/25/17.
 */

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.hpo.HPOParser;
import org.monarchinitiative.lr2pg.prototype.Disease;
import org.monarchinitiative.lr2pg.prototype.MapUtil;
import com.github.phenomics.ontolib.ontology.data.*;
import java.io.*;
import java.util.*;
import org.junit.*;



public class HPO2LRTest1 {

   // private String annotpath = "/Users/ravanv/Documents/HPO_LR1/LR2PG/HPO/phenotype_annotation.tab";
   private String annotpath = "/Users/ravanv/Documents/HPO_LRTest/LR2PG/HPO/phenotype_annotation.tab";
    private String hpopath = "/Users/ravanv/Documents/HPO_LRTest/LR2PG/HPO/hp.obo";
    private String fileName = "/Users/ravanv/Documents/HPO_LRTest/LR2PG/HPOTerms.txt";
    private Ontology<HpoTerm, HpoTermRelation> ontology = null;
    /**
     * Map of all of the Phenotypic abnormality terms (i.e., not the inheritance terms).
     */
    private Map<TermId, HpoTerm> termmap = null;
    Ontology<HpoTerm, HpoTermRelation> inheritance = null;

    /**
     * List of all annotations parsed from phenotype_annotation.tab.
     */
    private List<HpoDiseaseAnnotation> annotList = null;
    private Map<String, Disease> diseaseMap = null;
    Map<TermId, Integer> hpoTerm2DiseaseCount = null;
    //Map to store likelihood ratio for each disease
    Map<String,Double> Disease2LR = new HashMap<>();
    //List to store list of HPO terms of a patient
    List<String> HPOTermsPatient = new ArrayList<>();
    //List to store TermIds of HPO terms of a patient
    List<TermId> ListOfTermIdsOfHPOTerms = new ArrayList<>();

    /**
     * List of likelihood ratios, pretest odds, posttest odds and posttest prob. for each disease
     */
    List<Double> LikelihoodRatios = new ArrayList<Double>();
    List<Double> PretestOdds = new ArrayList<Double>();
    List<Double> PostTestOdds = new ArrayList<Double>();
    List<Double> PostTestProb = new ArrayList<Double>();
    /**
     * sign of the Likelihood ratio, 'P' for positive, 'N' for negative
     */
    char TestSign = 'P';

    double PretestProb = 0.5;

    private void parseHPOData(String hpopath, String annotpath) {
        HPOParser parser = new HPOParser();
        this.ontology = parser.parseOntology(hpopath);
        parser.parseAnnotation(annotpath);
        this.annotList =parser.getAnnotList();
                this.inheritance = parser.getInheritanceSubontology();
        this.termmap = parser.extractStrictPhenotypeTermMap();
    }

    private void debugPrintOntology() {
        TermId rootID = ontology.getRootTermId();
        Collection<HpoTerm> termlist = ontology.getTerms();
        Map<TermId, HpoTerm> termmap = new HashMap<>();
        for (HpoTerm term : termlist) {
            termmap.put(term.getId(), term);
        }
        Term root = termmap.get(rootID);
    }
    public void debugPrintDiseaseMap() {
        for (String d: diseaseMap.keySet()) {
            Disease disease = diseaseMap.get(d);
            System.err.println(String.format("Disease: %s: HPO ids: %d",disease.getName(),disease.getHpoIds().size()));
        }


    }

    private void createDiseaseModels() {
        diseaseMap = new HashMap<>();
        for (HpoDiseaseAnnotation annot : annotList) {
            String database = annot.getDb(); /* e.g., OMIM, ORPHA, DECIPHER */
            String diseaseName = annot.getDbName(); /* e.g., Marfan syndrome */
            String diseaseId = annot.getDbObjectId(); /* e.g., OMIM:100543 */
            TermId hpoId = annot.getHpoId();
             /*Filter database to just get OMIM */
            Disease disease = null;
            if (diseaseMap.containsKey(diseaseId)) {
                disease = diseaseMap.get(diseaseId);
            } else {
                disease = new Disease(database, diseaseName, diseaseId); //String database, String name,String id
                diseaseMap.put(diseaseId, disease);
            }
            if (this.termmap.containsKey(hpoId)) { // restrict to clinical terms, i.e., not inheritance.
                disease.addHpo(hpoId);
            }

        }

    }


    /**
     * This function reads a disease name and the HPO terms from a file and stores HPO terms in a list
     * The disease and its corresponding HPO terms are obtained form Phenomizer
     * @return List of HPO terms of a patient
     */

    public void getHPOTermsfromFile() {
        String line = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //The first line of the file is the name of disease. In this case, the disease is chosen from OMIM database
            line = bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                HPOTermsPatient.add(line);
            }

            bufferedReader.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + fileName + "'");
        } catch (IOException ex) {
            System.out.println("Error reading file '" + fileName + "'");
        }

    }


    /**
     * This function stores HPO IDs (first read as HPO term from a file) in the format of TermIds.
     * @return List of TermIds of HPO terms.
     */

    public void getHPOIdFile() {
        TermPrefix pref = new ImmutableTermPrefix("HP");
        getHPOTermsfromFile();
        for (int i = 0; i < HPOTermsPatient.size(); ++i) {
            String HPid = HPOTermsPatient.get(i);
            if (HPid.startsWith("HP:")) {
                HPid = HPid.substring(3);
                TermId tid = new ImmutableTermId(pref, HPid);
                ListOfTermIdsOfHPOTerms.add(tid);
            }
        }

    }

    /**
     * This function counts the number of diseases that are annotated to each HPO term, including
     * implicited (inherited) annotations, and places the result in {@link #hpoTerm2DiseaseCount}.
     * TODO convert into Java8 stream
     */
    /*private void initializeTerm2DiseaseMap() {
        hpoTerm2DiseaseCount = new HashMap<>();
        int good = 0, bad = 0;
        for (Disease disease : diseaseMap.values()) {
            System.err.println(String.format("Disease %s", disease.getName()));
            for (TermId termId : disease.getHpoIds()) {
                System.err.println(String.format("Term %s Status %s",
                        termId.getIdWithPrefix(), ontology.getAllTermIds().contains(termId)));
            }
            try {
                Collection<TermId> ids = disease.getHpoIds();
                if (ids == null) {
                    System.err.println("TermIds NULL");
                    System.exit(1);
                } else if (ids.size() == 0) {
                    System.err.println("TermIds zero size");
                    System.exit(1);
                }
                ids.remove(null);
                Set<TermId> ancestors = ontology.getAllAncestorTermIds(ids);
                ancestors.remove(null);
                for (TermId hpoid : ancestors) {
                    if (hpoid == null) continue;
                    if (!hpoTerm2DiseaseCount.containsKey(hpoid)) {
                        hpoTerm2DiseaseCount.put(hpoid, 1);
                    } else {
                        hpoTerm2DiseaseCount.put(hpoid, 1 + hpoTerm2DiseaseCount.get(hpoid));
                    }
                }
                good++;
            } catch (Exception e) {
                Collection<TermId> ids = disease.getHpoIds();

                bad++;
                System.exit(1);
            }

        }
    }*/

    private void initializeTerm2DiseaseMap() throws Exception {
        hpoTerm2DiseaseCount = new HashMap<>();
        int good=0,bad=0;
        for(Disease disease: diseaseMap.values()){
            System.err.println(String.format("Disease %s", disease.getName()));
            for (TermId termId : disease.getHpoIds()) {
                System.err.println(String.format("Term %s Status %s",
                        termId.getIdWithPrefix(), ontology.getAllTermIds().contains(termId)));
            }

            Collection<TermId> ids = disease.getHpoIds();
            if (ids==null) {
                String msg="TermIds NULL";
                throw new Exception(msg);
            } else if (ids.size()==0) {
                System.err.println("TermIds zero size");
                System.err.println("disease: " + disease.getName());
                debugPrintDiseaseMap();
                //System.exit(17);
                String msg = String.format("Disease %s had zero HpoIds",disease.getName());
                throw new Exception(msg);
            }
            ids.remove(null);
            Set<TermId> ancestors = ontology.getAllAncestorTermIds(ids);
            ancestors.remove(null);
            for (TermId hpoid : ancestors) {
                if (hpoid==null) continue;
                if (!hpoTerm2DiseaseCount.containsKey(hpoid)) {
                    hpoTerm2DiseaseCount.put(hpoid, 1);
                } else {
                    hpoTerm2DiseaseCount.put(hpoid, 1 + hpoTerm2DiseaseCount.get(hpoid));
                }
            }
            good++;


        }


    }


    /**
     * Returns the frequency of an HPO annotation among all diseases of our corpus, i.e., in {@link #diseaseMap}.
     * @param hpoId The HPO Term whose frequency we want to know
     * @return frequency of hpoId among all diseases
     */
    private double getBackgroundFrequency(TermId hpoId) {
        int NumberOfDiseases = diseaseMap.size();
        if (hpoTerm2DiseaseCount.containsKey(hpoId)) { // If the hpoTerm2DiseaseCount contains the HPO term
            return hpoTerm2DiseaseCount.get(hpoId)*(1.0) / NumberOfDiseases; //return number of diseases wit HPO term divided by total number of diseases
        } else {
            return 0;
        }
    }

    /**
     * If disease has the HPO term, return 0.9; else return 0 (initial approach/simplification)
     * TODO later calculate the actual frequency if possible
     *
     * @param diseaseID
     * @param hpoId
     * @return The frequency of HPO feature (hpoId) in patients with the given disease
     */
    private double getFrequency(String diseaseID, TermId hpoId) {
        Disease disease1 = diseaseMap.get(diseaseID);
        if (disease1 != null && disease1.getHpoIds().contains(hpoId))
            return 0.9;
        else
            return 0.0;
    }

    /**
     * This function gets HPO IDs of a patient and calculates the likelihood ratio, Pretest odds,
     * Posttest odds and Posttest Probabilities for each disease.
     */


    @Test
    public void ResultsLrOddProb() {
        getHPOIdFile();
        parseHPOData(hpopath, annotpath);
        debugPrintOntology();
        createDiseaseModels();
        try {
            initializeTerm2DiseaseMap();
        } catch (Exception e) {
            System.err.println(e);
        }

        for (String disease : diseaseMap.keySet()) {
            List<HPOTestResult> results = new ArrayList<>();
            for (TermId id : ListOfTermIdsOfHPOTerms) {
                HPOTestResult result = new HPOTestResult(getFrequency(disease, id), getBackgroundFrequency(id));
                results.add(result);
            }
            HPOLRTest hpolrtest = new HPOLRTest(results, PretestProb, TestSign);
            LikelihoodRatios.add(hpolrtest.getCompositeLikelihoodRatio());
            Disease2LR.put(disease,(-1)*hpolrtest.getCompositeLikelihoodRatio());
            PretestOdds.add(hpolrtest.getPretestOdds());
            PostTestOdds.add(hpolrtest.getPosttestOdds());
            PostTestProb.add(hpolrtest.getPosttestProbability());

        }
        Disease2LR = MapUtil.sortByValue(Disease2LR);
        for(String disease: Disease2LR.keySet()){
            double LR =  Disease2LR.get(disease);
            Disease2LR.put(disease, (-1)*LR);

        }
    }



}

