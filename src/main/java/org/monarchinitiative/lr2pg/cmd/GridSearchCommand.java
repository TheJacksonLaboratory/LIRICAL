package org.monarchinitiative.lr2pg.cmd;

import com.beust.jcommander.Parameters;
import org.monarchinitiative.lr2pg.analysis.GridSearch;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Grid search for simulation of phenotype-only cases")
public class GridSearchCommand extends SimulatePhenotypesCommand {

    public GridSearchCommand(){
        super();
    }

    public void run() throws Lr2pgException {
        Ontology ontology = initializeOntology();
        Map<TermId, HpoDisease> diseaseMap = parseHpoAnnotations(ontology);
        GridSearch gridSearch = new GridSearch(ontology,diseaseMap);
        gridSearch.gridsearch();
    }
}
