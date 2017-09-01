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
     * @param annotationPath Path to the phenotype_annotation.tab file
     * @return A list of disease-HPO phenotype annotations.
     */
    public List<HpoDiseaseAnnotation> parseAnnotation(String annotationPath) {
        File inputFile = new File(annotationPath);
        logger.trace(String.format("Parsing annotations at %s (%s)",annotationPath,inputFile));
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
