package org.monarchinitiative.lr2pg.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.Map;

/**
 * This class coordinates simulation of cases with only phenotype.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Simulate phenotype-only cases")
public class SimulatePhenotypesCommand extends Lr2PgCommand {
    private static final Logger logger = LogManager.getLogger();
    /** path to hp.obo file. (Must be in same directory as phenotype.hpoa). Set via {@link #datadir}. */
    private String hpoOboPath;
    /** path to phenotype.hpoa file. (Must be in same directory as hp.obo). Set via {@link #datadir}. */
    private String phenotypeAnnotationPath;
    /** Directory that contains {@code hp.obo} and {@code phenotype.hpoa} files. */
    @Parameter(names={"-d","--data"}, description ="directory to download data" )
    private String datadir="data";
    @Parameter(names={"-c","--n_cases"}, description="Number of cases to simulate")
    private int n_cases_to_simulate = 25;
    @Parameter(names={"-h","--n_hpos"}, description="Number of HPO terms per case")
    private int n_terms_per_case = 5;
    @Parameter(names={"-n","--n_noise"}, description="Number of noise terms per case")
    private int n_noise_terms = 1;
    @Parameter(names={"-i","--imprecision"}, description="Use imprecision?")
    private boolean imprecise_phenotype = false;



    public SimulatePhenotypesCommand(){

    }


    @Override
    public void run() throws Lr2pgException {
        File datadirFile = new File(datadir);
        String absDirPath=datadirFile.getAbsolutePath();
        this.hpoOboPath=String.format("%s%s%s",absDirPath,File.separator,"hp.obo");
        this.phenotypeAnnotationPath=String.format("%s%s%s",absDirPath,File.separator,"phenotype.hpoa");

        Lr2PgFactory factory = new Lr2PgFactory.Builder()
                .hp_obo(hpoOboPath)
                .phenotypeAnnotation(phenotypeAnnotationPath)
                .build();
        Ontology ontology = factory.hpoOntology();
        Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);

        PhenotypeOnlyHpoCaseSimulator phenotypeOnlyHpoCaseSimulator = new PhenotypeOnlyHpoCaseSimulator(ontology,
                diseaseMap,
                n_cases_to_simulate,
                n_terms_per_case,
                n_noise_terms,
                imprecise_phenotype);
        logger.info("Simulating {} cases with {} terms each, {} noise terms. imprecision={}",
            n_cases_to_simulate,n_terms_per_case,n_noise_terms,imprecise_phenotype);
        phenotypeOnlyHpoCaseSimulator.simulateCases();
    }
}
