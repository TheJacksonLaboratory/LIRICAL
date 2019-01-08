The grid command
================

We developed code to perform simulations that we used to produce some of the results in the manuscript. This
code is not needed to run LR2PG, but is left here for users who may wish to reproduce or extend the simulation
results.

Grid search
===========

This command runs simulations with different combinations of parameters.
It varies the number of HPO terms per simulated case from 1 to 10. For
each term number, it varies the number of random (unrelated) terms from
1 to 4. It runs the entire simulation with and without imprecision (imprecision
means that we replace each non-noise HPO term with its parent, i.e., with
a less precise term).

The code will provide a running output of its work to standard OUT,  and will take
up to several hours to run depending on the hardware. ::

    terms: 1; noise terms: 0; percentage at rank 1: 6.00
    terms: 1; noise terms: 1; percentage at rank 1: 1.00
    (...)
    terms: 10; noise terms: 3; percentage at rank 1: 76.00
    terms: 10; noise terms: 4; percentage at rank 1: 72.00

It will output an R script that can be used to generate a Figure that summarizes
all of the results.