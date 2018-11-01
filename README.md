# LR2PG
Likelihood ratio analysis of phenotypes and genotypes

## Installing and setting up LR2PG
LR2PG makes use of the phenotype analysis library phenol, which needs to be installed locally.
Currently, LR2PG uses version 1.3.0-SNAPSHOT.

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
$ java -jar target/LR2PG-0.4.6.jar download
```
If you want to download to a different location, use the -d <path> argument while downloading and in all of
the subsequent steps.

LR2PG makes use of the Exomiser data resources, which need to be downloaded from the Exomiser FTP site
(https://data.monarchinitiative.org/exomiser/latest/).  For instance, to do the analysis with the hg38
genome assembly, download the data file 1805_hg38.zip  and unzip it. The Exomiser will first calculate
the expected background frequency of predicted pathogenic variants and write this to a file that will
be used in subsequent steps (this will take about an hour on a typical laptop).

```
$ java -jar target/LR2PG-0.4.6.jar gt2git TODO
```

To run a demo, execute the following commands in the LR2PG directory.





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