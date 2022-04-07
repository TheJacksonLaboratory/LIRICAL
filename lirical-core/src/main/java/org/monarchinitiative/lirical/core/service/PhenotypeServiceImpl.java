package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;

record PhenotypeServiceImpl(Ontology hpo,
                            HpoDiseases diseases,
                            HpoAssociationData associationData) implements PhenotypeService {
}
