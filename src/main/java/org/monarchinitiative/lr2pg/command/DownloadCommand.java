package org.monarchinitiative.lr2pg.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadCommand extends Command {
    private static final Logger logger = LogManager.getLogger();

    private String downloadDirectory=null;

    public DownloadCommand(String path){
        this.downloadDirectory=path;
    }

    public void execute() {
        logger.trace("executing download command");
    }
}
