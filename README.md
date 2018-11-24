# LR2PG
Likelihood ratio analysis of phenotypes and genotypes

## Installing and setting up LR2PG
LR2PG makes use of the phenotype analysis library phenol, which needs to be installed locally.
Currently, LR2PG uses version ``1.3.0-SNAPSHOT``.

```
$ git clone https://github.com/monarch-initiative/phenol.git
$ cd phenol
$ mvn install
```
To compile LR2PG, run the following command

```
$ mvn clean package
```
 LR2PG also uses a number of other files, which it
can automatically download using the download command (use the --overwrite flag to download fresh copies,
otherwise, it only downloads files if not present). By default, LR2PG downloads to a new directory
called data, which will be created as a subdirectory in the directory from which LR2PG is run.
```
$ java -jar target/LR2PG-0.5.4.jar download
```
If you want to download to a different location, use the -d <path> argument while downloading and in all of
the subsequent steps.

## Initializing the background frequency file


LR2PG makes use of the Exomiser data resources, which need to be downloaded from the Exomiser FTP site
(https://data.monarchinitiative.org/exomiser/latest/).  For instance, to do the analysis with the hg38
genome assembly, download the data file 1805_hg38.zip  and unzip it. The Exomiser will first calculate
the expected background frequency of predicted pathogenic variants and write this to a file that will
be used in subsequent steps (this will take about an hour on a typical laptop).

```
$ java -jar target/LR2PG-0.5.4.jar gt2git -m <mvstore> -j <jannovar> -g <genome>
```
In this command, ``mvstore`` refers to the path of the Exomiser data store, e.g., ``1802_hg19_variants.mv.db``;
``jannovar`` refers to the path of the Jannovar transcript data file, e.g., ``1802_hg19_transcripts_refseq.ser``;
and ``genome`` refers to the genome build. Use the corresponding genome build, ``hg19`` or ``hg38``.

This command will output the background frequency file to the data direcotry (by default, a subdirectory call ``data`` in the
current working direcgtory; the data directory can also be specified with the ``-d`` flag). THe location of this file must be
specified in the YAML configuration file to run the prioritization function.

For the final release, we will add the background files for hg19 and hg38 to the distribution, but let's finish testing
prior to doing that.

## Running LR2PG
To run the VCF prioritization tool of LR2PG, create a YAML configuration file. There are several examples in the ``resources/yaml``
directory. Then, run the program with the following command.
```
$ java -jar target/LR2PG-0.5.4.jar vcf -y <yaml>
```

Please consult the ``src/main/resources/yaml`` directory for several example YAML files.

## Clinvar pathogenicity scores
To get the exomiser pathogenicity scores for all ClinVar variants, we can run LR2PG as follows
```
$ java -jar target/LR2PG-0.4.6.jar gt2git
  -j
  /home/peter/data/exomiser/1802_hg19/1802_hg19_transcripts_refseq.ser
  --mvstore
  /home/peter/data/exomiser/1802_hg19/1802_hg19_variants.mv.db
  -g
  hg19
  --clinvar

```
The program will output a file with two columns (it runs in about one hour on a good desktop).
```
1.0     PATHOGENIC
0.0     BENIGN
0.963   LIKELY_BENIGN
0.742   BENIGN_OR_LIKELY_BENIGN
0.0     BENIGN
1.0     PATHOGENIC
0.0     BENIGN_OR_LIKELY_BENIGN
```
The first column is the Exomiser pathogenicity score and the second column is the ClinVar interpretation.
There is thus one line for each variant in ClinVar that has a PATHOGENIC or a BENIGN classification.
We can plot this data with the following R script. Note that we combine the three
subcategories PATHOGENIC, LIKELY_PATHOGENIC, PATHOGENIC_OR_LIKELY_PATHOGENIC into a
single category ("pathogenic") and likewise for BENIGN.

The following R script can be used to plot the data (change the current working directory to the
directory with the outputfile, ``clinvarpath.txt``, which by default is the same directory
in which the program is executed).


```
library(ggplot2)
library(ggsci)

dat <- read.table("clinvarpath.txt",header=FALSE)
colnames(dat) <- c("path","category")
dat$cat<- ifelse((dat$category=='BENIGN' | dat$category=='LIKELY_BENIGN' | dat$category=='BENIGN_OR_LIKELY_BENIGN'),"benign","pathogenic")

p2 = ggplot(dat, aes(x=path, fill=cat)) +
  geom_histogram(colour = "black",  position = "dodge",binwidth=0.01) +
  #geom_histogram(aes(y = ..count..), binwidth = 0.2,   position="identity", alpha=0.9) +
  #scale_x_continuous(name = "Pathogenicity",
                   #  breaks = seq(-3, 3, 1),
                    # limits=c(-3.5, 3.5)) +
  theme_bw() + theme(text = element_text(size=20),
                     axis.text = element_text(size=20, hjust=0.5),
                     axis.title = element_text(size=20),
                     legend.title = element_blank(),
                     legend.position="top")
p2_npg = p2 + scale_fill_npg()
p2_npg
```

This is only needed for testing and we may remove it from the main distribution.


## Other subprograms
Currently LR2PG supports several other functions that may be useful for testing and debugging. We will probably
remove them when we finalize the code for submission.





To run the phenogeno simulation, use the following syntax
```
java -jar Lr2pg.jar phenogeno --disease <id> --geneid <string> \
		--term-list <string> [-d <directory>] [--varcount <int>] [--varpath <double>]
	--disease <id>: id of disease to simulate (e.g., OMIM:600321)
	-d <directory>: name of directory with HPO data (default:"data")
	--svg <file>: name of output SVG file (default: test.svg)
	--geneid <string>: symbol of affected gene
	--term-list <string>: comma-separated list of HPO ids
	--varcount <int>: number of variants in pathogenic bin (default: 1)
	--varpath <double>: mean pathogenicity score of variants in pathogenic bin (default: 1.0)
```

For example, to run a simulation of Marfan syndrome, we would run the following commands with the phenotypes

* HP:0002751 (Kyphoscoliosis)
* HP:0001166 (Arachnodactyly)
* HP:0004933 (Ascending aortic dissection)
* HP:0001083 (Ectopia lentis)
* HP:0003179 (Protrusio acetabuli)

```
java -jar Lr2pg.jar phenogeno --disease OMIM:154700  --geneid 2200 \
		--term-list HP:0002751,HP:0001166,HP:0004933,HP:0001083,HP:0003179 \
		 --varcount 1 --varpath 1.0
```


To run a simulation of Hyperphosphatasia With Mental Retardation Syndrome 1 OMIM:239300 

* HP:0003155 Elevated alkaline phosphatase
* HP:0001792 Small nail
* HP:0001252 Muscular hypotonia
* HP:0009882 Short distal phalanx of finger
* HP:0001249 Intellectual disability
* HP:0003155 Elevated alkaline phosphatase


HP:0003155,HP:0001792,HP:0001252,HP:0009882,HP:0001249,HP:0003155


```
java -jar Lr2pg.jar phenogeno --disease OMIM:239300  --geneid 55650 \
		--term-list HP:0003155,HP:0001792,HP:0001252,HP:0009882,HP:0001249,HP:0003155 \
		--varcount 2 --varpath 1.0
```
	



To run a simulation of an "unsolved case" (P30 from our STM paper)

HP:0002790,HP:0001252,HP:0001510,HP:0000252,HP:0005469,HP:0000325,HP:0000337,HP:0008551,HP:0000369,HP:0000160,HP:0000219,HP:0010282,HP:0000581
  



* HP:0002790 Neonatal breathing dysregulation 
* HP:0001252 Muscular hypotonia 
* HP:0001510 Growth delay 
* HP:0000252 Microcephaly 
* HP:0005469 Flat occiput 
* HP:0000325 Triangular face 
* HP:0000337 Broad forehead 
* HP:0008551 Microtia 
* HP:0000369 Low-set ears 
* HP:0000160 Narrow mouth 
* HP:0000219 Thin upper lip vermilion 
* HP:0010282 Thin lower lip vermilion 
* HP:0000581 Blepharophimosis 