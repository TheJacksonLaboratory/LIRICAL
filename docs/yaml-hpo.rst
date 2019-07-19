.. _rstyaml-hpo:

Running LIRICAL with a YAML file (HPO data)
===========================================


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
        mindiff: 50
        threshold: 0.05
        tsv: true
        datadir: mydata
    hpoIds: ['HP:0001156', 'HP:0001363', 'HP:0011304', 'HP:0010055']
    negatedHpoIds: ['HP:0001328']
    prefix: example


This file can be found at ``src/test/resources/yaml/example1.yaml``.



In YAML, lines that begin with ``#`` are comments, and the three dashes
indicate the start of the contents of the file. The ``analysis`` element is used to hold a dictionary with options for
running the program. The items in ``analysis`` refer to the genome assembly and to the paths of files required to run LIRICAL.
Users must provide values for ``genomeAssembly``, ``vcf``, and ``exomiser``. Default values will be use for the
other three entries if the user does not proviude values.


1. ``mindiff``
By default, LIRICAL shows all differential diagnoses with a posterior probability of
at least 1%, and at least 10 entries regardless of the posterior probability. If you
want LIRICAL to show details about more differentials, set this option to the desired number.

2. ``threshold``
This option controls the minimum post-test probability to show a differential diagnosis in HTML output.
By default, LIRICAL shows all differnetials with a posterior probability of 1% or greater.


3. ``tsv`` T
Use TSV instead of HTML output (Default: false).

4. ``datadir``
The path with LIRICAL data that should be downloaded before running LIRICAL
(see :ref:`rstsetup` for details). This option should not be used if the default data location (``data``) is used.



Additionally, ``hpoIds`` is a list of HPO term representing the clinical manifestations
observed in the individual being analyzed. In contrast, ``negatedHpoIds`` represents
phenotypic abnormalities (HPO terms) that were explicitly excluded in the proband.


Finally,  ``prefix`` is the prefix of the output file (optional, default: lirical).
 For instance, if the prefix is ``example1``, then the HTML output file will be
``example1.html``.





Running LIRICAL
~~~~~~~~~~~~~~~


A typical command that runs LIRICAL using settings shown in the YAML file with the default data directory would be simply ::

    $ java -jar LIRICAL.jar vcf -y example.yml

