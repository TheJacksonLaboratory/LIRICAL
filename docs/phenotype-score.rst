.. _rstphenotype-score:

LIRICAL's Phenotype Score
=========================

LIRICAL calculates a likelihood ratio score for phenotypic observations for each differential diagnosis. The phenotype
likelihood ratio score can be combined with LIRICAL's genotype likelhood ratio score for a combined analysis of
phenotypes and genetic data (such as exome or genome sequencing) or can be used as a tool to assess phenotype data
alone.


This page explains how to interpret LIRICAL's phenotype score. Each disease shows a detailed explanation of the matching
score. For instance, the  match with `Ectodermal Dysplasia 9, Hair/nail Type <https://hpo.jax.org/app/browse/disease/OMIM:614931>`_ shown
on :ref:`rstlirical-html` shows the following:

::

    E:Nail dystrophy[HP:0008404][84.767];
    Q~D:Onycholysis of fingernails[HP:0040039]~Abnormality of the nail[HP:0001597][2.423]; \
    Q~D:Absent hair[HP:0002298]~Abnormal hair quantity[HP:0011362][2.082];
    Q~D:Absent eyebrow[HP:0002223]~Abnormal hair quantity[HP:0011362][2.082];
    Q~D:Absent eyelashes[HP:0000561]~Abnormal hair quantity[HP:0011362][2.082];
    XA:Abnormality of the dentition[HP:0000164][1.089];
    XA:Abnormal sweat gland morphology[HP:0000971][1.001]

Each match shows a code for the category of the match, followed by details of the matching term (only
one term is shown for exact matches), and the matching score.



 .. list-table:: Phenopacket requirements for the ``File`` element
    :widths: 25 50 50
    :header-rows: 1

    * - Code
      - Explanation
      - Example
    * - *E*
      - exact match
      - E:Nail dystrophy[HP:0008404]
    * - *Q~D*
      - query term subset of disease term
      - Absent hair[HP:0002298]~Abnormal hair quantity[HP:0011362][

TODO -- finish table
