.. _rstyamlorphenopackethpo:

YAML or Phenopacket as input?
=============================

How should users choose between YAML and Phenopackets as an input format? In general, we recommend that
users choose `Phenopackets <https://phenopackets-schema.readthedocs.io/en/latest/>`_ as the input format.
YAML is a simple format that can easily be edited by hand in a text editor and is suitable for testing and
demonstration, but is not as flexible or robust as Phenopackets. We have provided a simple tool that
creates Phenopackets for use by LIRICAL and other similar software
(`PhenopacketGenerator <https://github.com/TheJacksonLaboratory/PhenopacketGenerator>`_).
As a convenience, we present
the same simple case in first YAML and then Phenopacket format.


YAML version
^^^^^^^^^^^^

The data represents an individual with some characteristic manifestations of
`neurofibromatosis type 2 <https://hpo.jax.org/app/browse/disease/OMIM:101000>`_, in whom
`Tibial pseudoarthrosis <https://hpo.jax.org/app/browse/term/HP:0009736>`_, a characteristic feature of
neurofibromatosis type 1, has been ruled out. ::

    analysis:
        mindiff: 50
        threshold: 0.05
        tsv: true
        orphanet: true
    hpoIds: ['HP:0002321', 'HP:0000365', 'HP:0000360', 'HP:0009589', 'HP:0002858']
    negatedHpoIds: ['HP:0009736']
    prefix: NF2-example

Save this file as example.yml and then run LIRICAL as ::

     $ java -jar LIRICAL.jar yaml -y example.yml

Phenopackets version
^^^^^^^^^^^^^^^^^^^^

The identical data can be represented in Phenopacket format (in which only required fields are used) as follows. ::

    {
        "id": "proposita",
            "subject": {
                "id": "proposita",
            },
        "phenotypicFeatures": [ {
            "type": {
                "id": "HP:0000360",
                "label": "Tinnitus"
              	}
	    },{
            "type": {
                "id": "HP:0002321",
                "label": "Vertigo"
            }
	    }, {
            "type": {
                "id": "HP:0000365",
                "label": "Hearing impairment"
            }
        }, {
            "type": {
                "id": "HP:0009589",
                "label": "Bilateral vestibular Schwannoma"
            }
        }, {
            "type": {
                "id": "HP:0002858",
                "label": "Meningioma"
            }
        },{
            "type": {
                "id": "HP:0009736",
                "label": "Tibial pseudoarthrosis"
            },
		    "negated" : "true"
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
            }]
        }
    }

Save this file as example.json and then run LIRICAL as ::

    $ java -jar LIRICAL.jar phenopacket -p example.json

Identical results should be obtained for both cases. See :ref:`rstphenopackethpo` and :ref:`rstyamlhpo` for more
information about parameters and running LIRICAL with genomic data from VCF files.