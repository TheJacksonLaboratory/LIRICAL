package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

public interface PhenotypeService {

    static PhenotypeService of(MinimalOntology ontology,
                               HpoDiseases diseases,
                               HpoAssociationData associationData) {
        return new PhenotypeServiceImpl(ontology, diseases, associationData);
    }

    MinimalOntology hpo();

    HpoDiseases diseases();

    HpoAssociationData associationData();

}
