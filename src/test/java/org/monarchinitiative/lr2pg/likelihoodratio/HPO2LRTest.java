package org.monarchinitiative.lr2pg.likelihoodratio;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.*;
import org.junit.Test;
import org.monarchinitiative.lr2pg.prototype.Disease;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class HPO2LRTest {
    private String path = "/Users/ravanv/Documents/HPO_LR1/LR2PG/HPO/phenotype_annotation.tab";
    private String hpopath="/Users/ravanv/Documents/HPO_LR1/LR2PG/HPO/hp.obo";
    private Ontology<HpoTerm, HpoTermRelation> ontology=null;
    /** Map of all of the Phenotypic abnormality terms (i.e., not the inheritance terms). */
    private Map<TermId,HpoTerm> termmap=null;

    /** List of all annotations parsed from phenotype_annotation.tab. */
    private List<HpoDiseaseAnnotation> annotList=null;

    private Map<String,Disease> diseaseMap=null;

    @Test
    public void findBadTerm() {
        // This initializes ontology (which should have just phenotype terms)
        parseHPOData(hpopath,path);
        // This gets the four TermIds for OMIM 613172 and puts them in a list
        List<TermId> ids = getTermsFromDisease();
        System.out.println(String.format("*** Looking for the %d ancestors of the TermId's used to annotate MIM:613172 ***",ids.size()));

        for (TermId id:ids) {
            System.out.println(String.format("Testing term id %s at %s",id.toString(),termmap.get(id).getName() ));
            if (this.termmap.containsKey(id)) {
                System.out.println(String.format("TermId %s (%s) was found in PhenotypicAbnormality subontology", termmap.get(id).getName(), termmap.get(id).getId()));
            } else {
                System.out.println(String.format("TermId %s was NOT found in PhenotypicAbnormality subontology", id.toString()));
            }
            Set<TermId> uniset = new HashSet<>();
            uniset.add(id);
            try {
                Set<TermId> ancestors = ontology.getAllAncestorTermIds(uniset);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }



    @Test
    public void testAutosomalDominantInheritance() {
        TermPrefix pref = new ImmutableTermPrefix("HP");
        TermId autosomalDominantId = new ImmutableTermId(pref,"0000006");
        parseHPOData(hpopath,path);
        System.out.println("About to test term: "+ autosomalDominantId.toString());
        Set<TermId> myset = new HashSet<>();
        myset.add(autosomalDominantId);
        try {
            Set<TermId> ancestors = ontology.getAllAncestorTermIds(myset);
            System.out.println(String.format("We extracted %d ancesters -OK!!",ancestors.size()));
        } catch (Exception e) {
            System.out.println("We crashed on Autosomal dominant");
            e.printStackTrace();
        }
    }





    private void parseHPOData(String HPOpath, String annotation) {
        HpoOntology hpoOntology;
        try {
            HpoOboParser hpoOboParser = new HpoOboParser(new File(HPOpath));
            hpoOntology = hpoOboParser.parse();
            this.ontology =  hpoOntology.getPhenotypicAbnormalitySubOntology();
            this.termmap=ontology.getTermMap();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }



    public List<TermId> getTermsFromDisease() {
        TermPrefix pref = new ImmutableTermPrefix("HP");
        List<TermId> idlist = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = null;

            while ((line=br.readLine())!=null) {
                if (! line.startsWith("OMIM"))
                    continue;
                String A[] = line.split("\t");
                String id=A[1];
                if (id.equals("613172")) {
                    String HPid = A[4];
                    if (HPid.startsWith("HP:")) {
                        HPid=HPid.substring(3);
                        TermId tid = new ImmutableTermId(pref,HPid);
                        idlist.add(tid);
                    }
                    //System.out.println(line + "\n\t"+HPid);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return idlist;
    }





}
