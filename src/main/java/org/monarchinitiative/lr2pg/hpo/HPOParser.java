package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.base.TermAnnotationParserException;
import com.github.phenomics.ontolib.io.obo.hpo.HpoDiseaseAnnotationParser;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.*;
import org.apache.log4j.Logger;
import org.monarchinitiative.lr2pg.prototype.Disease;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * This class uses the <a href="https://github.com/phenomics/ontolib">ontolb</a> library to
 * parse both the {@code hp.obo} file and the phenotype annotation file
 * {@code phenotype_annotation.tab}
 * (see <a href="http://human-phenotype-ontology.github.io/">HPO Homepage</a>).
 * @author Peter Robinson
 * @author Vida Ravanmehr
 * @version 0.0.1
 */
public class HPOParser {
    static Logger logger = Logger.getLogger(HPOParser.class.getName());
    Ontology<HpoTerm, HpoTermRelation> inheritance=null;
    Ontology<HpoTerm, HpoTermRelation> abnormalPhenoSubOntology=null;
    /** Map of all of the Phenotypic abnormality terms (i.e., not the inheritance terms). */
    private Map<TermId,HpoTerm> termmap=null;
    /** List of all annotations parsed from phenotype_annotation.tab. */
    private List<HpoDiseaseAnnotation> annotList=null;









    public Map<String,Disease> createDiseaseModels() {
        Map<String,Disease> diseaseMap = new HashMap<>();
        logger.trace("createDiseaseModels");
        for (HpoDiseaseAnnotation annot: annotList) {
            String database=annot.getDb(); /* e.g., OMIM, ORPHA, DECIPHER */
            String diseaseName=annot.getDbName(); /* e.g., Marfan syndrome */
            String diseaseId = annot.getDbObjectId(); /* e.g., OMIM:100543 */
            TermId hpoId  = annot.getHpoId();
            // TODO ADD FREQUENCY
            //annot.getFrequency();
            Optional<Float> fr = annot.getFrequency();
            if (fr.isPresent()) {
                float f = fr.get();
            } else {
                // not there
            }
            //annot.getFrequencyModifier();
            /* Filter database to just get OMIM */
            Disease disease=null;
            if (diseaseMap.containsKey(diseaseId)) {
                disease=diseaseMap.get(diseaseId);
            } else {
                disease = new Disease(database,diseaseName,diseaseId); //String database, String name,String id
                diseaseMap.put(diseaseId,disease);
            }
            if ( this.termmap.containsKey(hpoId)) { // restrict to clinical terms, i.e., not inheritance.
                disease.addHpo(hpoId);
            } else {
                logger.trace("Not adding term "+ hpoId.getId());
            }
        }
        return diseaseMap;
    }


    public Map<TermId,HpoTerm> extractStrictPhenotypeTermMap() {
        Map<TermId,HpoTerm> map = new HashMap<>();
        Set<TermId> phenoTermIds = this.abnormalPhenoSubOntology.getNonObsoleteTermIds();
        for (TermId id : phenoTermIds) {
            map.put(id,abnormalPhenoSubOntology.getTermMap().get(id));
        }
        return map;
    }

    public Ontology<HpoTerm, HpoTermRelation>  parseOntology(String HPOpath) {
        HpoOntology hpo;
        TermPrefix pref = new ImmutableTermPrefix("HP");
        TermId inheritId = new ImmutableTermId(pref,"0000005");
        try {
            HpoOboParser hpoOboParser = new HpoOboParser(new File(HPOpath));
            hpo = hpoOboParser.parse();
            this.abnormalPhenoSubOntology = hpo.getPhenotypicAbnormalitySubOntology();
            this.inheritance = hpo.subOntology(inheritId);
        } catch (IOException e) {
            logger.error(String.format("Unable to parse HPO OBO file at %s", HPOpath ));
            logger.error(e,e);
            System.exit(1);
        }
        return abnormalPhenoSubOntology;
    }






    public Ontology<HpoTerm, HpoTermRelation> getInheritanceSubontology() {
        Map<TermId,HpoTerm> submap = inheritance.getTermMap();
        Set<TermId> actual = inheritance.getNonObsoleteTermIds();
        for (TermId t:actual) {
            System.out.println("INHERITANCE GOT TERM "+ submap.get(t).getName());
        }
        return this.inheritance;
    }


    public List<HpoDiseaseAnnotation> getAnnotList() {
        return annotList;
    }

    /**
     * @param annotationPath Path to the phenotype_annotation.tab file
     * @return A list of disease-HPO phenotype annotations.
     */
    public void  parseAnnotation(String annotationPath) {
        File inputFile = new File(annotationPath);
        logger.trace(String.format("Parsing annotations at %s (%s)",annotationPath,inputFile));
        annotList = new ArrayList<>();
        try {
            HpoDiseaseAnnotationParser parser = new HpoDiseaseAnnotationParser(inputFile);
            while (parser.hasNext()) {
                HpoDiseaseAnnotation anno = parser.next();
                annotList.add(anno);
            }
        } catch (IOException e) {
            System.err.println("Problem reading from file.");
        } catch (TermAnnotationParserException e) {
            System.err.println("Problem parsing file.");
        }

    }


    public void initializeTermMap() {
        this.termmap = extractStrictPhenotypeTermMap();
    }

    /**
     * Returns the frequency of an HPO annotation among all diseases of our corpus, i.e., in {@link # Map diseaseMap}.
     * @param hpoId The HPO Term whose frequency we want to know
     * @return frequency of hpoId among all diseases
     */
    public double getBackgroundFrequency(TermId hpoId, Map<String,Disease>diseaseMap, Map<TermId, Integer> hpoTerm2DiseaseCount) {
        int NumberOfDiseases = diseaseMap.size();
        // If the hpoTerm2DiseaseCount contains the HPO term
        if (hpoTerm2DiseaseCount.containsKey(hpoId))
            //return number of diseases wit HPO term divided by total number of diseases
            //Needs to be completed!!! i should include getFrequency in this formula!!
        {
            return ((int) hpoTerm2DiseaseCount.get(hpoId)* 1.0 /NumberOfDiseases);
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
    public double getFrequency(String diseaseID, TermId hpoId,Map<String,Disease>diseaseMap) {
        Disease disease1 = (Disease) diseaseMap.get(diseaseID);
        if (disease1 != null && disease1.getHpoIds().contains(hpoId))
            return 0.9;
        else
            return 0.0;
    }

    public void initializeTerm2DiseaseMap(Map<TermId, Integer> hpoTerm2DiseaseCount, Map<String, Disease> diseaseMap, Ontology<HpoTerm, HpoTermRelation> hpoOntology) throws Exception {
        //hpoTerm2DiseaseCount = new HashMap<>();
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
                debugPrintDiseaseMap(diseaseMap);
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

    public void debugPrintDiseaseMap(Map <String, Disease>diseaseMap) {
        for (String d: diseaseMap.keySet()) {
            Disease disease = diseaseMap.get(d);
            System.err.println(String.format("Disease: %s: HPO ids: %d",disease.getName(),disease.getHpoIds().size()));
        }


    }



}
