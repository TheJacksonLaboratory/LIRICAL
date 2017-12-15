# LR2PG
Likelihood ratio analysis of phenotypes and genotypes

To run LR2PG
```
$ mvn clean package
$ java -jar target/LR2PG-0.0.1-SNAPSHOT.jar download
$ java -jar target/LR2PG-0.0.1-SNAPSHOT.jar simulate
```


The input to the method is a list of HPO terms observed in a case (patient). This list is evaluated
for each disease in our database to search for the best match. We need to evaluate two probabilities.

1. The probability of the test result given the disease is present. This is the frequency of the finding in the
disease. A certain proportion of our dataset has feature freqeuncy data. For now, we will assume that if a term does
not have a fraquency modifier, then it is ALWAYS_PRESENT if the disease is annotated to the term.
We then need the probability of the test result given the disease is not present (1-specificity). We will assume that
the patient being tested has one of the diseases in the database (e.g., one of the Mendelian diseases), and we
will calculate this as the mean freqeuncy of the HPO feature over all of the diseaes (this number is precalculated; if we
are testing disease i, then the background freqeuncy will also contain the feature for disease i but this will be weighted
by 1/N, where N is the number of diseases in the database. This is a conservative approximation.

2. What do we do with features that the patient has but the disease in question does not have? If we assign a frequency of
zero to these features, then the LR for all diseases that do not have the feature would be zero. In reality, if a patient
has disease D and displays HPO feature h even though D is not annotated to this feature, there are several possibilities.
First, since the annotations in our database are not complete, the lack of annotation of D to h could be a false negative.
Secondly, the patient could have disease D and some other condition that additionally cause feature h to occur. Finally,
the patient could actually have two diseases. Therefore, we do not want to be too strict when handling these situations.

We will therefore do the following. If a feature h is not found in the annotations for H, we will recursively follow all
paths to the root of the ontology.
```
ancestorsTerms <- h
level=0;
prob=0
backgroundprob=0
DO
level = level + 1
ancestorTerms <- anc(ancestorTerms)
foreach t in ancestorTerms
  continue if t is root
  if (D is annotated to t)
    prob = LR(disease, t)/(1+log(level)) # penalty for imprecision
    backgroundprob = LR(not-disease,t)
    break
  endif
endfor
WHILE (ancestorTerms is not empty)
```
## Imprecision
This algorithm will produce "reasonable" numbers if disease D is annotated by some feature that is at least in the
same subhierarchy as h. Given the fact that annotations are 'propagated' up the tree by the true path rule, if a disease
is annotated to say _nuclear cataract_ but the patient had _cortical cataract_, then the numerator of the LR will still
be relatively large. There are two penalties -- one is that since we meet at the most informative common ancestor (MICA),
in this case, _cataract_, the background probability will be higher (more of the other diseases will have _cataract_ then
will have _cortical cataract_). We additionally divide by a penalty factor: 1/(1+log(level)) that does not penalize if
we only go up one level (it seems likely in routine medical practice that there is some imprecision in the terminology
used to record phenotypes, and that we should not penalize slight imprecision), but will grow with the number of levels
required to find a MICA. For now, we will do this in a greedy fashion, i.e., we will take the first MICA that we find.
TODO - consider using 1/level or 1/(1+log(level)) -- both do not penalize a difference in one of the level, but the
penalty grows at a different rate.

## Red herring of false negative annotation
What is we reach the root before we find the MICA? In this case, the HPO feature h affects an organ that is not
affected at all in the disease D. Our assumption is then that the finding is a "red herring", i.e., not related at
all to the disease. Since all diseases will be annotated to root, our formula would give us 1/level for the LR. Let us
start with that.



