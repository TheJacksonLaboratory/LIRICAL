The simulate command
====================

We developed code to perform simulations that we used to produce some of the results in the manuscript. This
code is not needed to run LR2PG, but is left here for users who may wish to reproduce or extend the simulation
results.

This command runs one simulation with one particular set of parameters. It is related to the
grid command which runs 100 different combinations.

This command has the following options. ::

     Options:
          -d, --data
            directory to download data (default: data)
            Default: data
          -i, --imprecion
            Use imprecision?
            Default: false
          -c, --n_cases
            Number of cases to simulate
            Default: 25
          -h, --n_hpos
            Number of HPO terms per case
            Default: 5
          -n, --n_noise
            Number of noise terms per case
            Default: 1


The command is run as ::

    $ java -jar Lr2pg.jar simulate [options]


It will print a summary of the results to standard out, for instance, ::

    Simulation of 25 cases with 5 HPO terms, 1 noise terms. Imprecision: false
    Rank=1: count:19 (76.0%)
    Rank=2: count:2 (8.0%)
    Rank=3: count:1 (4.0%)
    Rank=8: count:1 (4.0%)
    Rank=11-20: count:0 (0.0%)
    Rank=21-30: count:0 (0.0%)
    Rank=31-100: count:1 (4.0%)
    Rank=101-...: count:1 (4.0%)


