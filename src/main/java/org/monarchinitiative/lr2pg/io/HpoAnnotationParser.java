package org.monarchinitiative.lr2pg.io;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.io.base.TermAnnotationParserException;
import com.github.phenomics.ontolib.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class HpoAnnotationParser {
    static Logger logger = Logger.getLogger(HpoAnnotationParser.class.getName());

    public HpoAnnotationParser(){

    }



    /**
     * @param annotationPath Path to the phenotype_annotation.tab file
     * @return A list of disease-HPO phenotype annotations.
     */
    public void  parseAnnotation(String annotationPath) {
        File inputFile = new File(annotationPath);
//        logger.trace(String.format("Parsing annotations at %s (%s)",annotationPath,inputFile));
//        annotList = new ArrayList<>();
//        try {
//            HpoDiseaseAnnotationParser parser = new HpoDiseaseAnnotationParser(inputFile);
//            while (parser.hasNext()) {
//                HpoDiseaseAnnotation anno = parser.next();
//                annotList.add(anno);
//            }
//        } catch (IOException e) {
//            System.err.println("Problem reading from file.");
//        } catch (TermAnnotationParserException e) {
//            System.err.println("Problem parsing file.");
//        }

    }




}
