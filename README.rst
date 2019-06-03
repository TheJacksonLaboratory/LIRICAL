#######
LIRICAL
#######

LIkelihood Ratio Interpretation of Clinical AbnormaLities
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
LIRICAL is designed to provide clincially interpretable computational analysis of phenotypic
abnormalities (encoded using the `Human Phenotype Ontology <http://www.human-phenotyope-ontology.org>`_),
optionally combined with an analysis of variants and genotypes if a VCF file is provided with the
results of diagnostic gene panel, exome, or genome sequencing.


Detailed documentation is available



This is a useful website for Ubuntu users: http://ubuntuhandbook.org/index.php/2018/11/how-to-install-oracle-java-11-in-ubuntu-18-04-18-10/


Running LIRICAL
~~~~~~~~~~~~~~~
After build LIRICAL as above, try the following commands to confirm that the build process worked. ::


    $ mv target/LIRICAL.jar .
    $ java -jar LIRICAL.jar -h
        Usage: java -jar LIRICAL.jar [options] [command] [command options]
        Options:
            -h, --help
        display this help message
    (...)


Download files with LIRICAL
~~~~~~~~~~~~~~~~~~~~~~~~~~~

LIRICAL needs to download files from the HPO database and other sources. It does so automatically if
your computer is connected to the internet. ::

    $  java -jar LIRICAL.jar download


This command will download four files and copy them  to a new subdirectory called ``data`` that later commands will use.

Runing LIRICAL with a Phenopacket
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We can run a test phenopacket file that is included in the LIRICAL distribution. ::

    $ java -jar Lr2pg.jar phenopacket -p src/test/resources/pfeifferNoVcf.json


This will run LR2PG with the phenotype-only algorithm (i.e., no VCF file with exome or genome data is used). To run
the ``phenopacket`` command with a VCF file, indicate the path of the VCF file in the phenopacket and also pass the
``-e`` flag with the location of the Exomiser database.

Running LIRICAL with a YAML configuration file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Alternatively, you can run LIRICAL with a YAML configuration file. See the examples
in the src/test/resources/yaml directory.

Then run the ``vcf`` command. ::

    $ java -jar Lr2pg.jar vcf -y src/test/resources/yaml/demo1.yml



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



