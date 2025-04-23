package org.monarchinitiative.lirical.cli.cmd;


import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Download a number of files needed for the analysis. We download by default to a subdirectory called
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@CommandLine.Command(name = "download",
        aliases = {"D"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Download files for LIRICAL.")
public class DownloadCommand extends BaseCommand {

    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);

    @CommandLine.Option(names={"-d","--data"},
            description ="directory to download data (default: ${DEFAULT-VALUE})" )
    public Path datadir = Path.of("data");

    @CommandLine.Option(names={"-w","--overwrite"},
            description = "overwrite previously downloaded files (default: ${DEFAULT-VALUE})")
    public boolean overwrite = false;

    @Override
    public Integer execute() {
        try {
            logger.info("Downloading data to {}", datadir.toAbsolutePath());

            BioDownloader downloader = BioDownloader.builder(datadir)
                    .overwrite(overwrite)
                    .hpoJson()
                    .hpDiseaseAnnotations()
                    .orphaToGene()
                    .hgnc()
                    .medgene2MIM()
                    // Jannovar v0.35 transcript databases
                    .custom("hg19_ensembl.ser", createUrlOrExplode("https://zenodo.org/record/5410367/files/ensembl_87_hg19.ser"))
//                    .custom("hg19_ucsc.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg19_ucsc.ser"))
//                    .custom("hg19_refseq.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg19_refseq.ser"))
                    .custom("hg19_refseq.ser", createUrlOrExplode("https://zenodo.org/record/5410367/files/refseq_105_hg19.ser"))
                    .custom("hg19_refseq_curated.ser", createUrlOrExplode("https://zenodo.org/record/5410367/files/refseq_curated_105_hg19.ser"))

                    .custom("hg38_ensembl.ser", createUrlOrExplode("https://zenodo.org/record/5410367/files/ensembl_91_hg38.ser"))
//                    .custom("hg38_ucsc.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg38_ucsc.ser"))
//                    .custom("hg38_refseq.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg38_refseq.ser"))
                    .custom("hg38_refseq.ser", createUrlOrExplode("https://zenodo.org/record/5410367/files/refseq_109_hg38.ser"))
                    .custom("hg38_refseq_curated.ser", createUrlOrExplode("https://zenodo.org/record/5410367/files/refseq_curated_109_hg38.ser"))

                    .build();
            downloader.download();
            logger.info("Done!");
            return 0;
        } catch (FileDownloadException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return 1;
        }
    }

    private static URL createUrlOrExplode(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new LiricalRuntimeException(e);
        }
    }

}
