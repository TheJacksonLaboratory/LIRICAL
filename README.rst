#####
LR2PG
#####

Likelihood ratio analysis of phenotypes and genotypes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Detailed documentation is available in the ``docs`` subdirectory and will be put onto the public read-the docs site
as soon as we make this repository public. See below for how to make the read-the-docs locally.


Building LR2PG from source
~~~~~~~~~~~~~~~~~~~~~~~~~~
LR2PG relies on some classes from the
`Exomiser <https://monarch-exomiser-web-dev.monarchinitiative.org/exomiser/>`_.
To build LR2PG, we need to install the Exomiser code base locally. To do so, we cone the code and install
using the maven system. ::

    $ git clone https://github.com/exomiser/Exomiser.git
    $ cd Exomiser
    $ mvn install

This will install the Exomiser into your local maven repository and make it available for the maven build of LR2PG.
Following this step, we clone the LR2PG repository and build it with maven. ::

    $ git clone https://github.com/TheJacksonLaboratory/LR2PG.git
    $ cd LR2PG
    $ mvn package

This will create an executable jar file in the ``target`` subdirectory. In the following, we move this file to the
current working directory and check the build.

Obtaining Exomiser data files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
LR2PG makes use of the Exomiser database. We need to download the relevant files and tell LR2PG where to find them.
LR2PG currently uses version 12.0.0 of Exomiser. You will need to clone this version to build LR2PG. If necessary,
adjust the version number with the pull down menu of the Exomiser GitHub page.

The Exomiser data files are only required for VCF analysis.

1. Download the Exomiser database from https://monarch-exomiser-web-dev.monarchinitiative.org/exomiser/download.
Make sure to go to the subdirectory called 'latest' (you may need to scroll the bar on the right to find this
directory). Download either 1811_hg19.zip (for the hg19, i.e., GRC_37 human genome assembly) or 1811_hg38.zip  for
hg38, and unzip the file. You do not need the 1811_phenotype.zip file for LR2PG. The ``README.md`` file in the Exomiser
download directory has some additional information.

Java version
~~~~~~~~~~~~
LR2PG currently is built and runs with Java 8. We have been able to build and run the code under Java 11,
but do not currently provide a build file for this.


This is a useful website for Ubuntu users: http://ubuntuhandbook.org/index.php/2018/11/how-to-install-oracle-java-11-in-ubuntu-18-04-18-10/


Running LR2PG
~~~~~~~~~~~~~
After build LR2PG as above, try the following commands to confirm that the build process worked. ::


    $ mv target/Lr2pg.jar .
    $ java -jar Lr2pg.jar -h
        Usage: java -jar Lr2pg.jar [options] [command] [command options]
        Options:
            -h, --help
        display this help message
    (...)


Download files with LR2PG
~~~~~~~~~~~~~~~~~~~~~~~~~

LR2PG needs to download files from the HPO database and other sources. It does so automatically if your computer is connected to the internet.

```
$  java -jar Lr2pg.jar download
```

This command will download four files and copy them  to a new subdirectory called ``data`` that later commands will use.

4. Run LR2PG

We can run a test phenopacket file that is included in the LR2PG distribution
```
$ java -jar Lr2pg.jar phenopacket -p src/test/resources/pfeifferNoVcf.json
```

This will run LR2PG with the phenotype-only algorithm (i.e., no VCF file with exome or genome data is used). To run
the ``phenopacket`` command with a VCF file, indicate the path of the VCF file in the phenopacket and also pass the
``-e`` flag with the location of the Exomiser database.

5. YAML
It may be easier to use one of the YAML configuration files to run LR2PG. See the examples in the src/test/resources/yaml directory.
Then run the ``vcf`` command

```
$ java -jar Lr2pg.jar vcf -y src/test/resources/yaml/demo1.yml
```






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



