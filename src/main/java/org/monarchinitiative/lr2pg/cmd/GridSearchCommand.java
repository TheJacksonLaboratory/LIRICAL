package org.monarchinitiative.lr2pg.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.monarchinitiative.lr2pg.analysis.GridSearch;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.Map;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Grid search for simulation of phenotype-only cases")
public class GridSearchCommand extends Lr2PgCommand {

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

    public void run() throws Lr2pgException {
        File datadirFile = new File(datadir);
        String absDirPath=datadirFile.getAbsolutePath();
        String hpoOboPath=String.format("%s%s%s",absDirPath,File.separator,"hp.obo");
        String phenotypeAnnotationPath=String.format("%s%s%s",absDirPath,File.separator,"phenotype.hpoa");

        Lr2PgFactory factory = new Lr2PgFactory.Builder()
                .hp_obo(hpoOboPath)
                .phenotypeAnnotation(phenotypeAnnotationPath)
                .build();
        Ontology ontology = factory.hpoOntology();
        Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);



        GridSearch gridSearch = new GridSearch(ontology,diseaseMap, n_cases_to_simulate);
        gridSearch.gridsearch();
    }
}
