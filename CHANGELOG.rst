=========
Changelog
=========

-------------------
latest
-------------------

-------------------
v2.1.0 (2025-04-23)
-------------------

- Support the most recent Exomiser database format (``2406`` or newer)
- Fix bug with Orpha disease to gene association parsing
- Allow to select target diseases with the ``--target-diseases`` CLI option
- Add `ENSEMBL` and `REFSEQ_CURATED` transcript databases
- Download Jannovar databases from Zenodo (except of UCSC transcripts)
- Update dependencies
  - Jannovar `>=0.35`
  - HTSJDK `>=4.0`
  - Jackson libs `2.18.3`
  - Freemarker `>=2.3.32`
  - commons-io `>=2.14.0`
  - commons-compress `>=1.26.0`
  - commons-csv `>=1.10.0`
  - commons-lang3 `>=3.12.0`
  - logback-classic `>=1.4.14`

-------------------
v2.0.2 (2024-05-01)
-------------------

- Use `en_US` locale for running tests
- Remove an extra `\t` from the TSV header

-------------------
v2.0.1 (2024-04-16)
-------------------

- Bug fix - When running with a VCF file, the TSV report will only include the diseases with a mutation
  in the associated gene, *unless* the ``--sdwndv`` is provided via CLI. Previously, the TSV reports included
  the diseases with no mutation regardless of the ``--sdwndv`` option.

-------------------
v2.0.0 (2023-12-29)
-------------------

- Add ``prioritize`` command for running LIRICAL entirely from CLI
- Support running analysis starting from both ``v1`` and ``v2`` phenopacket versions
- Simplify the YAML input format
- Enhance HTML report, add `JSON` output format
- Split the codebase into several modules
- Require Java 17 or better
- Host documentation and API docs on github.io

-------------------
v1.3.3 (2021-05-14)
-------------------
- Fixed output directory option for YAML input format

-------------------
v1.3.2 (2021-04-09)
-------------------
- Fixed null pointer error in YAML output

------
v1.3.1
------
- Update download URL for phenotype.hpoa
- Update versions of multiple plugins/dependencies
- Add option to simultaneously output HTML and TSV
- maven wrapper

------
v1.3.0
------
- Switch to picocli command line interface

------
v1.2.0
------
- update to phenol 1.6.0 (note minor change to phenotype.hpoa format with '#' starting header line

------
v1.1.0
------
- bugfix -- NCBI Gene ID ingest
- update to phenol 1.5.0

------
v1.0.3
------
- updated to phenol 1.4.2
- added output of UCSC links to visualize variants
- added some unit tests and fixed a few minor bugs

------
v1.0.1
------
- updated to phenol 1.4.2
- updated hpo annotation download URL

------
v1.0.0
------
- Update to phenopacket-schema version 1.0.0
- Improvements to HTML output



----------
v1.0.0-RC2
----------
- fixed bug in YAML output
- removing Ensembl option
- Adding sparkline graphic to show posttest probability
- various bug fixes

----------
v1.0.0-RC1
----------
- Preparing first release
- Adding posttest probability SVG to HTML output

-------
v0.9.24
-------
- fixed bug in accessing the background data files within the JAR resource
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
