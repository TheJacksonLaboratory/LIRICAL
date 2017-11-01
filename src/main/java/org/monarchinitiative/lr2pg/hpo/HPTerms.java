package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ravanv on 10/3/17.
 */
public class HPTerms {

    private String pathToHpFile=null;
    //List to store list of HPO terms of a patient
    List<String> HPOTermsPatient = new ArrayList<>();
    //List to store TermIds of HPO terms of a patient


    public HPTerms(String path) {
        pathToHpFile=path;
    }



    /**
     * This function reads a disease name and the HPO terms from a file and stores HPO terms in a list
     * The disease and its corresponding HPO terms are obtained form Phenomizer
     * @return List of HPO terms of a patient
     */

    public void getHPOTermsfromFile() {
        String line = null;
        try {
            FileReader fileReader = new FileReader(pathToHpFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //The first line of the file is the name of disease. In this case, the disease is chosen from OMIM database
            line = bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                HPOTermsPatient.add(line);
            }

            bufferedReader.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + pathToHpFile + "'");
        } catch (IOException ex) {
            System.out.println("Error reading file '" + pathToHpFile + "'");
        }

    }


    /**
     * This function stores HPO IDs (first read as HPO term from a file) in the format of TermIds.
     * @return List of TermIds of HPO terms.
     **/

    public  void getHPOIdFile(List<TermId>ListOfTermIdsOfHPOTerms) {
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




}
