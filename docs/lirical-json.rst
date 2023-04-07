.. _rstlirical-json:

LIRICAL JSON Output
===================

Lirical outputs results in a JSON format if ``--output-format json`` option is included in the command-line::

  lirical phenopacket -p LDS2.v2.json \
    --output-format json

The JSON document contains three sections, the sections are described in the following text along with examples.

Examples of each section are provided below:

Analysis data
^^^^^^^^^^^^^

``analysisData`` contains the analysis inputs (e.g. sample ID, age, present/excluded phenotypic features):

.. code-block:: json

  "analysisData" : {
    "sampleId" : "EXAMPLE",
    "age" : "P1Y2M3D",
    "sex" : "MALE",
    "observedPhenotypicFeatures" : [ "HP:0000001", "HP:0000002" ],
    "excludedPhenotypicFeatures" : [ "HP:0000003" ]
  }

Analysis metadata
^^^^^^^^^^^^^^^^^

``analysisMetadata`` includes user-provided options, analysis date, resource versions, etc.

.. code-block:: json

  "analysisMetadata" : {
    "liricalVersion" : "liricalVersion",
    "hpoVersion" : "hpoVersion",
    "transcriptDatabase" : "transcriptDatabase",
    "analysisDate" : "2022-12-29T14:02:58.653929682",
    "sampleName" : "sampleId",
    "isGlobalAnalysisMode" : true
  }

Analysis results
^^^^^^^^^^^^^^^^

``analysisResults`` has a list of `TestResult`\ s for each of the tested diseases.

Here we show an example for a made-up disease `OMIM:1234567`.

.. code-block:: json

  "analysisResults" : [ {
    "diseaseId" : "OMIM:1234567",
    "pretestProbability" : 1.2,
    "observedPhenotypicFeatures" : [ {
      "query" : "HP:0000001",
      "match" : "HP:0000002",
      "matchType" : "EXACT_MATCH",
      "lr" : 1.34,
      "explanation" : "EXPLANATION"
    } ],
    "excludedPhenotypicFeatures" : [ {
      "query" : "HP:0000001",
      "match" : "HP:0000003",
      "matchType" : "EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE",
      "lr" : 1.23,
      "explanation" : "EXCLUDED_EXPLANATION"
    } ],
    "genotypeLR" : {
      "geneId" : {
        "id" : "NCBIGene:1234",
        "symbol" : "GENE_SYMBOL"
      },
      "lr" : 1.23,
      "explanation" : "GENE_EXPLANATION"
    },
    "compositeLR" : 2.027286,
    "posttestProbability" : 1.0895759082370065
  } ]

