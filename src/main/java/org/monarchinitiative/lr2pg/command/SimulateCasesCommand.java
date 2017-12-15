package org.monarchinitiative.lr2pg.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCaseSimulator;

public class SimulateCasesCommand extends Command {
    private static final Logger logger = LogManager.getLogger();
    private final String dataDirectoryPath;


    public SimulateCasesCommand(String datadir ) {
        dataDirectoryPath=datadir;
    }

    public void execute() {
        logger.trace("Executing HpoCase simulation");
        HpoCaseSimulator simulator = new HpoCaseSimulator(this.dataDirectoryPath);
        simulator.debugPrint();
        simulator.simulateCases();
    }
}
