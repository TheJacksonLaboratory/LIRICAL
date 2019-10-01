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

    E:Specific learning disability[HP:0001328][152.894]
    E:Obesity[HP:0001513][62.561]
    E:Rod-cone dystrophy[HP:0000510][45.396]
    Q<D:Macular degeneration[HP:0000608]<Retinal degeneration[HP:0000546][32.605]
    E:Strabismus[HP:0000486][16.648]
    E:Global developmental delay[HP:0001263][6.800]
    Q~D:Attenuation of retinal blood vessels[HP:0007843]~Abnormal retinal morphology[HP:0000479][1.267]

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

In the output file of LIRICAL, such matches are shown with the code **E**. The likelihood ratio for this match is
84.767.

 .. list-table:: ``**E**. Exact phenotypic feature match
    :widths: 100
    :header-rows: 1

    * - Example
    * - E:Nail dystrophy[HP:0008404][84.767]


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


 .. list-table:: `**Q~D**. Query term matches ancestor of term annotated to :math:`\mathcal{D}`
    :widths: 100
    :header-rows: 1

    * - Example
    * - Q<D:Macular degeneration[HP:0000608]<Retinal degeneration[HP:0000546][32.605]

In this example, the likelihood ratio is 2.082. *Macular degeneration* (`HP:0000608 <https://hpo.jax.org/app/browse/term/HP:0000608>`_)
is a subclass of *Retinal degeneration* (`HP:0000546 <https://hpo.jax.org/app/browse/term/HP:0000546>`_).

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