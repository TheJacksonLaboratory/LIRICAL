package org.monarchinitiative.lirical.benchmark.cmd;


import org.monarchinitiative.lirical.benchmark.simulation.GridSearch;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Run a grid search over number of terms and number of noise terms for
 * phenotype-only LIRICAL. Can be run with or with imprecision.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@CommandLine.Command(name = "grid",
        aliases = {"G"},
        mixinStandardHelpOptions = true,
        description = "Grid search for simulation of phenotype-only cases")
public class GridSearchCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(GridSearchCommand.class);
    /** Directory that contains {@code hp.obo} and {@code phenotype.hpoa} files. */
    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    private String datadir="data";
    @CommandLine.Option(names={"-c","--n_cases"}, description="Number of cases to simulate (default: ${DEFAULT-VALUE})")
    private int n_cases_to_simulate = 100;
    @CommandLine.Option(names={"-i","--imprecision"}, description="Use imprecision? (default: ${DEFAULT-VALUE})")
    private boolean imprecise_phenotype = false;


    public GridSearchCommand(){
        super();
    }

    @Override
    public Integer call() throws LiricalException {
        throw new LiricalException("Sorry, not yet re-implemented");
//        LiricalFactory factory = LiricalFactory.builder()
//                .datadir(this.datadir) // TODO - fix
//                .build();
//        factory.qcHumanPhenotypeOntologyFiles();
//        logger.trace("Grid search: Simulating {} cases. imprecision={}",
//                n_cases_to_simulate,imprecise_phenotype?"yes":"no");
//        Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(factory.hpoOntology());
//        GridSearch gridSearch = new GridSearch(factory.hpoOntology(),diseaseMap, n_cases_to_simulate, imprecise_phenotype);
//        gridSearch.gridsearch();
//        return 0;
    }
}
