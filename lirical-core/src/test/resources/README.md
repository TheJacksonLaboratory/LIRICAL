# README

The test resources include the following files:
- `hp.small.json` - a toy HPO file with subset of terms
- `small.hpoa` - HPO annotation file containing 3 OMIM diseases: 
  - `CLEFT PALATE, DEAFNESS, AND OLIGODONTIA`
  - `CHARCOT-MARIE-TOOTH DISEASE, TYPE 4K`
  - `OMODYSPLASIA`
  - `BOBOPHOBIA` - made-up diseases used to test `DiseaseOnsetProbability` implementation. The diseases are AR with 
     a bunch of made-up phenotype terms with various onsets, depending on the Bobophobia's sub-type.
    
    There are two Bobophobia subtypes: `A` and `B`. Both subtypes present the same phenotypes: 
    *Rod-cone dystrophy* (`HP:0000510`) and *Strabismus* (`HP:0000486`). The phenotype present with later 
    onset in subtype `A`, and not all probands develop the features. However, Bobophobia `B`, the more severe subtype,
    presents both features since birth in all probands.
     
