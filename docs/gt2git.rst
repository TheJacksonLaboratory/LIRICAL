GT2GIT
######

This module is used to calculate the background frequency of predicted pathogenic mutations in genes. It
creates a file called ``background-frequency-hg19.txt`` or ``background-frequency-hg38.txt``. We have put
these files into the src/main/resources/background folder so that end users do not need to regenerate the
files themselves. However, we leave the source code in the project for those who desire to reproduce or
extend the findings in the manuscript. The main class that implements this is
GenicIntoleranceCalculator. The same class can also be used to extract ClinVar pathogenicity scores (see :ref:`ClinVar <clinvar>`).
The command is hidden in the default command line interface, but can be seen with the following. ::

    $ java -jar target/Lr2pg.jar gt2git -h


gt2git
~~~~~~

To run the gt2git command, download the desired Exomiser data resource from the Exomiser FTP site
(https://data.monarchinitiative.org/exomiser/latest/).  For instance, to do the analysis with the hg38
genome assembly (Exomiser version 1811), download the data file ``1811_hg38.zip``  and unzip it. The Exomiser will first calculate
the expected background frequency of predicted pathogenic variants and write this to a file that will
be used in subsequent steps (this will take about an hour on a typical laptop). ::

    $ java -jar target/Lr2pg.jar gt2git -e <exomiser-data-path> -g <genome>


In this command, ``exomiser-data-path`` refers to the path of the Exomiser data directory, e.g., ``1802_hg19``,
and ``genome`` refers to the genome build. Use the  genome build that corresponds to the Exomiser
genome build, ``hg19`` or ``hg38``.


