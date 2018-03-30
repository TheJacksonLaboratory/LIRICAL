package org.monarchinitiative.lr2pg.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCaseSimulator;

/**
 * This is a demonstration of the likelihood ratio algorithm that uses simulated cases to assess the performance of the
 * algorithm.
 */
public class SimulateCasesCommand implements Command {
    private static final Logger logger = LogManager.getLogger();
    /** Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}. */
    private final String dataDirectoryPath;

    /**
     * @param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    public SimulateCasesCommand(String datadir, int n_cases_to_simulate, int n_terms_per_case, int n_noise_terms ) {
        dataDirectoryPath=datadir;
    }

    public void execute() {
        logger.trace("Executing HpoCase simulation");
        try {
            HpoCaseSimulator simulator = new HpoCaseSimulator(this.dataDirectoryPath);
            simulator.debugPrint();
            simulator.simulateCases();
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }
    }
}
