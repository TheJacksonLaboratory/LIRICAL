package org.monarchinitiative.lr2pg.cmd;

import org.monarchinitiative.lr2pg.analysis.GridSearch;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class GridSearchCommand extends SimulatePhenotypesCommand {

    public GridSearchCommand(String dataDirPath){
        super(dataDirPath);
    }

    public void run() throws Lr2pgException {
        HpoOntology ontology = initializeOntology();
        Map<TermId, HpoDisease> diseaseMap = parseHpoAnnotations(ontology);
        GridSearch gridSearch = new GridSearch(ontology,diseaseMap);
        gridSearch.gridsearch();
    }
}
