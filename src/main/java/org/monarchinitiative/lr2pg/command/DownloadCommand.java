package org.monarchinitiative.lr2pg.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.io.FileDownloadException;
import org.monarchinitiative.lr2pg.io.FileDownloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Command to download the {@code hp.obo} and {@code phenotype.hpoa} files that
 * we will need to run the LR2PG approach.
 */
public class DownloadCommand implements Command {
    private static final Logger logger = LogManager.getLogger();
    /** Directory to which we will download the files. */
    private final String downloadDirectory;
    /** URL of the hp.obo file. */
    private final static String HP_OBO="https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.obo";
    /** URL of the annotation file phenotype.hpoa. */
    private final static String HP_ANNOTATION="http://compbio.charite.de/jenkins/job/hpo.annotations.2018/lastSuccessfulBuild/artifact/misc_2018/phenotype.hpoa";
    /** Basename of the phenotype annotation file. */
    private final static String HP_ANNOTATION_FILE="phenotype.hpoa";

    /**
     * Download all three files that we need for the analysis.
     * @param path path to directory to which to download the files.
     */
    public DownloadCommand(String path){
        this.downloadDirectory=path;
    }


    /**
     * Download the hp.obo and the phenotype_annotation.tab files.
     */
    public void execute() {
        downloadHpOntologyIfNeeded();
        downloadHpPhenotypeAnnotationFileIfNeeded();
    }

    private void downloadHpOntologyIfNeeded() {
        File f = new File(String.format("%s%shp.obo",downloadDirectory,File.separator));
        if (f.exists()) {
            logger.trace(String.format("Cowardly refusing to download hp.obo since we found it at %s",f.getAbsolutePath()));
            return;
        }
        FileDownloader downloader=new FileDownloader();
        try {
            URL url = new URL(HP_OBO);
            logger.debug("Created url from "+HP_OBO+": "+url.toString());
            downloader.copyURLToFile(url, new File(f.getAbsolutePath()));
        } catch (MalformedURLException e) {
            logger.error("Malformed URL for hp.obo");
            logger.error(e,e);
        } catch (FileDownloadException e) {
            logger.error("Error downloading hp.obo");
            logger.error(e,e);
        }
        logger.trace(String.format("Successfully downloaded hp.obo file at %s",f.getAbsolutePath()));
    }



    private void  downloadHpPhenotypeAnnotationFileIfNeeded() {
        File f = new File(String.format("%s%s%s",downloadDirectory,File.separator,HP_ANNOTATION_FILE));
        if (f.exists()) {
            logger.trace(String.format("Cowardly refusing to download %s since we found it at %s",
                    HP_ANNOTATION_FILE,
                    f.getAbsolutePath()));
            return;
        }
        FileDownloader downloader=new FileDownloader();
        try {
            URL url = new URL(HP_ANNOTATION);
            logger.debug("Created url from "+HP_ANNOTATION+": "+url.toString());
            downloader.copyURLToFile(url, new File(f.getAbsolutePath()));
        } catch (MalformedURLException e) {
            logger.error("Malformed URL for {}",HP_ANNOTATION_FILE);
            logger.error(e,e);
        } catch (FileDownloadException e) {
            logger.error("Error downloading {}",HP_ANNOTATION_FILE);
            logger.error(e,e);
        }
        logger.trace(String.format("Successfully downloaded HPO annotation file at %s",f.getAbsolutePath()));
    }


}
