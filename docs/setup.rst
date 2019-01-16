Setting up LR2PG
================

LR2PG is a desktop Java 1.8 application that requires several external files to run. This document
details how to download these files and prepare to run LR2PG.

The download command
~~~~~~~~~~~~~~~~~~~~

.. _lr2pgdownload:

LR2PG makes use of a data directory to store four required files.

1. ``hp.obo``. The main Human Phenotype Ontology file
2. ``phenotype.hpoa`` The main annotation file with all HPO disease models
3. ``Homo_sapiens_gene_info.gz`` A file from NCBI Entrez Gene with information about human genes
4. ``mim2gene_medgen`` A file from the NCBI medgen project with OMIM-derived links between genes and diseases

By default, LR2PG will download all four files into a newly created subdirectory called ``data`` in the
current working directory. You can change this default with the ``-d`` or ``--data`` options (If you change
this, then you will need to pass the location of your directory to all other LR2PG commands
using the ``-d`` flag). Download the
files automatically as follows. ::

    $ java -jar Lr2pg.jar download

LR2PG will not download the files if they are already present unless the ``--overwrite`` argument is passed. For
instance, the following command would download the four files to a directory called datafiles and would
overwrite any previously downloaded files. ::

    $ java -jar Lr2pg.jar download -d datafiles --overwrite


If desired, you can download these files on your own but you need to place them all in the
same directory to run LR2PG.


Exomiser data
~~~~~~~~~~~~~

.. _exomiserdata:

LR2PG makes use of the Exomiser data resources, which need to be downloaded from the Exomiser FTP site
(https://data.monarchinitiative.org/exomiser/latest/).  For instance, to do the analysis with the hg38
genome assembly, download the data file 1805_hg38.zip  and unzip it. The
`Exomiser <https://monarch-exomiser-web-dev.monarchinitiative.org/exomiser/>`_ is a previous algorithm
from our group that can be used to perform a range of genomic diagnostic and discovery tasks
(Todo cite the papers here). The Exomiser makes use of an internal database called here mvstore, e.g.,
``1802_hg19_variants.mv.db``, which is required for every run of the VCF analysis tool. Some of the
other files are required for users who desire to reproduce or extend some of the results in the
LR2PG manuscript and are described in the section TODO make link.

