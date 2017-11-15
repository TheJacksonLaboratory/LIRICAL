#!/bin/sh

if [ ! -e target/LR2PG-0.0.1-SNAPSHOT.jar ]; then
    mvn package
fi

HPO=src/test/resources/hp.obo
ANNOT=src/test/resources/small_phenoannot.tab



## Vida--adjust paths to your computer here,
HNAME=`hostname`
if [ ${HNAME} = "xx" ]; then
    HPO ="/Users/ravanv/Documents/HPO_LR1/LR2PG/HPO/hp.obo"
fi




java -jar target/LR2PG-0.0.1-SNAPSHOT.jar -o ${HPO} -a ${ANNOT}
