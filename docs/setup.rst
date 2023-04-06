.. _rstsetup:

==================
Setting up LIRICAL
==================

LIRICAL is a desktop Java application that requires several external files to run. This document
details how to download these files and prepare to run LIRICAL. LIRICAL requires Exomiser to be installed
as a library before it can be compiled and built.


Prerequisites
~~~~~~~~~~~~~

LIRICAL was written with Java version 17 but will compile under Java 17 or better. If you want to
build LIRICAL from source, then the build process described below requires
`Git <https://git-scm.com/book/en/v2>`_ and
`Java Development Kit 17 <https://www.oracle.com/java/technologies/downloads/>`_ or better.

.. note::
  The v1 of LIRICAL was written in Java 8 but starting from v2 we require Java 17 or better to take advantage
  of numerous performance improvements and novel language features.

Building from sources
~~~~~~~~~~~~~~~~~~~~~

Go the GitHub page of `LIRICAL <https://github.com/TheJacksonLaboratory/LIRICAL>`_, and clone or download the project.
Build the executable from source with Maven::

  git clone https://github.com/TheJacksonLaboratory/LIRICAL.git
  cd LIRICAL
  ./mvnw -Prelease package

We use the amazing `Maven Wrapper <https://maven.apache.org/wrapper/>`_ for building the sources, so installation
of Maven prior build is *not* required.

Verify that the building process went well by running:

.. parsed-literal::
  java -jar lirical-cli/target/lirical-cli-|release|.jar --help


Prebuilt LIRICAL executable
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Alternatively, go to the `Releases section <https://github.com/TheJacksonLaboratory/LIRICAL/releases>`_ on the
LIRICAL GitHub page and download the distribution ZIP file with an executable JAR file with the latest LIRICAL version.

Assuming the distribution file was downloaded to lirical-cli-|release|-distribution.zip,
the following will print the help message:

.. parsed-literal::
  unzip lirical-cli-|release|-distribution.zip
  java -jar lirical-cli-|release|/lirical-cli-|release|.jar --help

.. _rstsetupalias:

Set up alias
^^^^^^^^^^^^

In general, Java command line applications are invoked as ``java -jar executable.jar``. However, such incantation is
a bit too verbose and we can shorten it a bit by defining an alias.

Let's define a command alias for LIRICAL. Assuming the distribution ZIP was unpacked into
phenopacket-tools-cli-|release| directory, run the following to set up and check the command alias:

.. parsed-literal::
  alias lirical="java -jar $(pwd)/lirical-cli-|release|/lirical-cli-|release|.jar"
  lirical --help

.. note::
  From now on, we will use the ``lirical`` alias instead of the longer form.


.. _rstexomiserdatadir:

Exomiser database files
~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL uses data files from the `Exomiser <https://github.com/exomiser/Exomiser>`_
to annotate the VCF file and support variant interpretation.
We recommend that always the latest version of these files be used.
The data files are stored at the
`Exomiser download site <https://exomiser.monarchinitiative.org/exomiser/download>`_.
You may need to scroll (right hand side) to see the subdirectory ``latest``, which includes the current version of
these files. Download either ``2302_hg19.zip`` (for the hg19/GRCh37 genome assembly)  or ``2302_hg38.zip `` for the
hg38/GRCh38 assembly). Of course, the datafile you use should match the assembly used to align and call
the exome/genome data you want to analyze with LIRICAL. Unpack the ZIP file, e.g.,::

  unzip 2302_hg19.zip

LIRICAL uses the variant database file that is present in the data folder. The database file is named as
``<release>_<genome-build>_variants.mv.db``, where

* ``release`` is the release identifier (e.g. `2302`)
* ``genome-build`` is the identifier of the genome build (e.g. `hg38`)

Remember the path, since it will be needed to run LIRICAL with exome/genome data. We will use the CLI options:

* ``-e19 | --exomiser-hg19`` `/some/path/2302_hg19_variants.mv.db`, or
* ``-e38 | --exomiser-hg38`` `/some/path/2302_hg19_variants.mv.db`

to set path to the Exomiser variant database.

.. note::
  The ``-e`` option that was used to point to Exomiser data directory has been deprecated
  and will *not* work starting from LIRICAL v2.


.. _rstdownload:

LIRICAL data files
~~~~~~~~~~~~~~~~~~

LIRICAL requires some additional files to run.

1. ``hp.json``. The main Human Phenotype Ontology file
2. ``phenotype.hpoa`` The main annotation file with all HPO disease models
3. ``hgnc_complete_set.txt`` A text file from HUGO Gene Nomenclature Committee (HGNC) with information about human genes
4. ``mim2gene_medgen`` A file from the NCBI medgen project with OMIM-derived links between genes and diseases
5. Jannovar transcript annotation files with definitions of transcripts and genes:
  * ``hg19_refseq.ser``
  * ``hg19_ucsc.ser``
  * ``hg38_refseq.ser``
  * ``hg38_ucsc.ser``

LIRICAL offers a convenience function to download all files to a local directory::

By default, LIRICAL will download all files into a newly created subdirectory called ``data``
in the current working directory. You can change this default with the ``-d`` or ``--data`` options
(If you change this, then you will need to pass the location of your directory to all other LIRICAL commands
using the ``-d`` flag).

Download the files into the ``data`` folder by running::

  lirical download

.. note::
  We assume the LIRICAL alias was set as described in the :ref:`rstsetupalias` section.

LIRICAL will not download the files if they are already present unless the ``--overwrite`` argument is passed. For
instance, the following command would download the four files to a directory called ``datafiles`` and would
overwrite any previously downloaded files::

  lirical download -d datafiles --overwrite

If desired, you can download these files on your own but you need to place them all in the
same directory to run LIRICAL.
