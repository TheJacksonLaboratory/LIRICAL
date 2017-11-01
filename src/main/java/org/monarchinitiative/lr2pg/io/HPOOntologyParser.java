package org.monarchinitiative.lr2pg.io;


import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.*;
import org.apache.log4j.Logger;
import org.monarchinitiative.lr2pg.old.HPOParser;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * This class uses the <a href="https://github.com/phenomics/ontolib">ontolb</a> library to
 * parse both the {@code hp.obo} file and the phenotype annotation file
 * {@code phenotype_annotation.tab}
 * (see <a href="http://human-phenotype-ontology.github.io/">HPO Homepage</a>).
 * @author Peter Robinson
 * @author Vida Ravanmehr
 * @version 0.0.2 (2017-11-01)
 */


public class HPOOntologyParser {
    static Logger logger = Logger.getLogger(HPOParser.class.getName());
    /** Path to the {@code hp.obo} file. */
    private String hpoOntologyPath=null;


    Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology=null;
    Ontology<HpoTerm, HpoTermRelation> abnormalPhenoSubOntology=null;
    /** Map of all of the Phenotypic abnormality terms (i.e., not the inheritance terms). */
    private Map<TermId,HpoTerm> termmap=null;

    public HPOOntologyParser(String path){
        hpoOntologyPath=path;
    }


    public void parseOntology() throws IOException {
        HpoOntology hpo;
        TermPrefix pref = new ImmutableTermPrefix("HP");
        TermId inheritId = new ImmutableTermId(pref,"0000005");
        HpoOboParser hpoOboParser = new HpoOboParser(new File(hpoOntologyPath));
        hpo = hpoOboParser.parse();
        this.abnormalPhenoSubOntology = hpo.getPhenotypicAbnormalitySubOntology();
        this.inheritanceSubontology = hpo.subOntology(inheritId);
        Map<TermId,HpoTerm> submap = inheritanceSubontology.getTermMap();
        Set<TermId> actual = inheritanceSubontology.getNonObsoleteTermIds();
        for (TermId t:actual) {
            System.out.println("INHERITANCE GOT TERM "+ submap.get(t).getName());
        }
    }

    public Ontology<HpoTerm, HpoTermRelation> getPhenotypeSubontology() { return this.abnormalPhenoSubOntology; }
    public Ontology<HpoTerm, HpoTermRelation> getInheritanceSubontology() { return  this.inheritanceSubontology; }





}
