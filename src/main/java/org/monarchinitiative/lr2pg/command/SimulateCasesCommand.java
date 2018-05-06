package org.monarchinitiative.lr2pg.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCaseSimulator;

/**
 * This is a demonstration of the likelihood ratio algorithm that uses simulated cases to assess the performance of the
 * algorithm.
 */
public class SimulateCasesCommand implements Command {
    private static final Logger logger = LogManager.getLogger();
    /** Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}. */
    private final String dataDirectoryPath;
    private final int n_cases_to_simulate;
    private final int n_terms_per_case;
    private final int n_noise_terms;

    /**
     * @param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    public SimulateCasesCommand(String datadir, int cases_to_simulate, int terms_per_case, int noise_terms ) {
        dataDirectoryPath=datadir;
        this.n_cases_to_simulate=cases_to_simulate;
        this.n_terms_per_case=terms_per_case;
        this.n_noise_terms=noise_terms;
    }

    public void execute() {
        logger.trace("Executing HpoCaseOld simulation");
        HpoCaseSimulator simulator = new HpoCaseSimulator(this.dataDirectoryPath,n_cases_to_simulate, n_terms_per_case, n_noise_terms);
        simulator.debugPrint();
        simulator.simulateCases();
    }
}
