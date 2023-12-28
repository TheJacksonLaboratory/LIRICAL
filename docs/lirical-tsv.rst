.. _rstlirical-tsv:

LIRICAL TSV Output
==================

If LIRICAL is run with the ``--output-format tsv`` option, it will output a tab-separated values (TSV) file
with the results for each of the diagnoses.
For example, the following command will run LIRICAL on a Phenopacket and output a TSV file with the results::

  lirical phenopacket -p LDS2.v2.json \
    --output-format tsv

By default, LIRICAL outputs the data to a file called ``lirical.tsv``. This can be altered with the ``-x <prefix>`` option.

The TSV output consists of the header and the body. The header includes lines that start with an exclamation mark,
to provide information about the HPO terms used to run the analysis.

The body section summarizes the matches between the patient data and the diseases, one disease per row, ranked by
the post-test probability.
Each row includes the disease credentials, the pre-test and post-test probabilities, the composite likelihood ratio.
If the analysis was run with a VCF file, the report includes two extra columns with the gene associated with the disease
and the variants found in the gene.

.. list-table:: LIRICAL's TSV format
   :header-rows: 1
   :widths: 40 60

   *  -  Column name
      -  Explanation
   *  -  rank
      -  Placement of the candidate diagnosis by LIRICAL
   *  -  diseaseName
      -  Name of the candidate disease
   *  -  diseaseCurie
      -  Disease identifier, e.g., `OMIM:154700`
   *  -  pretestprob
      -  Pretest probability of the candidate disease
   *  -  postestprob
      -  Postest probability of the candidate disease
   *  -  compositeLR
      -  Combined likelihood ratio of the candidate disease (logarithm of the product of all individual LRs)
   *  -  entrezGeneId
      -  Identifier of the candidate disease gene (if run with a VCF file)
   *  -  variants
      -  Variant evaluation (if run with a VCF file)

