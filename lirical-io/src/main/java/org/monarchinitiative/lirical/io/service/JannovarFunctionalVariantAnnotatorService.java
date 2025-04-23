package org.monarchinitiative.lirical.io.service;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotatorService;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.LiricalDataResolver;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JannovarFunctionalVariantAnnotatorService implements FunctionalVariantAnnotatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JannovarFunctionalVariantAnnotatorService.class);

    private final LiricalDataResolver liricalDataResolver;
    private final GeneIdentifiers geneIdentifiers;
    private final Map<GenomeBuild, Set<TranscriptDatabase>> knownAnnotators;

    private GenomeBuild genomeBuild;
    private TranscriptDatabase transcriptDatabase;
    private FunctionalVariantAnnotator functionalVariantAnnotator;

    public static JannovarFunctionalVariantAnnotatorService of(
            LiricalDataResolver liricalDataResolver,
            GeneIdentifiers geneIdentifiers
    ) {
        Map<GenomeBuild, Set<TranscriptDatabase>> knownAnnotators = initializeAnnotators(liricalDataResolver);
        return new JannovarFunctionalVariantAnnotatorService(liricalDataResolver, geneIdentifiers, knownAnnotators);
    }

    private JannovarFunctionalVariantAnnotatorService(
            LiricalDataResolver liricalDataResolver,
            GeneIdentifiers geneIdentifiers,
            Map<GenomeBuild, Set<TranscriptDatabase>> knownAnnotators
    ) {
        this.liricalDataResolver = Objects.requireNonNull(liricalDataResolver);
        this.geneIdentifiers = Objects.requireNonNull(geneIdentifiers);
        this.knownAnnotators = knownAnnotators;
    }

    @Override
    public Optional<FunctionalVariantAnnotator> getFunctionalAnnotator(
            GenomeBuild genomeBuild,
            TranscriptDatabase transcriptDatabase
    ) {
        if (knownAnnotators.getOrDefault(genomeBuild, Set.of()).contains(transcriptDatabase)) {
            synchronized (this) {
                if (this.genomeBuild != genomeBuild || this.transcriptDatabase != transcriptDatabase) {
                    LOGGER.debug("Loading transcript database for {}:{}", genomeBuild, transcriptDatabase);
                    Path txDatabasePath = liricalDataResolver.transcriptCacheFor(genomeBuild, transcriptDatabase);
                    try {
                        functionalVariantAnnotator = loadFunctionalVariantAnnotator(txDatabasePath, geneIdentifiers);
                    } catch (LiricalDataException e) {
                        LOGGER.warn("Unable to load transcript database from {}", txDatabasePath.toAbsolutePath());
                        return Optional.empty();
                    }
                    this.genomeBuild = genomeBuild;
                    this.transcriptDatabase = transcriptDatabase;
                }
                return Optional.of(functionalVariantAnnotator);
            }
        } else {
            return Optional.empty();
        }
    }

    private static Map<GenomeBuild, Set<TranscriptDatabase>> initializeAnnotators(LiricalDataResolver liricalDataResolver) {
        Map<GenomeBuild, Set<TranscriptDatabase>> annotators = new HashMap<>();
        if (Files.isReadable(liricalDataResolver.hg19UcscTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG19, gb -> new HashSet<>()).add(TranscriptDatabase.UCSC);
        if (Files.isReadable(liricalDataResolver.hg19EnsemblTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG19, gb -> new HashSet<>()).add(TranscriptDatabase.ENSEMBL);
        if (Files.isReadable(liricalDataResolver.hg19RefseqTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG19, gb -> new HashSet<>()).add(TranscriptDatabase.REFSEQ);
        if (Files.isReadable(liricalDataResolver.hg19RefseqCuratedTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG19, gb -> new HashSet<>()).add(TranscriptDatabase.REFSEQ_CURATED);

        if (Files.isReadable(liricalDataResolver.hg38UcscTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG38, gb -> new HashSet<>()).add(TranscriptDatabase.UCSC);
        if (Files.isReadable(liricalDataResolver.hg38EnsemblTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG38, gb -> new HashSet<>()).add(TranscriptDatabase.ENSEMBL);
        if (Files.isReadable(liricalDataResolver.hg38RefseqTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG38, gb -> new HashSet<>()).add(TranscriptDatabase.REFSEQ);
        if (Files.isReadable(liricalDataResolver.hg38RefseqCuratedTxDatabase()))
            annotators.computeIfAbsent(GenomeBuild.HG38, gb -> new HashSet<>()).add(TranscriptDatabase.REFSEQ_CURATED);


        String configured = annotators.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> "%s -> %s".formatted(e.getKey(), v.toString())))
                .collect(Collectors.joining(", "));
        if (annotators.isEmpty())
            LOGGER.warn("No functional annotators were configured");
        else
            LOGGER.debug("Configured Jannovar functional annotators for {}", configured);

        return annotators;
    }

    private FunctionalVariantAnnotator loadFunctionalVariantAnnotator(Path txDatabasePath,
                                                                      GeneIdentifiers geneIdentifiers) throws LiricalDataException {
        JannovarData jannovarData = loadJannovarData(txDatabasePath);
        return JannovarFunctionalVariantAnnotator.of(jannovarData, geneIdentifiers);
    }

    private static JannovarData loadJannovarData(Path txDatabasePath) throws LiricalDataException {
        LOGGER.info("Loading transcript database from {}", txDatabasePath.toAbsolutePath());
        try {
            return new JannovarDataSerializer(txDatabasePath.toAbsolutePath().toString()).load();
        } catch (SerializationException e) {
            throw new LiricalDataException(e);
        }
    }
}
