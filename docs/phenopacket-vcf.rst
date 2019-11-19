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
            "id": "example-1"
        },
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
        }],
        "htsFiles":
        [{
            "uri": "file://path/to/data/example.vcf",
            "description": "test",
            "htsFormat": "VCF",
            "genomeAssembly": "GRCh19",
            "individualToSampleIdentifiers": {
                "patient1": "NA12345"
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

All of the options for the phenotype-only phenopacket analysis (:ref:`rstphenopackethpo`) can be used for the
clinical/genomic analysis. Additionally, the following options are available.

::

    -b, --background


LIRICAL uses a background frequency file that records the freqeuncy of predicted pathogenic variants
in protein-coding genes (as estimated from gnomAD data). By default, LIRICAL will use pre-fabricated
files for this (that are included in the ``src/main/resources/background`` directory). This is recommended
for most users. If you create your own background file, then you can use it with the ``-b`` option, that should
then indicate the path to a non-default background frequency file.

::

    -e, --exomiser

Path to the Exomiser data directory (required for VCF-based analysis).

::

     --transcriptdb

LIRICAL can use transcript data from UCSC, Ensembl, or RefSeq. The default is
`RefSeq <https://www.ncbi.nlm.nih.gov/refseq/>`_, but transcript definitions from
`UCSC <http://genome.ucsc.edu/>`_ and `Ensembl <http://genome.ucsc.edu/>`_ can also be used
(e.g., ``--transcriptdb USCS`` or ``--transcriptdb ensembl``).

::

    --global

By default,  LIRICAL's default mode, which only ranks candidate genes for which at least one pathogenic allele is
present in the VCF file. LIRICAL can also be run in a ```--global`` mode in which diseases are ranked irrespective of
whether a disease gene is known for a disease or whether the gene is found to have a pathogenic allele or not.



