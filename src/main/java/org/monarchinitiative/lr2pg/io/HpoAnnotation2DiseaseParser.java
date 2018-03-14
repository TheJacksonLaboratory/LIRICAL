package org.monarchinitiative.lr2pg.io;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;

import java.util.*;


/**
 * This class parses the phenotype_annotation.tab file into a collection of HpoDisease objects.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class HpoAnnotation2DiseaseParser {
    private static final Logger logger = LogManager.getLogger();
    /** Location of the V2 big file annotation file, {@code phenotype.hpoa}. */
    private String annotationFilePath =null;
    /** Reference to the HPO ontology. */
    private HpoOntology ontology;
    /** Key: a disease ID such as OMIM:123456. Value: Corresponding disease object. */
    private Map<String,HpoDisease> diseaseMap;



    public HpoAnnotation2DiseaseParser(String annotationFile, HpoOntology ontology){
        this.annotationFilePath =annotationFile;
        this.ontology=ontology;
        this.diseaseMap=new HashMap<>();
        parseAnnotation();
    }


    public Map<String, HpoDisease> getDiseaseMap() {
        return diseaseMap;
    }

    /** Parse the {@code phenotype.hpoa} file  */
    private void  parseAnnotation() {
        logger.trace(String.format("Parsing annotations at %s",annotationFilePath));
        // First stage of parsing is to get the lines parsed and sorted according to disease.
        HpoDiseaseAnnotationParser parser = new HpoDiseaseAnnotationParser(annotationFilePath,ontology);
        try {
            diseaseMap = parser.parse();
        } catch (PhenolException e) {
            logger.fatal("Could not parse annotation data...not a recoverable error");
            logger.fatal(e);
            System.exit(1);
        }
    }

}
