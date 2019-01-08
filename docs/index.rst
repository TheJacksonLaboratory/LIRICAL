LR2PG: Likelihood ratio analysis of phenotype/genotype data
===========================================================




.. toctree::
   :maxdepth: 2
   :caption: Contents:

   setup
   yaml
   phenopacket
   running
   simulate
   grid
   gt2git
   clinvar

LR2PG
~~~~~

This application performs phenotype-driven prioritization of candidate diseases and genes in the setting of
genomic diagnostics (exome or genome) in which the phenotypic abnormalities of the individual being sequenced
are available as `Human Phenotype Ontology (HPO) <http://www.human-phenotype-ontology.org>`_ terms.


Installation
~~~~~~~~~~~~

Users can go the GitHub page of `LR2PG <https://github.com/TheJacksonLaboratory/LR2PG>`_, clone or download the project,
and build the executable from source with maven, and then test the build. ::

    $ git clone https://github.com/TheJacksonLaboratory/LR2PG.git
    $ cd LR2PG
    $ mvn package
    $ java -jar target/Lr2pg.jar
    $ Usage: <main class> [options] [command] [command options]
      Options:
        -h, --help
          display this help message
      (...)

Alternatively, go to the Releases section of the GitHub page and download the latest precompiled version of LR2PG.

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
