#!/bin/sh

if [ ! -e target/LR2PG-0.0.1-SNAPSHOT.jar ]; then
    mvn package
fi

HPO=/home/robinp/data/hpo/hp.obo



HNAME=`hostname`
#if [ $HNAME eq "xx" ]; then
#    HPO = "vidas paths"
#fi




java -jar target/LR2PG-0.0.1-SNAPSHOT.jar
