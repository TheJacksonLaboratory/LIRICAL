.. _rstrunning:

===============
Running LIRICAL
===============

LIRICAL is a command-line Java tool that runs with Java version 17 or higher.
LIRICAL can be run both with and without genomic data in form of a VCF file from genome, exome,
or NGS gene-panel sequencing.

On typical computers, LIRICAL will run from about 15 to 60 seconds in phenotype-only mode,
~5 minutes with a typical exome file, or longer if a whole-genome file is used as input.

To get help, run LIRICAL with a command or with the option "-h"::

  lirical --help
  LIkelihood Ratio Interpretation of Clinical AbnormaLities

  Usage: lirical [-hV] [COMMAND]
    -h, --help      Show this help message and exit.
    -V, --version   Print version information and exit.
  Commands:
    download, D     Download files for LIRICAL.
    prioritize, R   Run LIRICAL from CLI arguments.
    phenopacket, P  Run LIRICAL from a Phenopacket.
    yaml, Y         Run LIRICAL from a YAML file.

  See the full documentation at https://lirical.readthedocs.io/en/master

.. note::
  We assume that the `lirical` command alias was set as described in the :ref:`rstsetupalias` section.

Run LIRICAL with a specific command with the ``-h`` option to get information about the command::

  lirical download -h
  Usage: lirical download [-hVw] [-d=<datadir>]
  Download files for LIRICAL.
    -d, --data=<datadir>   directory to download data (default: data)
    -w, --overwrite        overwrite previously downloaded files (default: false)
    -h, --help             Show this help message and exit.
    -V, --version          Print version information and exit.


LIRICAL has four main commands, ``download``, ``prioritize``, ``phenopacket``, and ``yaml``.
We will *not* discuss the ``download`` command since it has already been covered in the :ref:`rstdownload` section


Shared CLI options
^^^^^^^^^^^^^^^^^^

LIRICAL offers several commands for receiving phenotype and genotype inputs via CLI, phenopacket, or a YAML file.
However the commands share many CLI arguments for setting up the resource paths, the analysis configuration,
and where results should be written. We describe the shared CLI arguments in this section.


Resources
~~~~~~~~~

The options from this group set up resources required for LIRICAL analysis.

* ``-d | --data``: path to LIRICAL data directory.
  Required if the ``data`` folder is not set up next to the LIRICAL JAR file.
* ``-ed19 | --exomiser-hg19-dir``: path to Exomiser data directory for *hg19*.
  Required if the analysis is run with exome/genome sequencing files and ``--assembly`` is set to *hg19*.
* ``-ed38 | --exomiser-hg38-dir``: path to Exomiser data directory for *hg38*.
  Required if the analysis is run with exome/genome sequencing files and ``--assembly`` is set to *hg38*.
* ``-b | --background``: path to file with background variant frequencies for genes.
  This option should not be used unless there is a very good reason to do that.
  The background variant frequencies are bundled with the LIRICAL code. See :ref:`rstbg-var-freqs` for more info.
* ``--parallelism``: the number of workers/threads to use. The value must be a positive integer (default: ``1``).


Changes from the previous LIRICAL versions
******************************************

In previous versions, LIRICAL required the Exomiser variant database to be provided as a path to a single file,
either via ``-e38 | --exomiser-hg38`` or via ``-e19 | --exomiser-hg19`` option.
A single database file contained the allele annotations (frequencies, in silico pathogenicity, etc.)
together with ClinVar variant annotations.
However, starting from Exomiser data release ``2406``, the annotations are distributed as several files.
Exomiser release pipeline uses the following naming scheme for the files:
``2406_hg38_variants.mv.db`` for the database with allele info for GRCh38 variants of the `2406` data release,
and ``2406_hg38_clinvar.mv.db`` for the ClinVar annotations.

LIRICAL `2.1.0` adds support for the new database format. This is a breaking change and the CLI invocation
must be updated.

The simplest way to update the CLI is to use the new ``-ed38 | --exomiser-hg38-dir`` option,
that takes path to Exomiser data directory. LIRICAL will find the allele and ClinVar database files automatically.

