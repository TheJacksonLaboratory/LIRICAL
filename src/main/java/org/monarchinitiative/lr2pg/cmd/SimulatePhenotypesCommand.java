package org.monarchinitiative.lr2pg.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpOboParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * This class coordinates simulation of cases with only phenotype.
 * TODO allow client code to set parameters
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class SimulatePhenotypesCommand extends Lr2PgCommand {
    private static final Logger logger = LogManager.getLogger();

    private final String hpoOboPath;
    /** path to phenotype.hpoa file. */
    private final String phenotypeAnnotationPath;


    private final int DEFAULT_CASES_TO_SIMULATE=25;
    private final int DEFAULT_TERMS_PER_CASE=5;
    private final int DEFAULT_NOISE_TERMS=1;
    private final boolean DEFAULT_IMPRECISION=false;

    private int n_cases_to_simulate = DEFAULT_CASES_TO_SIMULATE;
    private int n_terms_per_case = DEFAULT_TERMS_PER_CASE;
    private int n_noise_terms = DEFAULT_NOISE_TERMS;
    private boolean imprecise_phenotype = DEFAULT_IMPRECISION;



    public SimulatePhenotypesCommand(String dataDirPath){
        File dirpath = new File(dataDirPath);
        String absDirPath=dirpath.getAbsolutePath();
        this.hpoOboPath=String.format("%s%s%s",absDirPath,File.separator,"hp.obo");
        this.phenotypeAnnotationPath=String.format("%s%s%s",absDirPath,File.separator,"phenotype.hpoa");
    }

    /** Initialize the HpoOnotlogy object from the hp.obo file. */
    protected HpoOntology initializeOntology() throws Lr2pgException{
        try {
            HpOboParser parser = new HpOboParser(new File(this.hpoOboPath));
            HpoOntology ontology = parser.parse();
            return ontology;
        } catch (PhenolException | FileNotFoundException ioe) {
            throw new Lr2pgException("Could not parse hp.obo file: " + ioe.getMessage());
        }
    }



     protected Map<TermId, HpoDisease> parseHpoAnnotations(HpoOntology ontology) throws Lr2pgException {
        if (ontology==null) {
            throw new Lr2pgException("HpoOntology object not intitialized");
        }
        HpoDiseaseAnnotationParser parser = new HpoDiseaseAnnotationParser(phenotypeAnnotationPath,ontology);
         try {
             return parser.parse();
         } catch (PhenolException pe ) {
             throw new Lr2pgException("Could not parse disease associations: " + pe.getMessage());
         }
    }


    protected PhenotypeOnlyHpoCaseSimulator getPhenotypeOnlySimulator()throws Lr2pgException {
        HpoOntology ontology = initializeOntology();
        Map<TermId, HpoDisease> diseaseMap = parseHpoAnnotations(ontology);
        return new PhenotypeOnlyHpoCaseSimulator(ontology,
                diseaseMap,
                n_cases_to_simulate,
                n_terms_per_case,
                n_noise_terms,
                imprecise_phenotype);
    }


    public void run() throws Lr2pgException {
        PhenotypeOnlyHpoCaseSimulator phenotypeOnlyHpoCaseSimulator = getPhenotypeOnlySimulator();
        logger.info("Simulating {} cases with {} terms each, {} noise terms. imprecision={}",
            n_cases_to_simulate,n_terms_per_case,n_noise_terms,imprecise_phenotype);
        phenotypeOnlyHpoCaseSimulator.simulateCases();

    }
}
