package org.monarchinitiative.lr2pg.configuration;

import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpOboParser;

import java.io.File;
import java.io.FileNotFoundException;

public class Configurator {
    private HpoOntology ontology=null;

    private String hpOboPath;

    public Configurator(String hpOboPath) {
        this.hpOboPath=hpOboPath;
    }

    public HpoOntology get() {
        if (this.ontology!=null) return ontology;
        try {
            HpOboParser parser = new HpOboParser(new File(this.hpOboPath));
            ontology = parser.parse();
            return ontology;
        } catch (PhenolException | FileNotFoundException ioe) {
            System.err.println("Could not parse hp.obo file: " + ioe.getMessage());
            throw new RuntimeException("Could not parse hp.obo file: " + ioe.getMessage());
        }
    }

}
