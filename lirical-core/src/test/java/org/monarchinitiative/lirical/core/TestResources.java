package org.monarchinitiative.lirical.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Utility class with lazily-loaded resources for testing
 */
public class TestResources {

    public static final Path TEST_BASE = Path.of("src/test/resources");
    public static final Path LIRICAL_TEST_BASE = TEST_BASE.resolve("org").resolve("monarchinitiative").resolve("lirical").resolve("core");
    private static final Path HPO_PATH = TEST_BASE.resolve("hp.small.json");
    private static final Path ANNOTATION_PATH = TEST_BASE.resolve("small.hpoa");
    private static final Path MIM2GENE_EXCERPT_PATH = TEST_BASE.resolve("mim2gene_medgen.excerpt");
    private static final Path HGNC_EXCERPT_PATH = TEST_BASE.resolve("hgnc_complete_set.head10_and_special.tsv");
    // The HPO is in the default curie map and only contains known relationships / HP terms
    private static volatile Ontology HPO;
    private static volatile HpoDiseases HPO_DISEASES;
    private static volatile HpoAssociationData HPO_ASSOCIATION_DATA;

    public static HpoDiseases hpoDiseases() {
        if (HPO_DISEASES == null) {
            synchronized (TestResources.class) {
                if (HPO_DISEASES == null)
                    HPO_DISEASES = loadHpoDiseases();
            }
        }
        return HPO_DISEASES;
    }

    private static HpoDiseases loadHpoDiseases() {
        try {
            HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), true, HpoDiseaseLoaderOptions.DEFAULT_COHORT_SIZE);
            HpoDiseaseLoader loader = HpoDiseaseLoaders.v2(hpo(), options);
            return loader.load(ANNOTATION_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Ontology hpo() {
        if (HPO == null) {
            synchronized (TestResources.class) {
                if (HPO == null)
                    HPO = OntologyLoader.loadOntology(HPO_PATH.toFile());
            }
        }
        return HPO;
    }

    public static HpoAssociationData hpoAssociationData() {
        if (HPO_ASSOCIATION_DATA == null) {
            synchronized (TestResources.class) {
                if (HPO_ASSOCIATION_DATA == null)
                    HPO_ASSOCIATION_DATA = loadHpoAssociationData(hpo(), hpoDiseases());
            }
        }
        return HPO_ASSOCIATION_DATA;
    }

    private static HpoAssociationData loadHpoAssociationData(Ontology hpo, HpoDiseases diseases) {
        return HpoAssociationData.builder(hpo)
                .hpoDiseases(diseases)
                .hgncCompleteSetArchive(HGNC_EXCERPT_PATH)
                .mim2GeneMedgen(MIM2GENE_EXCERPT_PATH)
                .build();
    }

    private TestResources() {
    }
}
