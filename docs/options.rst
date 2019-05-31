.. _rstoptions:

LIRICAL Options
===============

LIRICAL can be run with the following options.


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


Running LIRICAL with a yaml configuration file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``yaml`` command runs LIRICAL from a :ref:`rstyaml` configuration file.

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
    * - ``-d``
      - ``--download``
      - data
      - diretory that contains the downloaded data
    * - ``-k``
      - ``--keep``
      - false
      - retain candidates even if no candidate variant is found
    * - ``-m``
      - ``--mindiff``
      - 10
      - minimal number of differential diagnoses to show
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
      - use strict genotype matching for likelihood ratio calculation
    * - ``-t``
      - ``--threshold``
      - 0.01
      - minimum post-test prob. to show diagnosis in HTML output
    * - none
      - ``--transcriptdb``
      - ucsc
      - transcript database (UCSC, Ensembl, RefSeq)
    * - none
      - ``--tsv``
      - false
      - Use TSV instead of HTML output

phenopacket
~~~~~~~~~~~

    simulate-vcf      Simulate VCF analysis from phenopacket
      Usage: simulate-vcf [options]
        Options:
          -b, --background
            path to non-default background frequency file
          -d, --data
            directory to download data
            Default: data
          -e, --exomiser
            path to the Exomiser data directory
          -f, --filter
            filter on VCF FILTER quality
            Default: true
          -k, --keep
            retain candidates even if no candidate variant is found
            Default: false
          -m, --mindiff
            minimal number of differential diagnoses to show
            Default: 10
          -o, --output-directory
            directory into which to write output file(s).
          -p, --phenopacket
            path to phenopacket file
          --phenopacket-dir
            path to directory with multiple phenopackets
          -x, --prefix
            prefix of outfile
            Default: lirical
          --strict
            use strict genotype matching for likelihood ratio calculation
            Default: false
        * -v, --template-vcf
            path to template VCF file
          -t, --threshold
            minimum post-test prob. to show diagnosis in HTML output
            Default: 0.01
          --transcriptdb
            transcript database (UCSC, Ensembl, RefSeq)
            Default: ucsc
          --tsv
            Use TSV instead of HTML output
            Default: false
          -outputfile
            name of the output file with simulation results
            Default: vcf_simulation_results.tsv

    phenopacket      Run LIRICAL from a Phenopacket
      Usage: phenopacket [options]
        Options:
          -b, --background
            path to non-default background frequency file
          -d, --data
            directory to download data
            Default: data
          -e, --exomiser
            path to the Exomiser data directory
          -f, --filter
            filter on VCF FILTER quality
            Default: true
          -k, --keep
            retain candidates even if no candidate variant is found
            Default: false
          -m, --mindiff
            minimal number of differential diagnoses to show
            Default: 10
          -o, --output-directory
            directory into which to write output file(s).
        * -p, --phenopacket
            path to phenopacket file
          -x, --prefix
            prefix of outfile
            Default: lirical
          --strict
            use strict genotype matching for likelihood ratio calculation
            Default: false
          -t, --threshold
            minimum post-test prob. to show diagnosis in HTML output
            Default: 0.01
          --transcriptdb
            transcript database (UCSC, Ensembl, RefSeq)
            Default: ucsc
          --tsv
            Use TSV instead of HTML output
            Default: false