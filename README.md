# LR2PG
Likelihood ratio analysis of phenotypes and genotypes


## Likelihood Ratio Analysis of Phenotype/Genotype Data for Genomic Diagnostics

Detailed documentation is available in the ``docs`` subdirectory and will be put onto the public read-the docs site
as soon as we make this repository public. See below for how to make the read-the-docs locally.


## Running LR2PG for the impatient

Please see the read-the-docs page for detailed instructions. For those who just can't wait, the following
steps are sufficient to get LR2PG running on your system and to perform an initial analysis.








## How to generate the documentation


cd to the docs directory. Somehow, get to a sane python installation. Install both
sphinx and ``sphinx_rtd_theme``. Then make the HTML pages. For instance, assuming you have used
Anaconda to create a Python3 environment called ``p3``, ::

```
$ conda activate p3
$ pip install sphinx
$ pip install sphinx_rtd_theme
$ make html
```



