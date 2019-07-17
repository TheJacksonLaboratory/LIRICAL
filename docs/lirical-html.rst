.. _rstlirical-html:

LIRICAL HTML Output
===================




The HTML Output
~~~~~~~~~~~~~~~

The HTML output page begins with a summary of the sample name and a list of the `HPO <http:\\www.human-phenotype-ontology.org>`_ terms
used to run the program. By default, LIRICAL shows a detailed output only for diseases whose posttest probability is
calculated to be more than a threshold whose default value is 1% (but which can be adjusted using the ``-t`` command line
argument). For example, the following figures show an example in which Pfeiffer syndrome was ranked as the second
candidate according to its posttest probability of 45%. One heterozygous variant in the *FGFR2* gene was identified whose predicted
pathogenicity score was 1.0. This corresponds to a genotype likelihood ratio score of 10^{0.63} for this gene.
The diagnram then shows the contribution of each of the HPO terms as well as the genotype to the combined likelihood
ratio for this diagnosis. In this case, Broad hallux, Broad thumb, and Craniosynostosis have a contribution of
100-fold or more (note that the X-axis shows the base-10 logarithm of the likelihood ratios). Brachydactyly shows
a negative contribution because it is not associated with the disease (its likelhood ratio is shown in red).


.. figure:: _static/pfeiffer-output.png

