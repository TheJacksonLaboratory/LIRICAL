package org.monarchinitiative.lirical.cli.cmd;


import org.monarchinitiative.lirical.bootstrap.LiricalFactory;
import org.monarchinitiative.lirical.cli.simulation.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class coordinates simulation of cases with only phenotype. It draws HPOs at random and then
 * performs analysis and records the rank.
 * This differs from {@link org.monarchinitiative.lirical.cli.simulation.GridSearch} because GridSearch essentially
 * runs this analysis for different numbers of HPO observed/noise terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter N Robinson</a>
 */

@CommandLine.Command(name = "simulate",
        aliases = {"SP"},
        mixinStandardHelpOptions = true,
        description = "Simulate phenotype-only cases",
        hidden = true)
public class SimulatePhenotypeOnlyCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(SimulatePhenotypeOnlyCommand.class);
    /** Directory that contains {@code hp.obo} and {@code phenotype.hpoa} files. */
    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data" )
    private String datadir="data";
    @CommandLine.Option(names={"-c","--n_cases"}, description="Number of cases to simulate")
    private int n_cases_to_simulate = 25;
    @CommandLine.Option(names={"--n_hpos"}, description="Number of HPO terms per case")
    private int n_terms_per_case = 5;
    @CommandLine.Option(names={"-n","--n_noise"}, description="Number of noise terms per case")
    private int n_noise_terms = 1;
    @CommandLine.Option(names={"-i","--imprecision"}, description="Use imprecision?")
    private boolean imprecise_phenotype = false;


    /** No-op constructor meant to demo the phenotype LIRICAL algorithm by simulating some case based on
     * randomly chosen diseases and HPO terms.
     */
    public SimulatePhenotypeOnlyCommand(){
    }


    @Override
    public Integer call() {
        LiricalFactory factory = LiricalFactory.builder()
//                .datadir(this.datadir) // TODO - fix
                .build();
//        factory.qcHumanPhenotypeOntologyFiles();
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
        try {
            phenotypeOnlyHpoCaseSimulator.simulateCases();
        } catch (LiricalException e) {
            e.printStackTrace(); // should never happen, but nothing we can do about it
        }
        return 0;
    }
}
