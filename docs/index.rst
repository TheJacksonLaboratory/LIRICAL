LIRICAL: LIkelihood Ratio Interpretation of Clinical AbnormaLities
==================================================================




.. toctree::
   :maxdepth: 2
   :caption: Contents:

   setup
   yaml
   phenopacket
   LR2PG output files <output>
   other commands <other>


LIRICAL
~~~~~~~

This application performs phenotype-driven prioritization of candidate diseases and genes in the setting of
genomic diagnostics (exome or genome) in which the phenotypic abnormalities are described
as `Human Phenotype Ontology (HPO) <http://www.human-phenotype-ontology.org>`_ terms.


Reminder: how to generate the documentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

cd to the docs directory (where this file lives). Somehow, get to a sane python installation. Install both
sphinx and ``sphinx_rtd_theme``. Then make the HTML pages. For instance, assuming you have used
Anaconda to create a Python3 environment called ``p3``, ::

    $ conda activate p3
    $ pip install sphinx
    $ pip install sphinx_rtd_theme
    $ make html



*TODO* Create a public readthedocs page and delete this section prior to submission!
