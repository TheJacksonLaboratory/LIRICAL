.. _rstphenotype-score:

Example input file: Phenopacket
===============================

This is an example of an input file with four HPO terms, including one negated (ecluded) term, and the
path to a VCF file.  ::

    {
        "id": "proposita",
        "subject": {
            "id": "proposita",
            "ageAtCollection": {
                "age": "P27Y"
            },
            "sex": "FEMALE",
            "taxonomy": {
                "id": "NCBITaxon:9606",
                "label": "Homo sapiens"
            }
        },
        "phenotypicFeatures": [ {
            "type": {
                "id": "HP:0002650",
                "label": "Scoliosis"
            },
            "evidence": [{
                "evidenceCode": {
                    "id": "ECO:0000033",
                    "label": "author statement supported by traceable reference"
                },
                "reference": {
                    "id": "PMID:25163805",
                    "description": "NM_001135599.3:c.839-1G\u003eA (inferred from Figure 2; the RefSeq accession number in the original publication appears to be incorrect). The authors show that the variant leads to usage of a crytpic splice site."
                }
             }]
        }, {
            "type": {
                "id": "HP:0000592",
                "label": "Blue sclerae"
            },
            "evidence": [{
                "evidenceCode": {
                    "id": "ECO:0000033",
                    "label": "author statement supported by traceable reference"
                },
                "reference": {
                    "id": "PMID:25163805",
                    "description": "NM_001135599.3:c.839-1G\u003eA (inferred from Figure 2; the RefSeq accession number in the original publication appears to be incorrect). The authors show that the variant leads to usage of a crytpic splice site."
                }
            }]
        }, {
            "type": {
                "id": "HP:0001083",
                "label": "Ectopia lentis"
        },
            "negated": true,
            "evidence": [{
                "evidenceCode": {
                    "id": "ECO:0000033",
                    "label": "author statement supported by traceable reference"
                },
                "reference": {
                    "id": "PMID:25163805",
                    "description": "NM_001135599.3:c.839-1G\u003eA (inferred from Figure 2; the RefSeq accession number in the original publication appears to be incorrect). The authors show that the variant leads to usage of a crytpic splice site."
                }
            }]
        }, {
            "type": {
                "id": "HP:0010812",
                "label": "Short uvula"
            },
            "evidence": [{
                "evidenceCode": {
                    "id": "ECO:0000033",
                    "label": "author statement supported by traceable reference"
                },
                "reference": {
                    "id": "PMID:25163805",
                    "description": "NM_001135599.3:c.839-1G\u003eA (inferred from Figure 2; the RefSeq accession number in the original publication appears to be incorrect). The authors show that the variant leads to usage of a crytpic splice site."
                }
            }]
        }, {
            "type": {
                "id": "HP:0005116",
                "label": "Arterial tortuosity"
            },
            "evidence": [{
                "evidenceCode": {
                    "id": "ECO:0000033",
                    "label": "author statement supported by traceable reference"
            },
            "reference": {
                "id": "PMID:25163805",
                "description": "NM_001135599.3:c.839-1G\u003eA (inferred from Figure 2; the RefSeq accession number in the original publication appears to be incorrect). The authors show that the variant leads to usage of a crytpic splice site."
                }
            }]
        }],
        "htsFiles": [{
            "uri": "file://data/genomes/example.vcf.gz",
                   "description": "exome",
                   "htsFormat": "VCF",
                   "genomeAssembly": "GRCh38",
        }],
        "metaData": {
            "createdBy": "Hpo Case Annotator : 1.0.13",
            "submittedBy": "HP:probinson",
            "resources": [{
                "id": "hp",
                "name": "human phenotype ontology",
                "url": "http://purl.obolibrary.org/obo/hp.owl",
                "version": "2018-03-08",
                "namespacePrefix": "HP",
                "iriPrefix": "http://purl.obolibrary.org/obo/HP_"
            }, {
                "id": "ncbitaxon",
                "name": "NCBI organismal classification",
                "url": "http://purl.obolibrary.org/obo/ncbitaxon.owl",
                "version": "2018-03-02",
                "namespacePrefix": "NCBITaxon"
            }, {
                "id": "eco",
                "name": "Evidence and Conclusion Ontology",
                "url": "http://purl.obolibrary.org/obo/eco.owl",
                "version": "2018-11-10",
                "namespacePrefix": "ECO",
                "iriPrefix": "http://purl.obolibrary.org/obo/ECO_"
            }, {
                "id": "omim",
                "name": "Online Mendelian Inheritance in Man",
                "url": "https://www.omim.org",
                "namespacePrefix": "OMIM"
            }]
        }
    }



