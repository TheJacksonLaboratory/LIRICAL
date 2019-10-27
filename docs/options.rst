.. _rstoptions:

LIRICAL Options
===============

This page summarizes the options explained in detail in the :ref:`rstrunning` section.


Download
~~~~~~~~
The ``download`` command downloads files required to run LIRICAL:

* Homo_sampiens_gene_info.gz
* hp.obo
* phenotype.hpoa
* mim2gene_medgen


By default, LIRICAL will create a directory called ``data`` and download the files there. LIRICAL will
download to a non-default directory if the user passes the ``-d`` option.


.. list-table::  ``download`` command
    :widths: 25 25 50 50
    :header-rows: 1

    * - short
      - long
      - Default
      - Explanation
    * - ``-d``
      - ``--download``
      - data
      - directory to download data
    * - ``-w``
      - ``--overwrite``
      - false
      - overwrite prevously downloaded files, if any


Running LIRICAL with a phenopacket
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``phenopacket`` command runs LIRICAL from a :ref:`rstyaml` configuration file.

.. list-table::  ``yaml`` command
    :widths: 1 1 1 1
    :class: longtable
    :header-rows: 1

    * - short
      - long
      - Default
      - Explanation
    * - ``-p``
      - ``--phenopacket``
      - n/a
      - path to Phenopacket
    * - ``-d``
      - ``--download``
      - data
      - diretory that contains the downloaded data
    * - ``-g``
      - ``--global``
      - false
      - retain candidate diseases even if no candidate gene is known or no candidate variant is found in VCF file.
    * - ``-m``
      - ``--mindiff``
      - 10
      - minimal number of differential diagnoses to show in the HTML output file.
    * - ``-o``
      - ``--output-directory``
      - n/a
      - directory into which to write output file(s).
    * - ``-x``
      - ``--prefix``
      - lirical
      - prefix of outfile
    * - none
      - ``--strict``
      - false
      - use strict genotype matching for likelihood ratio calculation. This option causes LIRICAL to show only candidates that have a genotype that matches what is expected because of the mode of inheritance of the disease
    * - ``-t``
      - ``--threshold``
      - 0.01
      - minimum post-test probability to show a diagnosis in the HTML output. This option, together with ``--mindiff``, controls the number of panels that show information about candidates in the HTML output.
    * - none
      - ``--transcriptdb``
      - ucsc
      - transcript database. Valid optiona are UCSC, Ensembl, and RefSeq
    * - none
      - ``--tsv``
      - false
      - Use TSV instead of HTML output





YAML
~~~~

The ``yaml`` command runs LIRICAL from a :ref:`rstphenopacket` configuration file. Users should
indicate all non-default arguments within the YAML file. The only valid argument for the
``yaml`` command is the path to the YAML file. ::

    $ java -jar LIRICAL.java yaml -y example.yaml



.. list-table::  ``yaml`` command
    :widths: 25 25 50 50
    :header-rows: 1

    * - short
      - long
      - Default
      - Explanation
    * - ``-y``
      - ``--yaml``
      - n/a
      - path to yaml configuration file