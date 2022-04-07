package org.monarchinitiative.lirical.cli.cmd;


import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Download a number of files needed for the analysis. We download by default to a subdirectory called
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "download", aliases = {"D"},
        mixinStandardHelpOptions = true,
        description = "Download files for LIRICAL")
public class DownloadCommand implements Callable<Integer>{
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);

    @CommandLine.Option(names={"-d","--data"},
            description ="directory to download data (default: ${DEFAULT-VALUE})" )
    public Path datadir = Path.of("data");

    @CommandLine.Option(names={"-w","--overwrite"},
            description = "overwrite previously downloaded files (default: ${DEFAULT-VALUE})")
    public boolean overwrite = false;

    @Override
    public Integer call() {
        try {
            logger.info("Downloading data to {}", datadir.toAbsolutePath());
            BioDownloader downloader = BioDownloader.builder(datadir)
                    .overwrite(overwrite)
                    .hpoJson()
                    .hpDiseaseAnnotations()
                    .geneInfoHuman()
                    .medgene2MIM()
                    .build();
            downloader.download();
            logger.info("Done!");
            return 0;
        } catch (FileDownloadException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return 1;
        }
    }

}
