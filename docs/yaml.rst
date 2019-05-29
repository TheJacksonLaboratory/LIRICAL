Running LIRICAL VCF analysis with a YAML file
=============================================

.. _rstyaml:


The recommended input format for running LIRICAL is the `Phenopacket <https://github.com/phenopackets>`_, but
LIRICAL also supports `YAML <https://en.wikipedia.org/wiki/YAML>`_, which is a simple, human readable format that
is commonly used for configuration files.



YAML
----
Before running LIRICAL, download and built it and set it up according to the instructions on the :ref:`rstsetup` page.
LIRICAL uses default values for many configuration options (see below), and so the simplest possible YAML configuration
file must include the following information. ::

    ## LIRICAL Analysis Template.
    # These are all the possible options for running LIRICAL. Use this as a template for
    # your own set-up.
    ---
    analysis:
    # hg19 or hg38 is supported
    # make sure the Jannovar transcript file is taken from the same genome build
        genomeAssembly: hg19
        vcf: /path/to/example.vcf
        exomiser: /path/to/1811_hg19/
    hpoIds: ['HP:0001156', 'HP:0001363', 'HP:0011304', 'HP:0010055']
    prefix: example


This file can be found at ``src/test/resources/yaml/example1.yaml``. You can use it or one of the other examples as
a starting point for your own configuration file.



In YAML, lines that begin with ``#`` are comments, and the three dashes
indicate the start of the contents of the file. The ``analysis`` element is used to hold a dictionary with options for
running the program. It is required to indicate the genome assembly (usually hg19/GRCh37 or hg38/GRCh38)





The items in ``analysis`` all refer to the paths of files required to run LR2PG (except for the genomeAssembly, which
should be either hg19 or hg38).

1. ``vcf`` is the path to the file we want to analyze (required).
2. ``exomiser`` is the path to the Exomiser data directory (see :ref:`exomiserdata` for details) (required)
3. ``datadir`` The path with LR2PG data that should be downloaded before running LR2PG (see :ref:`lr2pgdownload` for details). This is optional and the default is ``data``.
4. ``background_freq`` Most users will want to use the precomputed background files provided by LR2PG. In this case, the correct bacground file (for hg19 or hg38)
is determined automatically on the basis of the genomeAssembly (optional).
5. ``genomeAssembly`` This should be either hg19 (or hg37, which is synonymous) or hg38 (required)
6. ``transcriptdb``. This determines the set of transcripts used to call variants. Valid values are UCSC, ensembl, or RefSeq, and the default is UCSC (optional)


Additionall, ``hpoIds`` is a list of HPO term representing the clinical manifestations observed in the individual being analyzed.
Finally,  ``prefix`` is the prefix of the output file (optional, default: lr2pg) For instance, if the prefix is ``pfeiffer1``, then the HTML output file will be
``pfeiffer1.html``.

There are additional example yaml files in src/test/resources/yaml.


Running LIRICAL
~~~~~~~~~~~~~~~

To see all options for running LR2PG with a yaml file, enter ::

    $ java -jar Lr2pg.jar vcf -h

A typical command that runs LR2PG using settings shown in the YAML file with the default data directory would be simply ::

    $ java -jar Lr2pg.jar vcf -y demo1.yml

