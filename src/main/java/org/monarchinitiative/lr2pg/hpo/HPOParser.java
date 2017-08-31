package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.base.TermAnnotationParserException;
import com.github.phenomics.ontolib.io.obo.hpo.HpoDiseaseAnnotationParser;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HPOParser {
    static Logger logger = Logger.getLogger(HPOParser.class.getName());

    public HPOParser() {

    }


    public Ontology<HpoTerm, HpoTermRelation>  parseOntology(String HPOpath) {
        HpoOntology hpo;
        Ontology<HpoTerm, HpoTermRelation> abnormalPhenoSubOntology =null;
        try {
            HpoOboParser hpoOboParser = new HpoOboParser(new File(HPOpath));
            hpo = hpoOboParser.parse();
            abnormalPhenoSubOntology = hpo.getPhenotypicAbnormalitySubOntology();
        } catch (IOException e) {
            logger.error(String.format("Unable to parse HPO OBO file at %s", HPOpath ));
            logger.error(e,e);
            System.exit(1);
        }
        return abnormalPhenoSubOntology;
    }


    /**
     *
     * @param annotationPath Path to the phenotype_annotation.tab file
     */
    public List<HpoDiseaseAnnotation> parseAnnotation(String annotationPath) {
        File inputFile = new File(annotationPath);
        List<HpoDiseaseAnnotation> annotList = new ArrayList<>();
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
        return annotList;

    }




}
