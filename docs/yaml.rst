.. _rstyaml:

Running LIRICAL with a YAML file
================================

The recommended input format for running LIRICAL is the `Phenopacket <https://github.com/phenopackets>`_, but
LIRICAL also supports `YAML <https://en.wikipedia.org/wiki/YAML>`_, which is a simple, human readable format that
is commonly used for configuration files.



YAML
----
Before running LIRICAL, download and built it and set it up according to the instructions on the :ref:`rstsetup` page.
LIRICAL uses default values for many configuration options (see below), and a simple YAML configuration file would
include the following information. ::

    ## LIRICAL Analysis Template.
    # These are all the possible options for running LIRICAL. Use this as a template for
    # your own set-up.
    ---
    sampleId: NF2-example
    hpoIds: ['HP:0002321', 'HP:0000365', 'HP:0000360', 'HP:0009589', 'HP:0002858']
    negatedHpoIds: ['HP:0009736']
    age: P20Y6M
    sex: FEMALE
    vcf: /path/to/example.vcf


In YAML, lines that begin with ``#`` are comments, and the three dashes
indicate the start of the contents of the file.

1. ``sampleId`` is the indentifier of the individual being analyzed.
2. ``hpoIds`` is a list of HPO term representing the clinical manifestations observed in the individual being analyzed.
3. ``negatedHpoIds`` a list of HPO terms that represent abnormalities that were **excluded** in the proband.
4. ``age`` age of the individual entered using the ISO8601 duration notation. E.g. ``P20Y6M`` for an individual with age of 20 years and 6 months.
5. ``sex`` sex of the individual, either ``MALE`` or ``FEMALE``.
6. ``vcf`` is the path to the file we want to analyze. Note that the VCF file must contain the sample corresponding to ``sampleId``.

You can use the example file as a starting point for your own configuration file.
The file can be found at ``src/test/resources/yaml/hpo_and_vcf.yml``.


Running LIRICAL
~~~~~~~~~~~~~~~


A typical command that runs LIRICAL using settings shown in the YAML file with the default data directory would be simply ::

    $ java -jar LIRICAL.jar yaml -y example.yml



