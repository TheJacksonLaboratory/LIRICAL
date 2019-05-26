package org.monarchinitiative.lirical.io;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Command to download the {@code hp.obo} and {@code phenotype.hpoa} files that
 * we will need to run the LIRICAL approach.
 */
public class HpoDownloader {
    private static final Logger logger = LoggerFactory.getLogger(HpoDownloader.class);
    /** Directory to which we will download the files. */
    private final String downloadDirectory;
    /** If true, download new version whether or not the file is already present. */
    private final boolean overwrite;

    private final static String HP_OBO = "hp.obo";
    /** URL of the hp.obo file. */
    private final static String HP_OBO_URL ="https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.obo";
    /** URL of the annotation file phenotype.hpoa. */
    private final static String HP_ANNOTATION_URL ="http://compbio.charite.de/jenkins/job/hpo.annotations.2018/lastSuccessfulBuild/artifact/misc_2018/phenotype.hpoa";
    /** Basename of the phenotype annotation file. */
    private final static String HP_ANNOTATION ="phenotype.hpoa";

    private final static String MIM2GENE_MEDGEN = "mim2gene_medgen";

    private final static String MIM2GENE_MEDGEN_URL = "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/mim2gene_medgen";

    private final static String GENE_INFO = "Homo_sapiens_gene_info.gz";

    private final static String GENE_INFO_URL = "ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz";



    public HpoDownloader(String path){
        this(path,false);
    }

    public HpoDownloader(String path, boolean overwrite){
        this.downloadDirectory=path;
        this.overwrite=overwrite;
        logger.error("overwrite="+overwrite);
    }

    /**
     * Download the files unless they are already present.
     */
    public void download() {
        downloadFileIfNeeded(HP_OBO,HP_OBO_URL);
        downloadFileIfNeeded(HP_ANNOTATION,HP_ANNOTATION_URL);
        downloadFileIfNeeded(GENE_INFO,GENE_INFO_URL);
        downloadFileIfNeeded(MIM2GENE_MEDGEN,MIM2GENE_MEDGEN_URL);
    }







    private void downloadFileIfNeeded(String filename, String webAddress) {
        File f = new File(String.format("%s%s%s",downloadDirectory,File.separator,filename));
        if (f.exists() && (! overwrite)) {
            logger.trace(String.format("Cowardly refusing to download %s since we found it at %s",
                    filename,
                    f.getAbsolutePath()));
            return;
        }
        FileDownloader downloader=new FileDownloader();
        try {
            URL url = new URL(webAddress);
            logger.debug("Created url from "+webAddress+": "+url.toString());
            downloader.copyURLToFile(url, new File(f.getAbsolutePath()));
        } catch (MalformedURLException e) {
            logger.error(String.format("Malformed URL for %s [%s]",filename, webAddress));
            logger.error(e.getMessage());
        } catch (FileDownloadException e) {
            logger.error(String.format("Error downloading %s from %s" ,filename, webAddress));
            logger.error(e.getMessage());
        }
    }





}
