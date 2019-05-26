Setting up LIRICAL
==================

LIRICAL is a desktop Java application that requires several external files to run. This document
details how to download these files and prepare to run LIRICAL.



Installation
~~~~~~~~~~~~

Go the GitHub page of `LIRICAL <https://github.com/TheJacksonLaboratory/LIRICAL>`_, and clone or download the project.
Build the executable from source with maven, and then test the build. ::

    $ git clone https://github.com/TheJacksonLaboratory/LIRICAL.git
    $ cd LIRICAL
    $ mvn package
    $ java -jar target/LIRICAL.jar
    $ Usage: <main class> [options] [command] [command options]
      Options:
        -h, --help
          display this help message
      (...)



LIRICAL requires `maven <https://maven.apache.org/>`_ version 3.5.3. Alternatively, go to the Releases section of the
`LIRICAL <https://github.com/TheJacksonLaboratory/LIRICAL>`_, GitHub page and download the latest precompiled version
of LIRICAL.



Exomiser as a Java library
~~~~~~~~~~~~~~~~~~~~~~~~~~
LIRICAL relies on some classes from the Exomiser. To build LIRICAL, we need to install the Exomiser code base locally.
Note that the version of Exomiser must match the version indicated in LIRICAL's pom file (current 12.0). To check this,
search for the following line in the ``pom.xml`` file: ::

     <exomiser.version>12.0.0</exomiser.version>





To do so, we clone the code and install using the maven system. ::

    $ git clone https://github.com/exomiser/Exomiser.git
    $ cd Exomiser

Now, we ensure that we are using the correct branch of Exomiser. ::

    $ git checkout release-12.0.0
        Switched to branch 'release-12.0.0'
        Your branch is up to date with 'origin/release-12.0.0'.
    $ git branch
        development
        master
        * release-12.0.0

Finally, we use the maven system to install the Exomiser library locally so that it can be used by LIRICAL. ::

    $ mvn install

This command will install the library in the ``.m2`` directory located in your home directory. If you like, explore
``.m2/repository/org/monarchinitiative/exomiser/`` to see how maven structures the repository.


Exomiser database files
~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL uses data files from the Exomiser. We recommend that always the latest version of these files be used. The
data files are stored at the `Exomiser download site <https://monarch-exomiser-web-dev.monarchinitiative.org/exomiser/download>`_.
You may need to scroll (right hand side) to see the subdirectory ``latest``, which includes the current version of
these files. Download either ``1811_hg19.zip`` (for the hg19/GRCh37 genome assembly)  or ``1811_hg38.zip`` for the
hg38/GRCh38 assembly). Of course, the datafile you use should match the assembly used to align and call
the exome/genome data you want to analyze with LIRICAL.  Unpack the file, e.g., ::

    $ unzip 1811_hg19.zip

Remember the path, since it will be needed to run LIRICAL with exome/genome data. We will use the argument: ::

    -e /some/path/1811_hg19

where ``1811_hg19`` is the directory that is created by unpacking the archive file. The directory should contain the
files

* 1811_hg19_genome.h2.db
* 1811_hg19_transcripts_ensembl.ser
* 1811_hg19_transcripts_refseq.ser
* 1811_hg19_transcripts_ucsc.ser
* 1811_hg19_variants.mv.db

These files are used by LIRICAL to annotate the VCF file and support variant interpretation.





The download command
~~~~~~~~~~~~~~~~~~~~

.. _rstdownload:

LIRICAL requires four additional files to run. It offers a convenience function to download all four files
to a local directory.


1. ``hp.obo``. The main Human Phenotype Ontology file
2. ``phenotype.hpoa`` The main annotation file with all HPO disease models
3. ``Homo_sapiens_gene_info.gz`` A file from NCBI Entrez Gene with information about human genes
4. ``mim2gene_medgen`` A file from the NCBI medgen project with OMIM-derived links between genes and diseases

By default, LIRICAL will download all four files into a newly created subdirectory called ``data`` in the
current working directory. You can change this default with the ``-d`` or ``--data`` options (If you change
this, then you will need to pass the location of your directory to all other LIRICAL commands
using the ``-d`` flag). Download the
files automatically as follows. ::

    $ java -jar LIRICAL.jar download

LIRICAL will not download the files if they are already present unless the ``--overwrite`` argument is passed. For
instance, the following command would download the four files to a directory called datafiles and would
overwrite any previously downloaded files. ::

    $ java -jar LIRICAL.jar download -d datafiles --overwrite


If desired, you can download these files on your own but you need to place them all in the
same directory to run LIRICAL.

