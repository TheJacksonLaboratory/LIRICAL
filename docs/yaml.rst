Running LR2PG with a YAML file
==============================

.. _yaml:

YAML
----

Here is an example YAML configuration file. ::

    ## LR2PG Analysis Template.
    # These are all the possible options for running LR2PG. Use this as a template for
    # your own set-up.
    ---
    analysis:
        # hg19 or hg38 is supported
        # make sure the Jannovar transcript file is taken from the same genome build
        genomeAssembly: hg19
        vcf: /home/robinp/data/exomiser-cli-9.0.1/examples/Pfeiffer.vcf
        jannovar: /home/robinp/data/exomiserdata/1802_hg19/1802_hg19_transcripts_refseq.ser
      #  hp.obo : data/hp.obo
       # phenotype.hpoa : data/phenotype.hpoa
      #  gene_info : data/Homo_sapiens_gene_info.gz
      #  medgen : data/mim2gene_medgen
       # background_freq : data/background-freq.txt
        datadir: data
        mvstore: /home/robinp/data/exomiserdata/1802_hg19/1802_hg19_variants.mv.db
    hpoIds: ['HP:0001156', 'HP:0001363', 'HP:0011304', 'HP:0010055']
    output: case1


The items in ``analysis`` all refer to the paths of files required to run LR2PG (except for the genomeAssembly, which
should be either hg19 or hg38). The ``vcf`` is the path to the file we want to analyze. The path for ``data``
is the download directory created by LR2PG with the download command (the default is simply "data").  The path
for mvstore refers to the Exomiser data directory.

The hpoIds is a list of HPO term representing the clinical manifestations observed in the individual being analyzed.
The ``output`` is the prefix of the output file.

TODO -- this will be simplified and modified a bit!