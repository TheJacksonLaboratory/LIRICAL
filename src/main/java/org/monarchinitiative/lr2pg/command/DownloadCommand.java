package org.monarchinitiative.lr2pg.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.io.FileDownloadException;
import org.monarchinitiative.lr2pg.io.FileDownloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadCommand extends Command {
    private static final Logger logger = LogManager.getLogger();

    private String downloadDirectory=null;



    private final static String HP_OBO="http://purl.obolibrary.org/obo/hp.obo";

    private final static String HP_ANNOTATION="http://compbio.charite.de/jenkins/job/hpo.annotations/lastStableBuild/artifact/misc/phenotype_annotation.tab";



    /**
     * Download all three files that we need for the analysis.
     * @param path
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
        File f = new File(String.format("%s%sphenotype_annotation.tab",downloadDirectory,File.separator));
        if (f.exists()) {
            logger.trace(String.format("Cowardly refusing to download phenotype_annotation.tab since we found it at %s",f.getAbsolutePath()));
            return;
        }
        FileDownloader downloader=new FileDownloader();
        try {
            URL url = new URL(HP_ANNOTATION);
            logger.debug("Created url from "+HP_ANNOTATION+": "+url.toString());
            downloader.copyURLToFile(url, new File(f.getAbsolutePath()));
        } catch (MalformedURLException e) {
            logger.error("Malformed URL for phenotype_annotation.tab");
            logger.error(e,e);
        } catch (FileDownloadException e) {
            logger.error("Error downloading phenotype_annotation.tab");
            logger.error(e,e);
        }
        logger.trace(String.format("Successfully downloaded HPO annotation file at %s",f.getAbsolutePath()));
    }


}
