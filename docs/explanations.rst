.. _rstexplanations:

LIRICAL: How does it work?
==========================


In medical genetics and related fields, `Human Phenotype Ontology (HPO) <https://hpo.jax.org/app/>`_ analysis has become
one of the standard methods for diagnostics. Current algorithms such as
`Exomiser <https://www.ncbi.nlm.nih.gov/pubmed/24162188>`_ and
`Phenomizer <https://www.ncbi.nlm.nih.gov/pubmed/19800049>`_ use a variety of semantic
and statistical approaches to prioritize the typically long lists of genes with candidate pathogenic variants, but do not
provide robust estimates of the strength of the predictions beyond the placement in a ranked list, nor do they provide
measures of how much any individual phenotypic observation has contributed to the prioritization result.
LIRICAL exploits the clinical likelihood ratio framework to provide an estimate of the posttest probability of candidate
diagnoses, the likelihood ratio for each observed HPO phenotype, and the predicted pathogenicity of observed variants.

LIRICAL makes use of the clinical likelihood ratio (LR) framework to phenotype-driven genomic diagnostics that addresses
these shortcomings. The LR is defined as  the probability of a given test result in an individual with the target disorder
 divided by the probability of that same result in an individual without the target disorder.  LIRICAL can be run in
a phenotype-only mode or can be run to analyze both phenotype and genotype findings (e.g., from Exome or Genome sequencing).
The following pages explain the algorithmic details.


.. toctree::
   :maxdepth: 1
   :caption: Contents:

   phenotype-score



