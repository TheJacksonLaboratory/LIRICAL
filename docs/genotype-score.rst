.. _rstgenotype-score:

LIRICAL's Genotype Score
========================

We can estimate the pathogenicity of a variant on the basis of a
computational pathogenicity score that ranges from 0 (predicted benign) to 1 (maximum pathogenicity prediction). LIRICAL
uses the pathogenicity score prediction of
`Exomiser <https://www.ncbi.nlm.nih.gov/pubmed/26562621>`_.  Our model depends on the assumed mode of inheritance of the
disease, and provides an estimate likelihood ratio for the observed genotype. For example, we expect two
pathogenic alleles in an autosomal recessive disease and one in an autosomal dominant disease. Our model takes
into account the expected frequency of seeing predicted pathogenic variants in the population. Genes known to carry few
common functional variants in healthy individuals may be judged more likely to cause certain kinds of disease than genes
known to carry many such variants (`Petrovski et al., 2013 <https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3749936/>`_).

LIRICAL's model provides an integrated score for each gene that assesses the observed genotype, comparing its probability
given that a disease associated with the gene is present in the proband vs. the probability that the genotype is unrelated
to the clinical manifestations observed in the proband. See the manuscript for algorithmic details.


