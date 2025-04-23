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
  of the novel Java features.


.. _prebuilt-lirical-executable:

Prebuilt LIRICAL executable
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Most users should use the prebuilt LIRICAL executable.
LIRICAL is distributed as a ZIP archive that contains the executable JAR file, the libraries,
and a few files for running the examples. The archive is available from the
`Releases section <https://github.com/TheJacksonLaboratory/LIRICAL/releases>`_
on the GitHub repository.

Assuming the distribution file was downloaded to lirical-cli-|release|-distribution.zip,
the following will print the help message:

.. parsed-literal::
  unzip lirical-cli-|release|-distribution.zip
  java -jar lirical-cli-|release|/lirical-cli-|release|.jar --help


.. _build-from-sources:

Building from sources
~~~~~~~~~~~~~~~~~~~~~

Alternatively, LIRICAL can also be built from sources. Assuming JDK 17 or better is available in the environment,
the build is as simple as running::

  git clone https://github.com/TheJacksonLaboratory/LIRICAL.git
  cd LIRICAL
  ./mvnw -Prelease package

The code first clones LIRICAL repository and then runs the amazing `Maven Wrapper <https://maven.apache.org/wrapper/>`_
for building the application, so prior installation of Maven is *not* required.

During the build, Maven compiles the LIRICAL code into an executable Java archive (JAR) file and assembles the entire
application into the distribution ZIP archive. The archive is stored at `lirical-cli/target` subdirectory and it is the
same archive that you can obtain from Github releases.

.. note::

  Building from sources is not necessary for a typical LIRICAL use. Use the :ref:`prebuilt-lirical-executable`
  unless you really really need to build the app yourself.


.. _build-as-library:

Building to use as a library
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In some rare cases, LIRICAL code needs to be available as a library to another applications. To make LIRICAL ready
for use as a library, we need to run::

  git clone https://github.com/TheJacksonLaboratory/LIRICAL.git
  cd LIRICAL
  ./mvnw -Prelease install

This is almost the same as building from sources done in the previous section. However, running `install`
instead of `package` will copy the LIRICAL JARs into the local Maven repository, making it available
for other applications.

.. note::

  Building to use as a library is not necessary for a typical LIRICAL use. Use the :ref:`prebuilt-lirical-executable`
  unless you really, really, really need to use LIRICAL as a library.


.. _rstsetupalias:

Set up alias
~~~~~~~~~~~~

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

The Exomiser data files can be obtained from the
`Data release <https://github.com/exomiser/Exomiser/discussions/categories/data-release>`_ section of Exomiser discussions.
We recommend that always the latest version of these files be used.

.. note::

    Starting from LIRICAL `v2.1.0`, only the database files compatible with Exomiser `14.0.0` (or newer) are supported.
    These include the `2406 data release <https://github.com/exomiser/Exomiser/discussions/562>`_
    or newer.

Download either ``2406_hg19.zip`` (for the hg19/GRCh37 genome assembly)  or ``2406_hg38.zip`` for the
hg38/GRCh38 assembly) and unpack the archive(s).

LIRICAL needs the ``2406_hg38_variants.mv.db`` and ``2406_hg38_clinvar.mv.db`` database files to extract
allele frequencies, in silico pathogenicity predictions, and ClinVar pathogenicity metadata for variants.


.. _rstdownload:

LIRICAL data files
~~~~~~~~~~~~~~~~~~

LIRICAL requires some additional files to run.

1. ``hp.json``. The main Human Phenotype Ontology file
2. ``phenotype.hpoa`` The main annotation file with all HPO disease models
3. ``hgnc_complete_set.txt`` A text file from HUGO Gene Nomenclature Committee (HGNC) with information about human genes
4. ``mim2gene_medgen`` A file from the NCBI medgen project with OMIM-derived links between genes and diseases
5. ``en_product6.xml`` A file with links between Orpha disease IDs and the genes
6. Jannovar transcript annotation files with definitions of transcripts and genes:

  * ``hg19_ensembl.ser``
  * ``hg19_refseq.ser``
  * ``hg19_refseq_curated.ser``
  * ``hg19_ucsc.ser``
  * ``hg38_ensembl.ser``
  * ``hg38_refseq.ser``
  * ``hg38_refseq_curated.ser``
  * ``hg38_ucsc.ser``


LIRICAL offers a convenience function to download all files to a local directory.
By default, LIRICAL will download all files into a newly created subdirectory called ``data``
in the current working directory. You can change this default with the ``-d | --data`` CLI option
(If you change this, then you will need to pass the location of your directory to all other LIRICAL commands).

Download the files into the ``data`` folder located next to the LIRICAL JAR file by running:

.. parsed-literal::
  cd lirical-cli-|release|
  lirical download

This will ensure LIRICAL finds the data folder automatically (see below).

.. note::
  We assume the LIRICAL alias was set as described in the :ref:`rstsetupalias` section.

LIRICAL will not download the files if they are already present unless the ``--overwrite`` argument is passed. For
instance, the following command would download all files to a directory called ``datafiles`` and would
overwrite any previously downloaded files::

  lirical download -d datafiles --overwrite

If desired, you can download these files on your own but you need to place them all in the
same directory and provide the path to the directory using the `-d | --data` option.

The path to the LIRICAL data directory can be provided in two ways:

1. using ``-d | --data`` CLI option (explicitly)
2. using the ``data`` folder is located next to the LIRICAL JAR file (implicitly)

Using ``-d | --data`` option will override using the ``data`` folder located next to the LIRICAL JAR file.
