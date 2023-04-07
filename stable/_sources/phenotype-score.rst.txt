.. _rstphenotype-score:

LIRICAL's Phenotype Score
=========================

LIRICAL calculates a likelihood ratio score for phenotypic observations for each differential diagnosis. The phenotype
likelihood ratio score can be combined with LIRICAL's genotype likelihood ratio score for a combined analysis of
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

 .. list-table:: Exact phenotypic feature match (E)
    :widths: 100
    :header-rows: 1

    * - Example
    * - E:Nail dystrophy[HP:0008404][84.767]


2. :math:`h_i` is an ancestor (superclass) of one or more of the terms to which :math:`\mathcal{D}` is annotated.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Because of the annotation propagation rule of subclass hierarchies in ontologies,
:math:`P(h_i|\mathcal{D})` is implicitly annotated to all of the ancestors of the set of annotating terms. For instance,
if the computational disease model of some disease :math:`\mathcal{D}` includes the HPO term *Polar cataract*
(`HP:0010696 <https://hpo.jax.org/app/browse/term/HP:0010696>`_)
then the disease is implicitly annotated to the parent term *Cataract*
(`HP:0000518 <https://hpo.jax.org/app/browse/term/HP:0000518>`_) (to see this consider that any person with a polar
cataract can also be said to have a cataract).

In this case, the probability of :math:`h_i` in disease :math:`\mathcal{D}` is equal to the maximum frequency of
any of the ancestors of :math:`h_i` in $\mathcal{D}$.



 .. list-table:: Query term is parent of a disease term term (D<Q)
    :widths: 100
    :header-rows: 1

    * - Example
    * - D<Q:Short middle phalanx of the 5th finger[HP:0004220]<Brachydactyly[HP:0001156][29.847]




3. :math:`h_i`  is a child term (subclass) of one or more of the terms to which :math:`\mathcal{D}` is annotated.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this case, :math:`h_i` is a descendant (i.e., specific subclass of) some term :math:`h_j` of :math:`\mathcal{D}`.
For instance, disease :math:`\mathcal{D}` might be annotated to *Syncope*
(`HP:0001279 <https://hpo.jax.org/app/browse/term/HP:0001279>`_), and the query term :math:`h_i` is
*Orthostatic syncope* (`HP:0012670 <https://hpo.jax.org/app/browse/term/HP:0012670>`_), which is a child term
of *Syncope*. In addition, *Syncope* has two other child terms, *Carotid sinus syncope*
(`HP:0012669 <https://hpo.jax.org/app/browse/term/HP:0012669>`_) and *Vasovagal syncope*
(`HP:0012668 <https://hpo.jax.org/app/browse/term/HP:0012668>`_). According to our model,
we will adjust the frequency of *Syncope* in disease :math:`\mathcal{D}` (say, 0.72) by dividing it by the total number
of child terms of :math:`h_j` (so in our example, we would use the frequency :math:`0.72\times 1/3=0.24`).



 .. list-table:: Query term is child of disease term (Q<D)
    :widths: 100
    :header-rows: 1

    * - Example
    * - Q<D:Macular degeneration[HP:0000608]<Retinal degeneration[HP:0000546][32.605]

In this example, the likelihood ratio is 2.082. *Macular degeneration* (`HP:0000608 <https://hpo.jax.org/app/browse/term/HP:0000608>`_)
is a subclass of *Retinal degeneration* (`HP:0000546 <https://hpo.jax.org/app/browse/term/HP:0000546>`_).

4. :math:`h_i`  and some term to which :math:`\mathcal{D}` is annotated have a non-root common ancestor.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This option pertains if options (ii) and (iii) do not, i.e., :math:`h_i`  is not a child term of any disease term
:math:`h_j`  and no disease term :math:`h_j`  is a child of :math:`h_i` .

If this is the case, then we find the closest common-ancestor, and determine the likelihood ratio according to the
formula :math:`\rm{LR}(h_i) = \frac{P(h_i|\mathcal{D})}{P(h_i|\neg \mathcal{D})}`. Because the common ancestor is
higher up in the HPO hierarchy, the likelihood ratio tends to be lower and sometimes substantially lower. In order
to limit the amount of negative influence of any one query term, the likelihood ratio is defined to be at least 1/100.

 .. list-table:: Non-root distant match (Q~D)
    :widths: 100
    :header-rows: 1

    * - Example
    * - Q~D:Macular degeneration[HP:0000608]~Abnormal retinal morphology[HP:0000479][0.127]

In this example, *Macular degeneration* (`HP:0000608 <https://hpo.jax.org/app/browse/term/HP:0000608>`_) is not
a direct child of *Abnormal retinal morphology* (`HP:0000479 <https://hpo.jax.org/app/browse/term/HP:0000479>`_) -- it
is a "grandchild", i.e., *Macular degeneration* is a direct child of &Abnormal macular morphology
(`HP:0001103 <https://hpo.jax.org/app/browse/term/HP:0001103>`_) which in turn is a direct child of *Abnormal retinal
morphology*. Therefore, it is considered to be a non-root distant match. It is assigned a likelihood ratio
of 0.127.


5. :math:`h_i` does not have any non-root common ancestor with any term to which :math:`\mathcal{D}` is annotated.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this case, a heuristic value of 1/100 is assigned for the likelihood ratio.

 .. list-table:: No match (NM)
    :widths: 100
    :header-rows: 1

    * - Example
    * - NM:Specific learning disability[HP:0001328][0.010]


6. phenotypic abnormality :math:`h_i` is explicitly excluded from disease :math:`\mathcal{D}`.
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In the HPO annotation resource, each disease is represented by a list of HPO terms that characterize it together with
metadata including provenance, and in some cases, frequency and onset information.
Some diseases additionally have explicitly excluded terms (there are a total of 921 such annotations in the September
2019 release of the HPOA data). These annotations are used for phenotypic abnormalities that are important for the
differential diagnosis. For instance, Marfan syndrome and Loeys-Dietz syndrome share many phenotypic abnormalities.
The feature *Ectopia lentis* (`HP:0001083 <https://hpo.jax.org/app/browse/term/HP:0001083>`_) is characteristic of
Marfan syndrome but is not found in Loeys-Dietz syndrome. The likelihood ratio for such query terms is assigned an
arbitrary value of :math:`\frac{1}{1000}`, i.e., the ratio for a candidate diagnosis is reduced by a factor of
one thousand if an HPO term is present in the proband that is explicitly excluded from the disease.

 .. list-table:: Excluded in query and present in disease (XP)
    :widths: 100
    :header-rows: 1

    * - Example
    * - XP:Ectopia lentis[HP:0001083][0.001]


If a term is excluded in the query, but not annotated one way of another in the disease, then the likelihood ratio is
calculated without additional heuristics. These query terms generally result in a likelihood ratio near 1 and do not affect
the differential diagnostic ranking much.

 .. list-table:: Excluded in query and not annotated in disease (XA)
    :widths: 100
    :header-rows: 1

    * - Example
    * - XA:Abnormality of alkaline phosphatase activity[HP:0004379][1.008]


On the other hand, if the query includes a negated term that is explicitly excluded in the disease, then the opposite
value is assigned, i.e., the ratio for a candidate diagnosis is increased by a factor of one thousand if an HPO term is
present in the proband that is explicitly excluded from the disease.


 .. list-table:: Excluded in both query and disease (XX)
    :widths: 100
    :header-rows: 1

    * - Example
    * - XX:Trident hand[HP:0004060][1000.000]