.. _rstgenotype-score:

LIRICAL's Genotype Score
========================

We can estimate the pathogenicity of a variant on the basis of a
computational pathogenicity score that ranges from 0 (predicted benign) to 1 (maximum pathogenicity prediction).
LIRICAL uses the pathogenicity score prediction of `Exomiser <https://www.ncbi.nlm.nih.gov/pubmed/26562621>`_.
Our model depends on the assumed mode of inheritance of the disease,
and provides an estimate likelihood ratio for the observed genotype.
For example, we expect two pathogenic alleles in an autosomal recessive disease and one in an autosomal dominant disease.
Our model takes into account the expected frequency of seeing predicted pathogenic variants in the population.
Genes known to carry few common functional variants in healthy individuals may be judged more likely
to cause certain kinds of disease than genes known to carry many such variants
(`Petrovski et al., 2013 <https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3749936/>`_).

LIRICAL's model provides an integrated score for each gene that assesses the observed genotype, comparing its probability
given that a disease associated with the gene is present in the proband vs. the probability that the genotype is unrelated
to the clinical manifestations observed in the proband. See the manuscript for algorithmic details.


Related analysis options
^^^^^^^^^^^^^^^^^^^^^^^^

The following genotype configuration options can be set on top of the phenotype-only analysis.

.. _rsttx-dbs:

Transcript databases
~~~~~~~~~~~~~~~~~~~~

LIRICAL can use transcript data from four transcript databases:

* `RefSeq <https://www.ncbi.nlm.nih.gov/refseq/>`_ - including curated transcripts (``NM_``)
  as well as the transcripts that are based on gene predictions (``XM_``)
* RefSeq curated - including curated transcripts (``NM_``) only,
  and *NOT* the transcripts that are based on gene predictions (``XM_``)
* `UCSC <http://genome.ucsc.edu/>`_
* `Ensembl <https://www.ensembl.org/info/data>`_

RefSeq transcripts are used by default.


.. _rstbg-var-freqs:

Background variant frequencies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL uses a background frequency file that records the freqeuncy of predicted pathogenic variants
in protein-coding genes (as estimated from gnomAD data). By default, LIRICAL will use pre-fabricated
files for this (that are included in the ``src/main/resources/background`` directory). This is recommended
for most users. If you create your own background file, then you can use it with the ``-b`` option, that should
then indicate the path to a non-default background frequency file.

.. _rstglobal-mode:

Global mode
~~~~~~~~~~~

By default, LIRICAL ranks candidate genes for which at least one pathogenic allele is present in the VCF file.
However, LIRICAL can also be run in a ``--global`` mode in which diseases are ranked irrespective of
whether a disease gene is known for a disease or whether the gene is found to have a pathogenic allele or not.
