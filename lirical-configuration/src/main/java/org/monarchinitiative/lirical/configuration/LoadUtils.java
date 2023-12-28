package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

class LoadUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadUtils.class);

    private LoadUtils() {

    }

    static MinimalOntology loadOntology(Path ontologyPath) throws LiricalDataException {
        try {
            LOGGER.debug("Loading HPO from {}", ontologyPath.toAbsolutePath());
            return MinimalOntologyLoader.loadOntology(ontologyPath.toFile());
        } catch (PhenolRuntimeException e) {
            throw new LiricalDataException(e);
        }
    }

    static HpoDiseases loadHpoDiseases(Path annotationPath,
                                       MinimalOntology hpo,
                                       HpoDiseaseLoaderOptions options) throws LiricalDataException {
        try {
            LOGGER.debug("Loading HPO annotations from {}", annotationPath.toAbsolutePath());
            HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, options);
            return loader.load(annotationPath);
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
    }

}
