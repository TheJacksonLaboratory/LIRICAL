package org.monarchinitiative.lirical.service;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;

public interface PhenotypeService {

    static PhenotypeService of(Ontology ontology,
                               HpoDiseases diseases,
                               HpoAssociationData associationData) {
        return new PhenotypeServiceImpl(ontology, diseases, associationData);
    }

    Ontology hpo();

    HpoDiseases diseases();

    HpoAssociationData associationData();

}
