.. _rstadvanced:

LIRICAL Advanced Options
========================

Most users will not need these commands, which are hidden from the normal user menu. The LIRICAL code base contains
functionalities that we used to develop and validate the program, and we describe them here briefly.


Generating the background files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL uses the files ``src/main/resources/background/background-hg19.tsv`` and
``src/main/resources/background/background-hg38.tsv`` to estimate the expected population
frequencies of predicted pathogenic variants. The important classes as BackgroundFrequencyCommand.java
and GenicIntoleranceCalculator.java. You do not need to generate the files yourself to run Exomiser (they are included
in the resource files). The following command generates the files. ::

    java -jar target/LIRICAL.jar background -e /path/to/exomiser/1811_hg19 -g hg19


