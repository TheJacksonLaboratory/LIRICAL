Running LR2PG
=============

Use either the YAML or the phenopacket method to pass G2GIT the necessary parameters and run it as described.

Depending on you computer, LR2PG will run from about 15 to 60 seconds, or longer if a whole-genome file is used
as input. This document will explain the HTML output as well as the format of the TSV output.


TODO -- wait until more dust has settled to write this.








(All of the following text needs to be revised!)

For example, to run a simulation of Marfan syndrome, we would run the following commands with the phenotypes

* HP:0002751 (Kyphoscoliosis)
* HP:0001166 (Arachnodactyly)
* HP:0004933 (Ascending aortic dissection)
* HP:0001083 (Ectopia lentis)
* HP:0003179 (Protrusio acetabuli)

The command would be. ::

    java -jar Lr2pg.jar phenogeno --disease OMIM:154700  --geneid 2200 \
        --term-list HP:0002751,HP:0001166,HP:0004933,HP:0001083,HP:0003179 \
        --varcount 1 --varpath 1.0

To run a simulation of Hyperphosphatasia With Mental Retardation Syndrome 1 OMIM:239300

* HP:0003155 Elevated alkaline phosphatase
* HP:0001792 Small nail
* HP:0001252 Muscular hypotonia
* HP:0009882 Short distal phalanx of finger
* HP:0001249 Intellectual disability
* HP:0003155 Elevated alkaline phosphatase


HP:0003155,HP:0001792,HP:0001252,HP:0009882,HP:0001249,HP:0003155


The command is. ::

    java -jar Lr2pg.jar phenogeno --disease OMIM:239300  --geneid 55650 \
        --term-list HP:0003155,HP:0001792,HP:0001252,HP:0009882,HP:0001249,HP:0003155 \
        --varcount 2 --varpath 1.0






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