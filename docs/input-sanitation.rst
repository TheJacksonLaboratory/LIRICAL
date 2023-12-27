.. _rst-input-sanitation:

=========================
Analysis input validation
=========================

LIRICAL performs Q/C checks and sanitation before running the analysis.

Here we summarize the requirements and checks performed on all sections of the analysis input.

Analysis requirements
^^^^^^^^^^^^^^^^^^^^^

Here we summarize the requirements of inputs that LIRICAL needs for the analysis.

Sample identifier
~~~~~~~~~~~~~~~~~

Sample identifier MUST be provided if the analysis is run with a multi-sample VCF file. Otherwise, LIRICAL is unable
to choose the variant genotypes.
The identifier is *optional* if running a phenotype-only analysis or with a single-sample VCF file,
where LIRICAL uses the identifier found in the VCF file.

The analysis will stop if run with multi-sample VCF file and the identifier is not available,
or if the provided identifier is not found in the VCF file (applies to single-sample VCFs as well).


Phenotypic features
~~~~~~~~~~~~~~~~~~~

LIRICAL uses a set of phenotypic features that were observed or specifically excluded in the subject to prioritize
the diseases and several checks are applied to mitigate common errors ensure correctness of the analysis.

The checks focus on the following:

- At least one present or excluded HPO term is provided.
- All phenotypic features are formatted as *Compact Uniform Resource Identifiers* (CURIEs), such as ``HP:0001250``
  for *Seizure*. A valid CURIE consists of a prefix (e.g. ``HP``), delimiter (``:`` or ``_``), and id (e.g. ``0001250``).
- The CURIEs are *unique*, i.e. used at most once.
- The CURIEs correspond to identifiers of *current* or *obsolete* HPO terms.
- The HPO terms are descendants of `Phenotypic abnormality <https://hpo.jax.org/app/browse/term/HP:0000118>`_ branch.
- The HPO terms are logically consistent:

    - The subject is not annotated with an HPO term in observed and excluded state at the same time.
    - The subject is not annotated with an observed HPO term and its observed or excluded ancestor.
    - The subject is not annotated with an excluded HPO term and its excluded ancestor.

Age
~~~

LIRICAL does not use the age of the subject at the moment. However, if set, the age must be formatted
as ISO8601 duration. For instance ``P1Y8M`` for 1 year and 8 months of age.

Sex
~~~

The sex must be provided as one of {``MALE``, ``FEMALE``, ``UNKNOWN``}. If the input is not parsable,
``UNKNOWN`` is used by default.

VCF file
~~~~~~~~

The path to VCF file can be provided via CLI or through phenopacket/YAML file. The path must point to a file
that is readable by the user running the LIRICAL process.


Validation policy
^^^^^^^^^^^^^^^^^

LIRICAL enforces the requirements depending on the validation policy. There are three validation policies:

- *MINIMAL*
- *LENIENT*
- *STRICT*

with increasing sanity requirements.

The input validation results are always logged in the output. The log includes the following line if the input is OK::

  Input sanitation found no issues

Alternatively, the issues are logged to the terminal. For instance::

  Found issues 0 errors and 1 warnings
   Errors 😱
   - Sample must not be annotated with Clonic seizure [HP:0020221] while its ancestor
     Seizure [HP:0001250] is excluded. Resolve the logical inconsistency by choosing
     one of the terms.
   Warnings 😧
   - Sample should not be annotated with Patent foramen ovale [HP:0001655] and its ancestor
     Atrial septal defect [HP:0001631]. Remove Atrial septal defect [HP:0001631]
     from the phenotype terms.

The issues are classified as errors and warnings.
*Error* is a serious issue that MUST be fixed and human intervention is required.
Warning is an issue that SHOULD be fixed. However, unlike an error, warning can be fixed automatically.
The output includes a suggested resolution, e.g. choosing *Clonic seizure* or *Seizure* in the error above.

The warnings are be fixed depending on the used validation policy.


`MINIMAL` validation policy
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Minimal validation policy enforces the least constraint on the analysis inputs.
The analysis is run *"as is"* and the run is aborted only if the most important information is missing.
Only the most rudimentary sanitation is applied.

Requirements
############

The analysis requires the following:

- At least one HPO term is provided.
- VCF path points to a readable file, if provided.
- Sample identifier is provided if run with a multi-sample VCF and the sample identifier
  must be present in the VCF file.

Sanitation
##########

The following actions are performed on the analysis input:

- Malformed CURIEs are removed.
- CURIEs that do not correspond to current or obsolete HPO terms are removed.
- The obsolete HPO term identifiers are replaced with the current identifiers.


`LENIENT` validation policy
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Lenient validation policy attempts to fix as many issues as possible.

Requirements
############

The policy requires all points of the minimal policy, plus:

- The subject is *NOT* annotated with an HPO term that is both present and excluded.
- The subject is *NOT* annotated with a present HPO term and its excluded ancestor.

Sanitation
##########

The actions of the minimal policy are performed, plus:

- Duplicate HPO terms are removed.
- The HPO terms that are not descendants of Phenotypic abnormality are removed.
- The logical inconsistencies are resolved:

    - If the subject is annotated with an excluded HPO term (e.g. no Focal seizure) and its excluded ancestor
      (e.g. no Seizure) then the term is removed and the ancestor is kept.
    - If the subject is annotated with a present HPO term (e.g. Focal seizure) and its present ancestor (e.g. Seizure),
      then the ancestor is removed and the term is kept.

`STRICT` validation policy
~~~~~~~~~~~~~~~~~~~~~~~~~~

Strict validation policy adds no additional requirements than those of *lenient* policy. However, the analysis
is not run unless no errors or warnings are found.

Requirements
############

On top of the lenient policy, strict policy requires the following:

- HPO terms are unique.
- HPO terms are descendants of Phenotypic abnormality.
- There are no logical inconsistencies in HPO terms.
- Age is well formatted, if provided.
- Sex is well formatted, if provided.

Sanitation
##########

Strict policy applies no sanitation.


Use the ``--dry-run`` option to check if the inputs can be run under given validation policy.
