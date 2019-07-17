package org.monarchinitiative.lirical.simulation;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.monarchinitiative.lirical.cmd.LiricalCommand;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.phenol.ontology.data.Ontology;


@Parameters(commandDescription = "Perform \"NOT\" simulation", hidden = true)
public class NotCommand extends LiricalCommand {

    /** Directory where various files are downloaded/created. */
    @Parameter(names={"-d","--data"}, description ="directory to download data" )
    private String datadir="data";
    @Parameter(names={"-n"}, description = "number of cases to simulate")
    private int n_cases = 100;

    public NotCommand() {}

    public void run() {
        LiricalFactory factory = new LiricalFactory.Builder()
                .datadir(this.datadir)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        Ontology hpo = factory.hpoOntology();
        NotSimulator notsim = new NotSimulator(hpo,factory.diseaseMap(hpo));
        notsim.runSimulations(n_cases);
    }

}
