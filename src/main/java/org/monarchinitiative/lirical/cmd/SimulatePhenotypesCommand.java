package org.monarchinitiative.lirical.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import org.monarchinitiative.lirical.configuration.Lr2PgFactory;
import org.monarchinitiative.lirical.exception.Lr2pgException;
import org.monarchinitiative.lirical.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This class coordinates simulation of cases with only phenotype.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Simulate phenotype-only cases",hidden = true)
public class SimulatePhenotypesCommand extends Lr2PgCommand {
    private static final Logger logger = LoggerFactory.getLogger(SimulatePhenotypesCommand.class);
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


    /** No-op constructor meant to demo the phenotype LR2PG algorithm by similating some case based on
     * randomly chosen diseases and HPO terms.
     */
    public SimulatePhenotypesCommand(){
    }


    @Override
    public void run() throws Lr2pgException {
        Lr2PgFactory factory = new Lr2PgFactory.Builder()
                .datadir(this.datadir)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        logger.trace("Running simulation with {} cases, {} terms/case, {} noise terms/case. Imprecision: {}",
                n_cases_to_simulate,n_terms_per_case,n_noise_terms,imprecise_phenotype?"yes":"no");
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
