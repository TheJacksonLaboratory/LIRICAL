package org.monarchinitiative.lr2pg.cmd;

import org.monarchinitiative.lr2pg.io.HpoDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadCommand extends Lr2PgCommand {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);

    private final String datadir;

    private final boolean overwrite;

    public DownloadCommand(String dir) {
        this(dir,false);
    }

    public DownloadCommand(String dir,boolean overwr) {
        datadir=dir;
        this.overwrite=overwr;
    }


    public void run() {
        logger.warn(String.format("Download analysis to %s", datadir));
        HpoDownloader downloader = new HpoDownloader(datadir, overwrite);
        downloader.download();
    }
}