Alternatively, paths to the database files can be provided manually:

* ``-e38 | --exomiser-hg38 | --exomiser-hg38-variants`` for the allele infos
* ``-c38 | --exomiser-hg38-clinvar`` for the ClinVar annotations

If both path to Exomiser data directory and the paths to individual database files are provided,
the paths to individual files will override the files found in Exomiser data directory.


Configuration options
~~~~~~~~~~~~~~~~~~~~~

The configuration options tweak the analysis.

* ``-g | --global``: global analysis, see :ref:`rstglobal-mode` for more info (default: ``false``).
* ``--sdwndv``: show diseases even if no deleterious variants are found in the gene associated with the disease.
  The option is a flag (takes no value) and its presence will lead to showing *all* diseases,
  even those with no deleterious variants.
  Only applicable to the HTML and TSV reports when running with a VCF file (genotype-aware mode).
  The JSON report will include *all* diseases all the time.
* ``--transcript-db``: transcript database (default: ``RefSeq``), see :ref:`rsttx-dbs` for more info.
* ``--use-orphanet``: use `Orphanet <https://www.orpha.net/consor/cgi-bin/index.php>`_ annotations (default: ``false``).
* ``--target-diseases``: limit the analysis to the provided disease IDs.
  Expecting a comma-separated list of disease IDs, such as `OMIM:614102,OMIM:619340`.
  The ``--use-orphanet`` option is ignored if at least one disease ID is provided.
  All diseases are analyzed by default.
* ``--strict``: use strict penalties if the genotype does not match the disease model
  in terms of number of called pathogenic alleles (default: ``false``).
* ``--pathogenicity-threshold``: Variants with greater pathogenicity score is considered deleterious (default: ``0.8``).
* ``--validation-policy``: set the level of input sanity check, see :ref:`rst-input-sanitation` for more info.
  Choose from `MINIMAL`, `LENIENT`, `STRICT` (default ``MINIMAL``).
* ``--dry-run``: check if the inputs meet the validation policy requirements, report any issues,
  and exit without running the analysis (default: ``false``).


Output options
~~~~~~~~~~~~~~

The output options dictate the format and location for the analysis results.

* ``-o | --output-directory``: where to write the analysis outputs (default: current working directory).
* ``-f | --output-format``: Output format to use for writing the results, can be provided multiple times.
  Choose from `html`, `tsv`, and `json` (default: ``html``).
* ``-x | --prefix``: prefix of the output files (default: ``lirical``)
* ``-t | --threshold``: minimum post-test probability to show diagnosi.s in the HTML report.
  The value must be in range :math:`[0, 1]`. The option must not be used with ``-m | -mindiff`` option at the same time.
* ``-m | --mindiff``: Minimal number of differential diagnoses to show.
* ``--display-all-variants``: Display all variants in the HTML report, not just the variants passing
  the pathogenicity threshold (default: ``false``).


LIRICAL prioritization commands
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

LIRICAL provides three commands for receiving phenotype and genotype inputs via CLI, as a phenopacket, or as a YAML file.

``prioritize`` - run LIRICAL with via CLI options
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Since `v2` release, all required inputs can be provided as command line arguments of the ``prioritize`` command.
This leads to a rather lengthy CLI. However, the CLI can be useful e.g. for using with pipeline engines such
as Nextflow or Snakemake.

The ``prioritize`` command takes the following options:

* ``-p | --observed-phenotypes``: a comma-separated IDs of HPO IDs
  that correspond to the phenotype terms observed in the proband.
* ``-n | --negated-phenotypes``: a comma-separated IDs of HPO IDs
  that correspond to the phenotype terms negated/excluded in the proband.
* ``--assembly`` genome build, choose from `hg19` or `hg38`, must be provided if ``--vcf`` is used (default: ``hg38``).
* ``--vcf``: path to VCF file with exome/genome sequencing results. The file can be compressed.
* ``--sample-id``: proband's identifier, must be provided if running with a multi-sample VCF file (default: `subject`).
* ``--age``: proband's age as an ISO8601 duration.
  (e.g. ``P9Y`` for 9 years, ``P2Y3M`` for 2 years and 3 months, or ``P33W`` for the 33th gestational week).
