package org.monarchinitiative.lirical.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseAnnotationLoader;
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
    public static final Path LIRICAL_TEST_BASE = TestResources.TEST_BASE.resolve("org").resolve("monarchinitiative").resolve("lirical").resolve("core");
    private static final Path HPO_PATH = TestResources.TEST_BASE.resolve("hp.small.json");
    private static final Path ANNOTATION_PATH = TestResources.TEST_BASE.resolve("small.hpoa");
    // The HPO is in the default  curie map and only contains known relationships / HP terms
    private static volatile Ontology ONTOLOGY;
    private static volatile HpoDiseases HPO_DISEASES;

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
            return HpoDiseaseAnnotationLoader.loadHpoDiseases(ANNOTATION_PATH, hpo(), Set.of(DiseaseDatabase.OMIM));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Ontology hpo() {
        if (ONTOLOGY == null) {
            synchronized (TestResources.class) {
                if (ONTOLOGY == null)
                    ONTOLOGY = OntologyLoader.loadOntology(HPO_PATH.toFile());
            }
        }
        return ONTOLOGY;
    }

    private TestResources() {
    }
}
