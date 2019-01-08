Generating the ClinVar score distribution
=========================================

This step is only needed for those who desire to reproduce or extend the data in the original publication,
and it is not needed to run LR2PG.

Purpose
~~~~~~~
LR2PG uses a cutoff Exomiser pathogenicity score of 0.8, which offers a good, if imperfect,
separation between variants called pathogenic by ClinVar and those that are not so called.

To get the exomiser pathogenicity scores for all ClinVar variants, we can run LR2PG as follows. ::

    $ java -jar target/Lr2pg.jar gt2git \
        -j /home/peter/data/exomiser/1802_hg19/1802_hg19_transcripts_refseq.ser \
        --mvstore /home/peter/data/exomiser/1802_hg19/1802_hg19_variants.mv.db \
        -g hg19 \
        --clinvar


The program will output a file with two columns (it runs in about one hour on a good desktop). ::

    1.0     PATHOGENIC
    0.0     BENIGN
    0.963   LIKELY_BENIGN
    0.742   BENIGN_OR_LIKELY_BENIGN
    0.0     BENIGN
    1.0     PATHOGENIC
    0.0     BENIGN_OR_LIKELY_BENIGN
    (...)


The first column is the Exomiser pathogenicity score and the second column is the ClinVar interpretation.
There is thus one line for each variant in ClinVar that has a PATHOGENIC or a BENIGN classification.
We can plot this data with the following R script. Note that we combine the three
subcategories PATHOGENIC, LIKELY_PATHOGENIC, PATHOGENIC_OR_LIKELY_PATHOGENIC into a
single category (``pathogenic``) and likewise for BENIGN.

The following R script can be used to plot the data (change the current working directory to the
directory with the outputfile, ``clinvarpath.txt``, which by default is the same directory
in which the program is executed). ::

    library(ggplot2)
    library(ggsci)
    dat <- read.table("clinvarpath.txt",header=FALSE)
    colnames(dat) <- c("path","category")
    dat$cat<- ifelse((dat$category=='BENIGN' | dat$category=='LIKELY_BENIGN' | dat$category=='BENIGN_OR_LIKELY_BENIGN'),"benign","pathogenic")

    p2 = ggplot(dat, aes(x=path, fill=cat)) +
    geom_histogram(colour = "black",  position = "dodge",binwidth=0.01) +
    #geom_histogram(aes(y = ..count..), binwidth = 0.2,   position="identity", alpha=0.9) +
    #scale_x_continuous(name = "Pathogenicity",
                   #  breaks = seq(-3, 3, 1),
                    # limits=c(-3.5, 3.5)) +
    theme_bw() + theme(text = element_text(size=20),
                     axis.text = element_text(size=20, hjust=0.5),
                     axis.title = element_text(size=20),
                     legend.title = element_blank(),
                     legend.position="top")
    p2_npg = p2 + scale_fill_npg()
    p2_npg

TODO--revise this script. Maybe this does not need to remain in the documentation.