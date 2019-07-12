.. _rstphenopacket:

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
        "phenotypes": [{
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
        "htsFiles": [{
            "htsFormat": "VCF",
            "individualToSampleIdentifiers": {
                "example-1": "example-1"
        },
        "genomeAssembly": "GRCH_37",
        "file": {
            "path": "/path/to/data/Pfeiffer.vcf"
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
        }]
     }
    }

Running LIRICAL with clinical and genomic data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL will perform combined phenotye and variant analysis if the Phenopacket contains an ``htsFiles`` element. In this
case, you need to indicate the path to the VCF file on your system as shown above (``/path/to/data/Pfeiffer.vcf``).


The ``-p`` option is used to indicate the Phenopacket, and the -e option is used to indicate the location of
the :ref:`rstexomiserdatadir`.
::

    $ java -jar LIRICAL.java phenopacket -p /path/to/example.json -e /path/to/exomiser-data/




Running LIRICAL with clinical data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
LIRICAL will perform phenotype-only analysis if the Phenopacket does not contain a ``htsFiles`` element.
In this case, the only required argument is the phenopacket. ::

    $ java -jar LIRICAL.java phenopacket -p /path/to/example.json




Output
~~~~~~

LR2PG can output either an HTML file with a summary of results or a tab-separated values (TSV) file for computational
pipelines. By default, LR2PG outputs an HTML file. TODO-link to page explaining the output.
Passing the --tsv flag will cause it to instead output a
TSV file that has one line for each differential diagnosis, ordered according to the post-test probability.