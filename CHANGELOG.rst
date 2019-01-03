=========
Changelog
=========
------
v0.9.3
------
- Added phenopacket import class
- fixed bug with genotype calculation for genes with multiple modes of inheritance
- Updated CLI to use picocli

------
v0.9.2
------
- Implemented Exomiser-style pathogenicity score for the analysis.

------
v0.9.1
------
- Implemented likelihood ratio for variants with known ClinVar pathogenic status and corresponding tests with mockito

------
v0.9.0
------
- First pre-release version
- Adding explanation for genotype LR score

------
v0.5.4
------
- improved HTML template, adding some CSS and structure
- improved display of information for differential diagnosis with no variants or no known disease gene

------
v0.5.2
------
- adding functionality from previous G2GIT project
- removing SpringBoot code
- adding FreeMarker org.monarchinitiative.lr2pg.output for analysis of VCF+phenotype data


------
v0.4.4
------
- refactored as SpringBoot application

------
v0.4.0
------
- refactored to use phenol 1.1.0
- adding code to simulate genotypes

------
v0.3.2
------
- refactored TermId to remove superfluous interface and renamed ImmutableTermId to TermId
- refactored TermSynonym to remove superfluous interface
- adding support for alt term ids to Owl2OboTermFactory (class renamed from GenericOwlFactory)
- adding support for database_cross_reference (usually PMID, ISBM, HPO, or MGI--added to term definitions)
- refactoring to use phenol v.1.0.2

------
v0.2.2
------
- Grid search over simulation parameters

------
v0.2.1
------
- Finished version one of phenotype LR scheme

------
v0.0.3
------
- fixed error with finding TermId in Disease2TermFrequency
