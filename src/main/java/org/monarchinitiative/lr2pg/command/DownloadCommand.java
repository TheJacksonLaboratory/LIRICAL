package org.monarchinitiative.lr2pg.command;

import org.apache.log4j.Logger;

public class DownloadCommand extends Command {
    static Logger logger = Logger.getLogger(DownloadCommand.class.getName());

    private String downloadDirectory=null;

    public DownloadCommand(String path){
        this.downloadDirectory=path;
    }

    public void execute() {
        logger.trace("executing download command");
    }
}
