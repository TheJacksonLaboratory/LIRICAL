.. _rstlirical-tsv:

LIRICAL TSV Output
==================

If LIRICAL is run with the ``-tsv`` option, it will output a tab-separated values (TSV) file with the results for each of the
diagnoses.

TODO-- we still have not finalized the format, but this is what it looks like now. A total of 7424 diagnoses were tested,
whose posttest probability ranged from 45.03% to 0.00% ::

    ! LIRICAL TSV Output
    ! Sample: manuel
    ! Observed HPO terms
    ! Brachydactyly (<a href="https://hpo.jax.org/app/browse/term/HP:0001156">HP:0001156</a>)
    ! Craniosynostosis (<a href="https://hpo.jax.org/app/browse/term/HP:0001363">HP:0001363</a>)
    ! Broad thumb (<a href="https://hpo.jax.org/app/browse/term/HP:0011304">HP:0011304</a>)
    ! Broad hallux (<a href="https://hpo.jax.org/app/browse/term/HP:0010055">HP:0010055</a>)
    rank	diseaseName	diseaseCurie	pretestprob	posttestprob	compositeLR	entrezGeneId	variants
    1	APERT SYNDROME	OMIM:101200	1/7424	45.03%	6,080.832	NCBIGene:2263	chr10:123256215T>G uc001lfg.4:c.518A>C:p.(E173A) pathogenicity:1.0 [HETEROZYGOUS]
    2	PFEIFFER SYNDROME	OMIM:101600	1/7424	45.03%	6,080.832	NCBIGene:2263	chr10:123256215T>G uc001lfg.4:c.518A>C:p.(E173A) pathogenicity:1.0 [HETEROZYGOUS]
    3	ACROCEPHALOPOLYSYNDACTYLY TYPE III	OMIM:101120	1/7424	4.39%	340.497	n/a
    (...)
    7,422	DEAFNESS, AUTOSOMAL DOMINANT 50	OMIM:613074	1/7424	0.00%	0	NCBIGene:407053
    7,423	EDICT SYNDROME	OMIM:614303	1/7424	0.00%	0	NCBIGene:406960
    7,424	PULMONARY FIBROSIS AND/OR BONE MARROW FAILURE, TELOMERE-RELATED, 2; PFBMFT2	OMIM:614743	1/7424	0.00%	0	NCBIGene:7012


