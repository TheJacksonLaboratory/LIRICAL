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

It will output a datafile (called e.g., ``grid_25_cases_precise.txt`` for a simulation with 25 cases for each
run using precision). that can be used to generate a Figure that summarizes
all of the results. For the paper, we should run 100 cases for each run (at least).



R script example
~~~~~~~~~~~~~~~~

To create a graphic, let's extend the following R script (Note the script needs to be adjusted, see
http://www.sthda.com/english/wiki/impressive-package-for-3d-and-4d-graph-r-software-and-data-visualization). ::

    dat <- read.table("grid_2_cases_precise.txt",header=TRUE)
    mat<-data.matrix(dat)
    hist3D(z = mat, scale = FALSE, expand = 0.01, bty = "g", phi = 20,
        col = "#0072B2", border = "black", shade = 0.2, ltheta = 90,
        space = 0.3, ticktype = "detailed", d = 2)

