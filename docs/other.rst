Other commands
==============

The LIRICAL code base contains functionality that is not needed to run LIRICAL for prioritizing candidates of the
differential diagnosis, but which we used during the development of the algorithm. We expect that the great majority of
users will not need or want to use these functions, but we have left the code in the
code base for bioinformaticians and other users who may wish to reproduce or extend our results.

We have hidden the commands in the default command line interface, but detailed help can be obtained
for any command on this page by passing LIRICAL the command name and the ``-h`` or ``--help`` option.


Simulate
~~~~~~~~

This command runs one simulation with one particular set of parameters. It is related to the
grid command which runs 100 different combinations.

This command has the following options. ::

     Options:
          -d, --data
            directory to download data (default: data)
            Default: data
          -i, --imprecion
            Use imprecision?
            Default: false
          -c, --n_cases
            Number of cases to simulate
            Default: 25
          -h, --n_hpos
            Number of HPO terms per case
            Default: 5
          -n, --n_noise
            Number of noise terms per case
            Default: 1


The command is run as ::

    $ java -jar Lr2pg.jar simulate [options]


It will print a summary of the results to standard out, for instance, ::

    Simulation of 25 cases with 5 HPO terms, 1 noise terms. Imprecision: false
    Rank=1: count:19 (76.0%)
    Rank=2: count:2 (8.0%)
    Rank=3: count:1 (4.0%)
    Rank=8: count:1 (4.0%)
    Rank=11-20: count:0 (0.0%)
    Rank=21-30: count:0 (0.0%)
    Rank=31-100: count:1 (4.0%)
    Rank=101-...: count:1 (4.0%)


The grid command
~~~~~~~~~~~~~~~~

We developed code to perform simulations that we used to produce some of the results in the manuscript. This
code is not needed to run LR2PG, but is left here for users who may wish to reproduce or extend the simulation
results.

To see the options available for this command, enter ::

    $ java -jar LIRICAL.jar grid -h




This command runs simulations with different combinations of parameters.
It varies the number of HPO terms per simulated case from 1 to 10. For
each term number, it varies the number of random (unrelated) terms from
1 to 4. It runs the entire simulation with and without imprecision (imprecision
means that we replace each non-noise HPO term with its parent, i.e., with
a less precise term).

The code will provide a running output of its work to standard OUT,  and will take
up to several hours to run depending on the hardware. It will output a datafile
(called e.g., ``grid_25_cases_precise.txt`` for a simulation with 25 cases for each
run using precision) that can be used to generate a Figure that summarizes
all of the results.  ::

    n.terms	n.random	rank1
    0	0	0.00
    0	1	0.00
    0	2	20.00
    0	3	20.00
    0	4	20.00
    1	0	40.00
    1	1	60.00
    (...)


For the paper, we should run 100 cases for each run (at least).


R script for the grid command
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To create a graphic, let's extend the following R script (Note the script needs to be adjusted, see
http://www.sthda.com/english/wiki/impressive-package-for-3d-and-4d-graph-r-software-and-data-visualization). ::

    dat <- read.table("grid_2_cases_precise.txt",header=TRUE)
    mat<-data.matrix(dat)
    hist3D(z = mat, scale = FALSE, expand = 0.01, bty = "g", phi = 20,
        col = "#0072B2", border = "black", shade = 0.2, ltheta = 90,
        space = 0.3, ticktype = "detailed", d = 2)

gt2git
~~~~~~

This module is used to calculate the background frequency of predicted pathogenic mutations in genes. It
creates a file called ``background-frequency-hg19.txt`` or ``background-frequency-hg38.txt``. We have put
these files into the src/main/resources/background folder so that end users do not need to regenerate the
files themselves. However, we leave the source code in the project for those who desire to reproduce or
extend the findings in the manuscript. The main class that implements this is
GenicIntoleranceCalculator. The same class can also be used to extract ClinVar pathogenicity scores (see :ref:`ClinVar <clinvar>`).
The command is hidden in the default command line interface, but can be seen with the following. ::

    $ java -jar target/Lr2pg.jar gt2git -h




To run the gt2git command, download the desired Exomiser data resource from the Exomiser FTP site
(https://data.monarchinitiative.org/exomiser/latest/).  For instance, to do the analysis with the hg38
genome assembly (Exomiser version 1811), download the data file ``1811_hg38.zip``  and unzip it. The Exomiser will first calculate
the expected background frequency of predicted pathogenic variants and write this to a file that will
be used in subsequent steps (this will take about an hour on a typical laptop). ::

    $ java -jar target/Lr2pg.jar gt2git -e <exomiser-data-path> -g <genome>


In this command, ``exomiser-data-path`` refers to the path of the Exomiser data directory, e.g., ``1802_hg19``,
and ``genome`` refers to the genome build. Use the  genome build that corresponds to the Exomiser
genome build, ``hg19`` or ``hg38``.


Generating the ClinVar score distribution
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. _clinvar:

This step is only needed for those who desire to reproduce or extend the data in the original publication,
and it is not needed to run LR2PG.


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

.