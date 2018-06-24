package org.monarchinitiative.lr2pg.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * Analyze the likelihood ratios for a case represented by a list of HPO terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2017-11-24)
 */
public class HpoCase2SvgCommand implements Command {
    private static final Logger logger = LogManager.getLogger();
    private final String dataDirectory;
    /** A disease CURIE such as OMIM:600100. */
    private final TermId diseaseCurie;

    private final int n_terms_per_case;
    private final int n_noise_terms;

    public HpoCase2SvgCommand(String datadir, String diseaseId, String outfileName, int terms_per_case, int noise_terms ) {

        this.diseaseCurie = TermId.constructWithPrefix(diseaseId);
        this.dataDirectory=datadir;
        this.n_terms_per_case=terms_per_case;
        this.n_noise_terms=noise_terms;
    }


    public void execute() {
        int cases_to_simulate=1;
        PhenotypeOnlyHpoCaseSimulator simulator = new PhenotypeOnlyHpoCaseSimulator( dataDirectory, cases_to_simulate, n_terms_per_case, n_noise_terms);
        try {
            HpoDisease disease = simulator.name2disease(diseaseCurie);
            HpoOntology ontology = simulator.getOntology();
            simulator.simulateCase(disease);
            TestResult result = simulator.getResults(disease);
            HpoCase hpocase = simulator.getCurrentCase();
            Lr2Svg l2svg = new Lr2Svg(hpocase,result, ontology);
            l2svg.writeSvg("test.svg");
        } catch (Lr2pgException e) {
            e.printStackTrace();
            System.err.println("Could not simulate case");
        }
    }

}
