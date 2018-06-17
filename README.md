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




