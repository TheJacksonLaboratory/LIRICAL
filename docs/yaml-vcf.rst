.. _rstyaml-vcf:

Running LIRICAL with a YAML file (HPO and VCF data)
===================================================


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
        genomeAssembly: hg19
        vcf: /path/to/example.vcf
        exomiser: /path/to/1811_hg19/
    hpoIds: ['HP:0001156', 'HP:0001363', 'HP:0011304', 'HP:0010055']
    prefix: example


This file can be found at ``src/test/resources/yaml/example1.yaml``.



In YAML, lines that begin with ``#`` are comments, and the three dashes
indicate the start of the contents of the file. The ``analysis`` element is used to hold a dictionary with options for
running the program. The items in ``analysis`` refer to the genome assembly and to the paths of files required to run LIRICAL.
Users must provide values for ``genomeAssembly``, ``vcf``, and ``exomiser``. Default values will be use for the
other three entries if the user does not proviude values.


1. ``vcf`` is the path to the file we want to analyze (required).
2. ``exomiser`` is the path to the Exomiser data directory (see :ref:`rstexomiserdatadir` for details) (required)
3. ``genomeAssembly`` This should be either hg19 (or hg37, which is synonymous) or hg38 (required)
4. ``datadir`` The path with LIRICAL data that should be downloaded before running LIRICAL (see :ref:`rstsetup` for details). This option should not be used if the default data location (``data``) is used.
5. ``background_freq`` Most users will want to use the precomputed background files provided by LIRICAL. In this case, the correct background file (for hg19 or hg38) is determined automatically on the basis of the genomeAssembly. This option should be used to have LIRICAL ingest a custom background file
6. ``transcriptdb``. This determines the set of transcripts used to call variants. Valid values are UCSC or RefSeq, and the default is UCSC (optional)
7. ``global``. If the YAML file contains the line ``global: true`` then it will not discard candidate diseases with no known disease gene or candidatesfor which no predicted pathogenic variant was found in the VCF.

Any of the options described in :ref:`rstyaml-hpo` can also be used here.

Additionally, ``hpoIds`` is a list of HPO term representing the clinical manifestations observed in the individual being analyzed.
Finally,  ``prefix`` is the prefix of the output file (optional, default: lirical) For instance, if the prefix is ``example1``, then the HTML output file will be
``example1.html``.


The following YAML file contains values for all of the options. ::

    ## LIRICAL Analysis Template.
    # These are all the possible options for running LIRICAL. Use this as a template for
    # your own set-up.
    ---
    analysis:
    # hg19 or hg38 is supported
        genomeAssembly: hg19
        vcf: /Users/peterrobinson/Documents/data/Pfeiffer.vcf
        exomiser: /Users/peterrobinson/Documents/data/exomiser/1802_hg19/
        datadir: /path/to/custom_location1/
        background: /path/to/custom_location2/background-hg38.txt
        transcriptdb: refseq
    hpoIds: [ 'HP:0001363', 'HP:0011304', 'HP:0010055']
    negatedHpoIds: ['HP:0001328']
    prefix: example2


This file can be found at ``src/test/resources/yaml/example2.yaml``. This YAML file additionally has a list
of HPO terms that represent abnormalities that were **excluded** in the proband (``negatedHpoIds``).

You can use either example file as a starting point for your own configuration file.


Running LIRICAL
~~~~~~~~~~~~~~~


A typical command that runs LIRICAL using settings shown in the YAML file with the default data directory would be simply ::

    $ java -jar LIRICAL.jar vcf -y example.yml



