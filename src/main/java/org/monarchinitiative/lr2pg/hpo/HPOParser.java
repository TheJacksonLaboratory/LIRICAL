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




    public HPOParser() {

    }



    public Map<String,Disease> createDiseaseModels() {
        Map<String,Disease> diseaseMap = new HashMap<>();
        logger.trace("createDiseaseModels");
        for (HpoDiseaseAnnotation annot: annotList) {
            String database=annot.getDb(); /* e.g., OMIM, ORPHA, DECIPHER */
            String diseaseName=annot.getDbName(); /* e.g., Marfan syndrome */
            String diseaseId = annot.getDbObjectId(); /* e.g., OMIM:100543 */
            TermId hpoId  = annot.getHpoId();
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






}
