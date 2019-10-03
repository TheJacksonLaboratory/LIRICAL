=========
Changelog
=========
-------
v0.9.23
-------
- update to phenopacket-schema version 1.0.0-RC3
- update to Exomiser 12.1.0
- revisions to phenotype likelihood ratio algorithm for non-exact matching
- more documentation

-------
v0.9.22
-------
- update to phenol-1.4.1
- Scoring for features excluded in query and disease.
- Adding option to use Orphanet annotations

-------
v0.9.21
-------
- ClinVar now requires assessment to be counted
- bug-fix of "missing" genes
- fixing SVG format for excluded phenotypes

-------
v0.9.20
-------
- LIRICAL will terminate and emit an error warning if an unknwon HPO term is used in a phenopacket


-------
v0.9.19
-------
- Fixed bug in counting pathogenic alleles (previously variants, not alleles, were being counted).
- Adjusted calculations of phenotype likelihood ratios.

-------
v0.9.18
-------
- Added evolutionary algorithm optimization
- Bug fixes
- YAML file analysis can do geno/pheno or pheno-only analysis

-------
v0.9.16
-------
- Added code to simulate cases with template VCF and phenopackets
- Fixed bug by which YAML file was not correctly setting prefix


-------
v0.9.15
-------
- Changing name of ap to LIRICAL: LIkelihood Ratio Interpretation of Clinical AbnormaLities.
- adding more unit tests

-------
v0.9.14
-------
- update to phenopacket-schema version 0.4.0
- update to Exomiser version 12.0.0
- tweaking code for negative findings

-------
v0.9.13
-------
- Streamlining some of the likelihood ratio code without changing logic
- Adding routine for genotypes with more than 2 called pathogenic variants
- tweaking HTML output
- update to phenol 1.3.3
- adding enforcer plugin

-------
v0.9.11
-------
- Adding support for excluded phenotypes

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
- Adding genotypeExplanation for genotype LR score

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
- adding FreeMarker org.monarchinitiative.lirical.output for analysis of VCF+phenotype data


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
