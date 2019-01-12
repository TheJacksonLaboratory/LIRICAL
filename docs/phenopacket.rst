Running LR2PG with a Phenopacket file
=====================================

`Phenopackets <https://github.com/phenopackets>`_ represent an open standard for sharing disease and phenotype information.
This is a new standard of the `Global Alliance for Genomics and Health <https://www.ga4gh.org/>`_ that
links detailed phenotype descriptions with disease, patient, and genetic information. We use PhenoPackets
as one of two options for the input of phenotype information to LR2PG (the other being :ref:`yaml` configuration files).

VCF file input
--------------

LR2PG can be run with or without a VCF file. If run without a VCF file, LR2PG performs a purely phenotype-based
analysis. If run with a VCF file, it will include the genotype likelihood ratio as a part of its calculations.
Users should include (or not) a reference to the VCF file in the phenopacket. The following example shows a phenopacket
representing an individual with `Pfeiffer syndrome <https://omim.org/entry/101600>`_ in whom exome sequencing has been performed. ::

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
            "path": "/home/peter/data/Pfeiffer.vcf"
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

Options
-------

These are the options for running the ``phenopacket`` command. ::

     phenopacket      Run LR2PG from a Phenopacket
      Usage: phenopacket [options]
        Options:
          -d, --data
            directory to download data (default: ${DEFAULT-VALUE})
            Default: data
        * -j, --jannovar
            path to Jannovar transcript information file
        * -m, --mvstore
            path to MV Store Exomiser database file
          -o, --outfile
            prefix of outfile
            Default: lr2pg
        * -p, --phenopacket
            path to phenopacket file
          -t, --threshold
            threshold for showing diagnosis in HTML output
            Default: 0.01
          --tsv
            Use TSV instead of HTML output
            Default: false


Output
------

LR2PG can output either an HTML file with a summary of results or a tab-separated values (TSV) file for computational
pipelines. By default, LR2PG outputs an HTML file. TODO-link to page explaining the output.
Passing the --tsv flag will cause it to instead output a
TSV file that has one line for each differential diagnosis, ordered according to the post-test probability.