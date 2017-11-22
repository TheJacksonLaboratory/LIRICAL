package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Represents a single case and the HPO terms assigned to the case (patient), as well as the results of the
 * likelihood ratio analysis.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.1 (2017-11-24)
 */
public class Case {

    private static TermPrefix HP_PREFIX = new ImmutableTermPrefix("HP");
    private Disease2TermFrequency disease2TermFrequencyMap=null;



    public Case(String hpoPath, String annotationPath, String caseData) {
        this.disease2TermFrequencyMap= new Disease2TermFrequency(hpoPath,annotationPath);
        parseCasedata(caseData);
    }


    private void parseCasedata(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line=br.readLine())!= null) {
                System.out.println(line);
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
