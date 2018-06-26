# LR2PG
Likelihood ratio analysis of phenotypes and genotypes

To run LR2PG we need to install a local copy of phenol (version 1.1.0).
```
$ git clone https://github.com/monarch-initiative/phenol.git
$ cd phenol
$ mvn install
```
To run a demo, execute the following commands in the LR2PG directory.


```
$ mvn clean package
$ java -jar target/LR2PG-0.0.1-SNAPSHOT.jar download
$ java -jar target/LR2PG-0.0.1-SNAPSHOT.jar simulate
```


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


Differential diagnosis: Stickler syndrome OMIM:108300.



