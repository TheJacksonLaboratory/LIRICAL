.. _rstrunning:

===============
Running LIRICAL
===============

LIRICAL is a command-line Java tool that runs with Java versions 8 or higher. LIRICAL
can be run both with and without genomic data in form of a VCF file from genome, exome,
or NGS gene-panel sequencing.


Running LIRICAL with a Phenopacket
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL can be run with clinical data (HPO terms) only or with clinical data and a VCF file representing the
results of gene panel, exome, or genome sequencing. The prefered input format is
`Phenopackets <https://github.com/phenopackets>`_, an open standard for sharing disease and phenotype information.
This is a new standard of the `Global Alliance for Genomics and Health <https://www.ga4gh.org/>`_ that
links detailed phenotype descriptions with disease, patient, and genetic information.


.. toctree::
    :maxdepth: 1

    Running LIRICAL with a Phenopacket (only HPO data) <phenopacket-hpo>
    Running LIRICAL with a Phenopacket (VCF and HPO data) <phenopacket-vcf>



Running LIRICAL with a YAML file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The other allowed input format is YAML. The format is designed to be as close as possible to
that of the Exomiser YAML format, but some fields, such as negated HPO terms, as LIRICAL-specific.

.. toctree::
    :maxdepth: 1

    Running LIRICAL with a YAML file (only HPO data) <yaml-hpo>
    Running LIRICAL with a YAML file (VCF and HPO data) <yaml-vcf>


Choosing between YAML and Phenopacket input formats
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

How should users choose between YAML and Phenopackets as an input format?

.. toctree::
    :maxdepth: 1

    yaml-or-phenopacket