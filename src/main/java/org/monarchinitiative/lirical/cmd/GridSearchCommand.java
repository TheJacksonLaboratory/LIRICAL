package org.monarchinitiative.lirical.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.monarchinitiative.lirical.analysis.GridSearch;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Run a grid search over number of terms and number of noise terms for
 * phenotype-only LIRICAL. Can be run with or with imprecision.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Grid search for simulation of phenotype-only cases",hidden = true)
public class GridSearchCommand extends LiricalCommand {
    private static final Logger logger = LoggerFactory.getLogger(GridSearchCommand.class);
     /** Directory that contains {@code hp.obo} and {@code phenotype.hpoa} files. */
    @Parameter(names={"-d","--data"}, description ="directory to download data" )
    private String datadir="data";
    @Parameter(names={"-c","--n_cases"}, description="Number of cases to simulate")
    private int n_cases_to_simulate = 100;
    @Parameter(names={"-i","--imprecision"}, description="Use imprecision?")
    private boolean imprecise_phenotype = false;


    public GridSearchCommand(){
        super();
    }

    public void run() throws LiricalException {
        LiricalFactory factory = new LiricalFactory.Builder()
                .datadir(this.datadir)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        Ontology ontology = factory.hpoOntology();
        logger.trace("Grid search: Simulating {} cases. imprecision={}",
                n_cases_to_simulate,imprecise_phenotype?"yes":"no");
        Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
        GridSearch gridSearch = new GridSearch(ontology,diseaseMap, n_cases_to_simulate, imprecise_phenotype);
        gridSearch.gridsearch();
    }
}
