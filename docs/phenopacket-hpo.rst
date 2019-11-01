.. _rstphenopackethpo:

Running LIRICAL with a Phenopacket file
=======================================
LIRICAL can be run with clinical data (HPO terms) only or with clinical data and a VCF file representing the
results of gene panel, exome, or genome sequencing. The prefered input format is
`Phenopackets <https://github.com/phenopackets>`_, an open standard for sharing disease and phenotype information.
This is a new standard of the `Global Alliance for Genomics and Health <https://www.ga4gh.org/>`_ that
links detailed phenotype descriptions with disease, patient, and genetic information (The
other allowed input format is YAML. See :ref:`rstyaml`).


Preparing Phenopacket-formated data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


See the `Phenopackets <https://phenopackets-schema.readthedocs.io/en/latest/>`_ website for details on the format. LIRICAL expects
the Phenopacket to be in JSON format. The following example shows a phenopacket
representing an individual with `Pfeiffer syndrome <https://omim.org/entry/101600>`_ in whom exome sequencing has
been performed, whereby the corresponding VCF file is available at ``/path/to/data/Pfeiffer.vcf``. ::

    {
        "subject": {
        "id": "example-1",
        "phenotypicFeatures": [{
            "type": {
                "id": "HP:0000244",
                "label": "Turribrachycephaly"
            },
            "classOfOnset": {
                "id": "HP:0003577",
                "label": "Congenital onset"
            }
        }, {
            "type": {
                "id": "HP:0001363",
                "label": "Craniosynostosis"
            },
            "classOfOnset": {
                "id": "HP:0003577",
                "label": "Congenital onset"
             }
        }, {
            "type": {
                "id": "HP:0000453",
                "label": "Choanal atresia"
            },
            "classOfOnset": {
            "id": "HP:0003577",
            "label": "Congenital onset"
             }
        }, {
            "type": {
                "id": "HP:0000327",
            "label": "Hypoplasia of the maxilla"
            },
            "classOfOnset": {
                "id": "HP:0003577",
                "label": "Congenital onset"
            }
        }, {
            "type": {
                "id": "HP:0000238",
                "label": "Hydrocephalus"
        },
            "classOfOnset": {
                "id": "HP:0003577",
                "label": "Congenital onset"
        }
        }]
    },
    "metaData": {
        "createdBy": "Peter R.",
        "resources": [{
        "id": "hp",
        "name": "human phenotype ontology",
        "namespacePrefix": "HP",
        "url": "http://purl.obolibrary.org/obo/hp.owl",
        "version": "2018-03-08",
        "iriPrefix": "http://purl.obolibrary.org/obo/HP_"
        }]
     }
    }



Running LIRICAL with clinical data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
LIRICAL will perform phenotype-only analysis if the Phenopacket does not contain a ``htsFiles`` element.
In this case, the only required argument is the phenopacket. ::

    $ java -jar LIRICAL.jar phenopacket -p /path/to/example.json



LIRICAL Options for clinical/genomic analysis
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following options can be use to alter the default behavior of LIRICAL

::

  -d, --data <directory>

By default, LIRICAL downloads several data files to a directory called ``data`` that it creates in the
current working directory. If you download these files to some other directory, then you will need to
indicate that path with this option.


::

     -m, --mindiff <int>


By default, LIRICAL shows all differential diagnoses with a posterior probability of
at least 1%, and at least 10 entries regardless of the posterior probability. If you
want LIRICAL to show details about more differentials, set this option to the desired number.

::

    -t, --threshold

This option controls the minimum post-test probability to show a differential diagnosis in HTML output.
By default, LIRICAL shows all differentials with a posterior probability of 1% or greater.


::

     -x, --prefix

The output file will be either ``prefix.html`` or ``prefix.tsv``, whereby prefix can be set with this
option (e.g., ``-x example`` would cause LIRICAL to output ``example.html``). By default, the prefix is
set to "lirical".


::

      -o, --output-directory

Directory into which to write output file(s).


::

     --tsv
Use TSV instead of HTML output (Default: false).


::

    --orphanet

Use annotation data from `Orphanet <https://www.orpha.net/consor/cgi-bin/index.php>`_.

Output
~~~~~~


See :ref:`rstoutput` for details on the HTML and TSV output files.