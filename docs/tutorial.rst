.. _rstsetup:


Tutorial
========

This tutorial shows how to use LIRICAL to evaluate an exome.


Setup
~~~~~
Follow the instructions on :ref:`rstsetup` to install the Exomiser database and set up LIRICAL.


The data
~~~~~~~~

We have simulated an exome VCF file by adding a disease associated variant to
aVCF file derived from project.NIST.hc.snps.indels.NIST7035.vcf.
A disease-associated mutation in the TGFBR2 gene (see Patient 4 in
`Cao et al., 2018 <https://www.ncbi.nlm.nih.gov/pubmed/?term=30101859>`_) was spiked into the VCF file.


Download the VCF file from `Figshare <https://figshare.com/account/articles/10636556>`_.

Creating a phenopacket
~~~~~~~~~~~~~~~~~~~~~~

Here is an excerpt of the text that described patient 4 in the above cited article.
::

    Patient  4  is  a  9-year-old  girl.  She  was  clinically  diagnosed  with  suspected
    Marfan syndrome  according  to  the  first  impression.  She  was  144  cm  tall and
    weighed  24  kg.  Her  father  was  176  cm  tall  and  weighed53  kg.  The  phenotypes
    of  this  patient  include strabismus,refractive  error,  pectus  carinatum,  scoliosis,
    arachnodactyly,  and  camptodactyly. The patient's main cardiovascular abnormalities
    were  Sinus  of  Valsalva  aneurysm,  aortic  rootdilation, aortic regurgitation,
    atrial septal defect,  patent foramen  ovale,  pulmonary  artery  dilatation,  and
    tricuspid valve  prolapse  with regurgitation.  Craniofacial  abnormalities  of  the
    patient include   bifid   uvula,   malar   hypoplasia,   and   micrognathia.

Use the `PhenopacketGenerator <https://github.com/TheJacksonLaboratory/PhenopacketGenerator>`_
to create a Phenopacket. Enter the corresponding data (you can use arbitrary Phenopacket and proband IDs)
and paste the clinical description into the text-mining window of PhenopacketGenerator. Then, select the
location of the VCF file that you saved in the previous step. You can now export the phenopacket. Use the
filename ``LDS2.json`` (or choose another name and adjust the following command accordingly).

Running LIRICAL
~~~~~~~~~~~~~~~

Run LIRICAL as follows.
::

    $ java -jar LIRICAL.java -p LDS2.json -x LDS2

Viewing the results
~~~~~~~~~~~~~~~~~~~

The above command will create a new file called ``LDS2.html`` (the ``-x`` option controls the prefix of the output file).
Open this file in a web browser.

TODO -- add graphics of the sparkline and two of the sections showing a good and a bad differential ldiagnosis
and explain how to use the output page.