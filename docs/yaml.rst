Running LR2PG VCF analysis with a YAML file
===========================================

.. _yaml:


LR2PG can be run using a `YAML <https://en.wikipedia.org/wiki/YAML>`_ configuration file (which is described on this page)
or from a :ref:`phenopacket`. YAML is a simple, human readable format that is commonly used for configuration files.



YAML
----

Here is an example YAML configuration file. ::

    ## LR2PG Analysis Template.
    # These are all the possible options for running LR2PG. Use this as a template for
    # your own set-up.
    ---
    analysis:
        # hg19 or hg38 is supported
        genomeAssembly: hg19
        vcf: /home/robinp/data/exomiser-cli-9.0.1/examples/Pfeiffer.vcf
        background_freq : data/background-freq.txt
        datadir: data
        exomiser: /home/robinp/data/exomiserdata/1802_hg19/
        transcriptdb: refseq
    hpoIds: ['HP:0001156', 'HP:0001363', 'HP:0011304', 'HP:0010055']
    prefix: pfeiffer1


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


Running LR2PG
~~~~~~~~~~~~~

To see all options for running LR2PG with a yaml file, enter ::

    $ java -jar Lr2pg.jar vcf -h

A typical command that runs LR2PG using settings shown in the YAML file with the default data directory would be simply ::

    $ java -jar Lr2pg.jar vcf -y demo1.yml

