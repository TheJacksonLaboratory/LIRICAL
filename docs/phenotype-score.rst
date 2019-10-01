.. _rstphenotype-score:

LIRICAL's Phenotype Score
=========================

LIRICAL calculates a likelihood ratio score for phenotypic observations for each differential diagnosis. The phenotype
likelihood ratio score can be combined with LIRICAL's genotype likelhood ratio score for a combined analysis of
phenotypes and genetic data (such as exome or genome sequencing) or can be used as a tool to assess phenotype data
alone.


This page explains how to interpret LIRICAL's phenotype score.


Each disease shows a detailed explanation of the matching
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

The algorithm is implemented in the function ``getLikelihoodRatio`` in the class ``PhenotypeLikelihoodRatio`` (see the
Java code for details). The algorithm checks each query term for the best match to disease terms and uses a sequence of
rules to try to find the match. For the following explanation, we will refer to the query HPO term as :math:`h_i` and the
disease as :math:`\mathcal{D}`.

The likelihood ratio is calculated as :math:`\rm{LR}(h_i) = \frac{P(h_i|\mathcal{D})}{P(h_i|\neg \mathcal{D})}.`
The following sections describe how the numerator is calculated, i.e., the probability that an individual with the
disease has the phenotypic feature. The denominator is always calculated as the probability that
an individual with an arbitrary Mendelian disease has the feature in question (which is calculated based on
the entire HPO database for Mendelian diseases). See the manuscript for more details.

1. :math:`h_i` is identical to one of the terms to which :math:`\mathcal{D}` is annotated.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this case, :math:`P(h_i|\mathcal{D})` is equal to the frequency of :math:`h_i` among all individuals with
disease :math:`\mathcal{D}` (by default, this is taken to be 100%, but in many cases more precise frequencies
are available in the HPO database).

In the output file of LIRICAL, such matches are shown with the code *E* (Table 1).

 .. list-table:: `. Exact phenotypic feature match
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

2. :math:`h_i` is an ancestor of one or more of the terms to which :math:`\mathcal{D}` is annotated.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Because of the annotation propagation rule of subclass hierarchies in ontologies,
:math:`P(h_i|\mathcal{D})` is implicitly annotated to all of the ancestors of the set of annotating terms. For instance,
if the computational disease model of some disease :math:`\mathcal{D}` includes the HPO term *Polar cataract*
(`HP:0010696 <https://hpo.jax.org/app/browse/term/HP:0010696>`_)
then the disease is implicitly annotated to the parent term *Cataract*
(`HP:0000518 <https://hpo.jax.org/app/browse/term/HP:0000518>`_) (to see this consider that any person with a polar
cataract can also be said to have a cataract).

In this case, the probability of :math:`h_i` in disease :math:`\mathcal{D}` is equal to the maximum frequency of
any of the ancestors of :math:`h_i` in $\mathcal{D}$.


3. :math:`h_i`  is a descendant of one or more of the terms to which :math:`\mathcal{D}` is annotated.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this case, :math:`h_i` is a descendant (i.e., specific subclass of) some term :math:`h_j` of :math:`\mathcal{D}`.
For instance, disease :math:`\mathcal{D}` might be annotated to *Syncope*
(`HP:0001279 <https://hpo.jax.org/app/browse/term/HP:0001279>`_), and the query term :math:`h_i` is
*Orthostatic syncope* (`HP:0012670 <https://hpo.jax.org/app/browse/term/HP:0012670>`_), which is a child term
of *Syncope*. In addition, *Syncope* has two other child terms, *Carotid sinus syncope*
(`HP:0012669 <https://hpo.jax.org/app/browse/term/HP:0012669>`_) and *Vasovagal syncope*
(`HP:0012668 <https://hpo.jax.org/app/browse/term/HP:0012668>`_). According to our model,
we will adjust the frequency of *Syncope* in disease :math:`\mathcal{D}` (say, 0.72) by dividing it by the total number
of child terms of :math:`h_j` (so in our example, we would use the frequency :math:`0.72\times 1/3=0.24`).