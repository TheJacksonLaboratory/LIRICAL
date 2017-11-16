package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.likelihoodratio.HPOLRTest;
import org.monarchinitiative.lr2pg.likelihoodratio.HPOTestResult;
import org.monarchinitiative.lr2pg.old.Disease;
import org.monarchinitiative.lr2pg.old.HPOParser;
import org.monarchinitiative.lr2pg.old.MapUtil;


import java.io.*;
import java.util.*;

public class HPOParserTest {
    private static String hpoPath="hp.obo";
    private static Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;
    /** 2000 randomly chosen HPO Annotations */
    private static List<HpoDiseaseAnnotation> annotations=null;
    private static HPOParser parser=null;
    private static Map<String,Disease> diseaseMap=null;
    Map<TermId, Integer> hpoTerm2DiseaseCount = null;
    Map<String,Double> Disease2LR = new HashMap();
    //List to store list of HPO terms of a patient
    List<String> HPOTermsPatient = new ArrayList<>();
    //List to store TermIds of HPO terms of a patient
    List<TermId> ListOfTermIdsOfHPOTerms = new ArrayList<>();
    private static String fileName = "/Users/ravanv/Documents/HPO_LRTest/LR2PG/HPOTerms.txt";
    char TestSign = 'P';

    double PretestProb = 0.5;
    List<Double> LikelihoodRatios = new ArrayList<Double> ();
    List<Double> PretestOdds = new ArrayList<Double> ();
    List<Double> PostTestOdds = new ArrayList<Double> ();
    List<Double> PostTestProb = new ArrayList<Double> ();


    @BeforeClass
    public static void setup() {
        ClassLoader classLoader = HPOParserTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        parser = new HPOParser();
        hpoOntology = parser.parseOntology(hpoPath);
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
      //  String annotationPath = "/Users/ravanv/Documents/HPO_LRTest/LR2PG/HPO/phenotype_annotation.tab";
        //parser.parseAnnotation(annotationPath);
        annotations = parser.getAnnotList();
        parser.initializeTermMap();
        diseaseMap = parser.createDiseaseModels();
    }


//    public void debugPrintDiseaseMap() {
//        for (String d: diseaseMap.keySet()) {
//            Disease disease = diseaseMap.get(d);
//            System.err.println(String.format("Disease: %s: HPO ids: %d",disease.getName(),disease.getHpoIds().size()));
//        }
//
//
//    }
//    private double getBackgroundFrequency(TermId hpoId) {
//        int NumberOfDiseases = diseaseMap.size();
//            if (hpoTerm2DiseaseCount.containsKey(hpoId)) { // If the hpoTerm2DiseaseCount contains the HPO term
//                return (hpoTerm2DiseaseCount.get(hpoId)*(1.0) / NumberOfDiseases); //return number of diseases wit HPO term divided by total number of diseases
//        } else {
//            return 0;
//        }
//    }

//    /**
//     * If disease has the HPO term, return 0.9; else return 0 (initial approach/simplification)
//     * TODO later calculate the actual frequency if possible
//     *
//     * @param diseaseID
//     * @param hpoId
//     * @return The frequency of HPO feature (hpoId) in patients with the given disease
//     */
//    private double getFrequency(String diseaseID, TermId hpoId) {
//        Disease disease1 = diseaseMap.get(diseaseID);
//        if (disease1 != null && disease1.getHpoIds().contains(hpoId))
//            return 0.9;
//        else
//            return 0.0;
//    }



    private void initializeTerm2DiseaseMap() throws Exception {
        hpoTerm2DiseaseCount = new HashMap<>();
        int good=0,bad=0;
        for(Disease disease: diseaseMap.values()){
            System.err.println(String.format("Disease %s", disease.getName()));
            for (TermId termId : disease.getHpoIds()) {
                System.err.println(String.format("Term %s Status %s",
                        termId.getIdWithPrefix(), hpoOntology.getAllTermIds().contains(termId)));
            }

                Collection<TermId> ids = disease.getHpoIds();
                if (ids==null) {
                    String msg="TermIds NULL";
                    throw new Exception(msg);
                } else if (ids.size()==0) {
                    System.err.println("TermIds zero size");
                    System.err.println("disease: " + disease.getName());
//                    debugPrintDiseaseMap();
                    //System.exit(17);
                    String msg = String.format("Disease %s had zero HpoIds",disease.getName());
                    throw new Exception(msg);
                }
                ids.remove(null);
                Set<TermId> ancestors = hpoOntology.getAllAncestorTermIds(ids);
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


  /*  @Test
    public void testGetHpoOntology(){
        Assert.assertTrue(hpoOntology != null);
        int expectedNumberOfAnnotationLines=2000;
        Assert.assertEquals(expectedNumberOfAnnotationLines,annotations.size());
    }*/


  /*  @Test
    public void testCreateDiseaseMap() {
        int expectedNumberOfDiseases=196;
        Assert.assertEquals(expectedNumberOfDiseases, diseaseMap.size());
    }*/

   /* @Test
    public void testAarskogSyndrome () {
        String aarskogKey="100050";
        Disease aarskog = diseaseMap.get(aarskogKey);
        Assert.assertNotNull(aarskog);
        int expectedTotalHpoForAarskog=40; // non-inheritance terms only!
        Assert.assertEquals(expectedTotalHpoForAarskog,aarskog.getHpoIds().size());

    }*/

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

//    @Test
//    public void testHPO2count(){
//        try {
//            initializeTerm2DiseaseMap();
//        } catch (Exception e) {
//            System.err.println(e);
//        }
//        getHPOIdFile();
//
//        for (String disease : diseaseMap.keySet()) {
//            List<HPOTestResult> results = new ArrayList<>();
//            for (TermId id : ListOfTermIdsOfHPOTerms) {
//                if(hpoTerm2DiseaseCount.containsKey(id)) {
//                    HPOTestResult result = new HPOTestResult(getFrequency(disease, id), getBackgroundFrequency(id));
//                    results.add(result);
//                }
//            }
//            HPOLRTest hpolrtest = new HPOLRTest(results, PretestProb, TestSign);
//            LikelihoodRatios.add(hpolrtest.getCompositeLikelihoodRatio());
//            Disease2LR.put(disease,hpolrtest.getCompositeLikelihoodRatio());
//            PretestOdds.add(hpolrtest.getPretestOdds());
//            PostTestOdds.add(hpolrtest.getPosttestOdds());
//            PostTestProb.add(hpolrtest.getPosttestProbability());
//
//        }
//        Disease2LR = MapUtil.sortByValue(Disease2LR);
//        /*for(String disease: Disease2LR.keySet()){
//          double LR =  Disease2LR.get(disease);
//          Disease2LR.put(disease, (-1)*LR);
//
//        }*/
//        try(BufferedWriter writer = new BufferedWriter(new FileWriter("myfile.txt",false)))
//        {
//            for(String disease:Disease2LR.keySet()){
//                writer.write(disease);
//                writer.write("  ");
//                writer.write(String.valueOf(Disease2LR.get(disease)));
//                writer.newLine();
//            }
//
//        }
//        catch(IOException e){
//            System.out.println(e.getMessage());
//        }
//    }






}
