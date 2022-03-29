package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunnerImpl;
import org.monarchinitiative.lirical.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.io.ExomiserDataResolver;
import org.monarchinitiative.lirical.io.GenotypeDataIngestor;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.LiricalDataResolver;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.service.ExomiserVariantMetadataService;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.lirical.service.VariantMetadataService;
import org.monarchinitiative.phenol.annotations.assoc.HpoAssociationLoader;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseAnnotationLoader;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class LiricalConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalConfiguration.class);

    // TODO - pull up
    private static final Set<DiseaseDatabase> DATABASES = Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER, DiseaseDatabase.ORPHANET);
    private static final GenomeAssembly ASSEMBLY = GenomeAssembly.HG38;
    private static final float PATHOGENICITY_THRESHOLD = 0.8f;
    private static final boolean STRICT = false;

    private final Lirical lirical;

    // TODO - group paths into an object
    public static LiricalConfiguration of(Path liricalDataDirectory,
                                          Path exomiserDataDirectory) throws LiricalDataException {
        LiricalDataResolver liricalDataResolver = LiricalDataResolver.of(liricalDataDirectory);
        ExomiserDataResolver exomiserDataResolver = ExomiserDataResolver.of(exomiserDataDirectory); // TODO - nullable!

        return new LiricalConfiguration(liricalDataResolver, exomiserDataResolver);
    }

    private LiricalConfiguration(LiricalDataResolver liricalDataResolver,
                                 ExomiserDataResolver exomiserDataResolver) throws LiricalDataException {
        this.lirical = createLirical(liricalDataResolver, exomiserDataResolver);
    }

    private static Lirical createLirical(LiricalDataResolver liricalDataResolver,
                                         ExomiserDataResolver exomiserDataResolver) throws LiricalDataException {
        VariantMetadataService variantMetadataService = createVariantMetadataService(exomiserDataResolver, ASSEMBLY, VariantMetadataService.defaultOptions());

        Ontology hpo = loadOntology(liricalDataResolver.hpoJson());
        HpoDiseases diseases = loadHpoDiseases(liricalDataResolver.phenotypeAnnotations(), hpo);
        HpoAssociationData associationData = loadAssociationData(hpo, liricalDataResolver.homoSapiensGeneInfo(), liricalDataResolver.mim2geneMedgen(), liricalDataResolver.phenotypeAnnotations());
        PhenotypeService phenotypeService = PhenotypeService.of(hpo, diseases, associationData);


        LiricalAnalysisRunner liricalAnalysisRunner = createLiricalAnalyzer(phenotypeService);

        return Lirical.of(variantMetadataService, phenotypeService, liricalAnalysisRunner);
    }

    private static LiricalAnalysisRunner createLiricalAnalyzer(PhenotypeService phenotypeService) throws LiricalDataException {
        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(phenotypeService.diseases());
        PhenotypeLikelihoodRatio phenotypeLikelihoodRatio = new PhenotypeLikelihoodRatio(phenotypeService.hpo(), phenotypeService.diseases().diseaseById());
        GenotypeLikelihoodRatio genotypeLrEvaluator = createGenotypeLrEvaluator(ASSEMBLY);

        return LiricalAnalysisRunnerImpl.of(phenotypeService,
                pretestDiseaseProbability,
                phenotypeLikelihoodRatio,
                genotypeLrEvaluator);
    }

    private static GenotypeLikelihoodRatio createGenotypeLrEvaluator(GenomeAssembly assembly) throws LiricalDataException {
        try (BufferedReader reader = openReaderForAssembly(assembly)) {
            Map<TermId, Double> background = GenotypeDataIngestor.parse(reader);
            GenotypeLikelihoodRatio.Options options = new GenotypeLikelihoodRatio.Options(PATHOGENICITY_THRESHOLD, STRICT);
            return new GenotypeLikelihoodRatio(background, options);
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
    }

    private static BufferedReader openReaderForAssembly(GenomeAssembly assembly) {
        String name = switch (assembly) {
            case HG19 -> "/background/background-hg19.tsv";
            case HG38 -> "/background/background-hg38.tsv";
        };
        return new BufferedReader(new InputStreamReader(LiricalConfiguration.class.getResourceAsStream(name)));
    }

    private static HpoAssociationData loadAssociationData(Ontology hpo,
                                                          Path homoSapiensGeneInfo,
                                                          Path mim2geneMedgen,
                                                          Path phenotypeHpoa) throws LiricalDataException {
        try {
            return HpoAssociationLoader.loadHpoAssociationData(hpo,
                    homoSapiensGeneInfo,
                    mim2geneMedgen,
                    null,
                    phenotypeHpoa,
                    DATABASES);
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
    }

    private static HpoDiseases loadHpoDiseases(Path annotationPath, Ontology hpo) throws LiricalDataException {
        try {
            return HpoDiseaseAnnotationLoader.loadHpoDiseases(annotationPath, hpo, DATABASES);
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
    }

    private static Ontology loadOntology(Path ontologyPath) throws LiricalDataException {
        try {
            return OntologyLoader.loadOntology(ontologyPath.toFile());
        } catch (PhenolRuntimeException e) {
            throw new LiricalDataException(e);
        }
    }

    private static VariantMetadataService createVariantMetadataService(ExomiserDataResolver resolver,
                                                                       GenomeAssembly assembly,
                                                                       VariantMetadataService.Options options) throws LiricalDataException {
        // TODO - finish configuration
        //  - resolver is nullable!
        return ExomiserVariantMetadataService.of(resolver.mvStorePath(), resolver.refseqTranscriptCache(), assembly, options);
    }

    public Lirical getLirical() {
        return lirical;
    }

}
