=========
Changelog
=========

-------
v0.9.10
-------
- Compiling with Java 11
- Simplifying and extending metadata and display in HTML

------
v0.9.8
------
- Improved HTML/CSS formating
- Simplifying command-line interface and adding Q/C checks for the existence of files with better error messages.

------
v0.9.7
------
- Adding functionality for running LR2PG from a Phenopacket with VCF file path
- now using phenol-1.3.2 from maven central
- adding Q/C code for input files.

------
v0.9.4
------
- Updating to phenol-1.3.2-SNAPSHOT
- Allow ingest of OMIM/DECIPHER specifically to avoid redundant disease classes.
- Improve HTML output template
- Updated CLI to use JCommander

------
v0.9.3
------
- Added phenopacket import class
- fixed bug with genotype calculation for genes with multiple modes of inheritance

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
