.. _rstphenopacketvcf:

Running LIRICAL with a Phenopacket file (HPO and VCF data)
==========================================================




Preparing Phenopacket-formated data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following example shows a phenopacket
representing an individual with `Pfeiffer syndrome <https://omim.org/entry/101600>`_. THe file
is adapted from the phenopacket on :ref"`rstphenopackethpo`. We have removed several of the
phenotypic features, and added an **HtsFiles* element that contains the path of the VCF file
(in our exmaple, the path is ``/path/to/data/Pfeiffer.vcf``, but obviously you need to adjust
the path to a file located on your system). ::

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
the :ref:`rstexomiserdatadir`. The minimal command (using all default settings) is as follows.
::

    $ java -jar LIRICAL.java phenopacket -p /path/to/example.json -e /path/to/exomiser-data/


LIRICAL Options for clinical/genomic analysis
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

todo