* ``--sex``: proband's sex, choose from `MALE`, `FEMALE`, `UNKNOWN` (default: `UNKNOWN`).


``phenopacket`` - run LIRICAL with a Phenopacket
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL can be run with clinical data (HPO terms) only or with clinical data and a VCF file representing the
results of gene panel, exome, or genome sequencing. The preferred input format is
`Phenopackets <https://phenopacket-schema.readthedocs.io/en/latest/>`_,
an open standard for sharing disease and phenotype information.
This is a new standard of the `Global Alliance for Genomics and Health <https://www.ga4gh.org/>`_ that
links detailed phenotype descriptions with disease, patient, and genetic information.


.. figure:: _static/hpo-textmining.png
    :width: 75 %
    :align: center
    :alt: PhenopacketGenerator

    For convenience, we provide a tool called
    `PhenopacketGenerator <https://github.com/TheJacksonLaboratory/PhenopacketGenerator>`_ that can be used
    to create a Phenopacket with a list of HPO terms and the path to a VCF file with which LIRICAL can be run.


LIRICAL can be run with clinical data (HPO terms) only or with clinical data and a VCF file representing the
results of gene panel, exome, or genome sequencing.

Let's consider an example of an individual with `Pfeiffer syndrome <https://omim.org/entry/101600>`_::

  {
    "id": "pfeiffer-example",
    "subject": {
      "id": "example-1"
    },
    "phenotypicFeatures": [{
      "type": {
        "id": "HP:0000244",
        "label": "Turribrachycephaly"
      }
    }, {
      "type": {
        "id": "HP:0001363",
        "label": "Craniosynostosis"
      }
    }, {
      "type": {
        "id": "HP:0000453",
        "label": "Choanal atresia"
      }
    }, {
      "type": {
        "id": "HP:0000327",
        "label": "Hypoplasia of the maxilla"
      }
    }, {
      "type": {
        "id": "HP:0000238",
        "label": "Hydrocephalus"
     }
    }],
    "metaData": {
      "createdBy": "Peter R.",
      "resources": [{
        "id": "hp",
        "name": "human phenotype ontology",
        "namespacePrefix": "HP",
        "url": "http://purl.obolibrary.org/obo/hp.owl",
        "version": "2018-03-08",
        "iriPrefix": "http://purl.obolibrary.org/obo/HP_"
      }],
      "phenopacketSchemaVersion": "2.0.0"
    }
  }

Save the file above as ``pfeiffer.json``.

**Running LIRICAL with clinical data**


LIRICAL will perform phenotype-only analysis if the ``phenopacket`` command incantation does not contain a ``--vcf`` option.
In this case, the only required argument is the phenopacket::

  lirical phenopacket -p pfeiffer.json


**Running LIRICAL with a VCF file**

Alternatively, LIRICAL can include the VCF file if the path is provided using ``--vcf`` option.
Note, we must also provide ``--assembly`` and ``-e19`` (or ``-e38``) options to indicate the genome assembly and path to Exomiser variant database::

  lirical phenopacket -p pfeiffer.json --vcf path/to/pfeiffer.vcf.gz --assembly hg19 -e19 /path/to/exomiser/2302_hg19_variants.mv.db


``yaml`` - running LIRICAL with a YAML file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The other allowed input format is :ref:`rstyaml`.

A typical command that runs LIRICAL using settings shown in the YAML file with the default data directory
would be simply::

  lirical yaml -y example.yml

This will run the phenotype-only analysis of the *Patient 4*.

To run the genotype-aware analysis, modify the YAML file such that the ``vcf`` field points to the location
of the VCF file on your file system. Then, the analysis is run as::

 lirical yaml -y example.yml --assembly hg19 -e19 /path/to/exomiser/2302_hg19_variants.mv.db


Choosing between YAML and Phenopacket input formats
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

How should users choose between :ref:`rstyamlorphenopackethpo`?
