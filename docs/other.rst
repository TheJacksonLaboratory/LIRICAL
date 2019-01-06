Running other LR2PG commands
============================

We developed code to perform simulations that we used to produce some of the results in the manuscript. This
code is not needed to run LR2PG, but is left here for users who may wish to reproduce or extend the simulation
results.


Explain simulation














gt2git
~~~~~~



LR2PG makes use of the Exomiser data resources, which need to be downloaded from the Exomiser FTP site
(https://data.monarchinitiative.org/exomiser/latest/).  For instance, to do the analysis with the hg38
genome assembly, download the data file 1805_hg38.zip  and unzip it. The Exomiser will first calculate
the expected background frequency of predicted pathogenic variants and write this to a file that will
be used in subsequent steps (this will take about an hour on a typical laptop). ::

    $ java -jar target/Lr2pg.jar gt2git -m <mvstore> -j <jannovar> -g <genome>


In this command, ``mvstore`` refers to the path of the Exomiser data store, e.g., ``1802_hg19_variants.mv.db``;
``jannovar`` refers to the path of the Jannovar transcript data file, e.g., ``1802_hg19_transcripts_refseq.ser``;
and ``genome`` refers to the genome build. Use the corresponding genome build, ``hg19`` or ``hg38``.

This command will output the background frequency file to the data direcotry (by default, a subdirectory call ``data`` in the
current working direcgtory; the data directory can also be specified with the ``-d`` flag). THe location of this file must be
specified in the YAML configuration file to run the prioritization function.

For the final release, we will add the background files for hg19 and hg38 to the distribution, but let's finish testing
prior to doing that.
