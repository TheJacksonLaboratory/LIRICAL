.. _rstlirical-tsv:

LIRICAL TSV Output
==================

If LIRICAL is run with the ``--output-format tsv`` option, it will output a tab-separated values (TSV) file
with the results for each of the diagnoses.
For example, the following command will run LIRICAL on a Phenopacket and output a TSV file with the results::

  lirical phenopacket -p LDS2.v2.json \
    --output-format tsv

By default, LIRICAL outputs the data to a file called ``lirical.tsv``. This can be altered with the ``-x <prefix>`` option.


.. list-table:: LIRICAL's TSV format
   :header-rows: 1
   :widths: 40 60

   *  -  Item
      -  Explanation
   *  -  rank
      -  placement of the candidate diagnosis by LIRICAL
   *  -  diseaseName
      -  Name of the candidate disease
   *  -  diseaseCurie
      -  disease ID, e.g., OMIM:154700
   *  -  pretestprob
      -  Pretest probability of the candidate disease
   *  -  postestprob
      -  Postest probability of the candidate disease
   *  -  compositeLR
      -  Combined likelihood ratio of the candidate disease (logarithm of the product of all individual LRs)
   *  -  entrezGeneId
      -  Identifier of the candidate disease gene (if available)
   *  -  variants
      -  variant evaluation (if available)


The file begins with comment lines (that start with an exclamation mark) that provide information about the
HPO terms used to run the analysis.

