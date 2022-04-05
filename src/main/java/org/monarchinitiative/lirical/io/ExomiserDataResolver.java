package org.monarchinitiative.lirical.io;

import org.monarchinitiative.lirical.model.GenomeBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExomiserDataResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExomiserDataResolver.class);
    private static final Pattern MV_STORE_PATTERN = Pattern.compile("(?<version>\\d{4})_(?<assembly>hg((19)|(38)))_variants.mv.db");

    private final Path exomiserDataDirectory;

    private final String version, assembly;

    public static ExomiserDataResolver of(Path exomiserDataDirectory) throws LiricalDataException {
        return new ExomiserDataResolver(exomiserDataDirectory);
    }

    private ExomiserDataResolver(Path exomiserDataDirectory) throws LiricalDataException {
        this.exomiserDataDirectory = Objects.requireNonNull(exomiserDataDirectory, "Exomiser data directory must not be null");
        LOGGER.debug("Using Exomiser directory at {}", exomiserDataDirectory.toAbsolutePath());
        try {
            Optional<Matcher> mvStoreMatcherOptional = Files.list(exomiserDataDirectory)
                    .map(Path::toFile)
                    .map(File::getName)
                    .map(MV_STORE_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .findFirst();
            if (mvStoreMatcherOptional.isEmpty())
                throw new LiricalDataException(String.format("Did not find Exomiser MV store file in `%s`", exomiserDataDirectory.toAbsolutePath()));
            Matcher matcher = mvStoreMatcherOptional.get();
            version = matcher.group("version");
            assembly = matcher.group("assembly");
            LOGGER.debug("Assuming we're working with Exomiser data version {} and genomic assembly {}", version, assembly);
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }

        checkAllResourcesArePresent();
    }

    public String version() {
        return version;
    }

    public String assembly() {
        return assembly;
    }

    public Optional<GenomeBuild> genomeBuild() {
        return switch (assembly.toUpperCase()) {
            case "HG19" -> Optional.of(GenomeBuild.HG19);
            case "HG38" -> Optional.of(GenomeBuild.HG38);
            default -> Optional.empty();
        };
    }

    private void checkAllResourcesArePresent() throws LiricalDataException {
        List<Path> required = List.of(mvStorePath(), refseqTranscriptCache(), ucscTranscriptCache());
        boolean error = false;
        for (Path path : required) {
            if (!Files.isRegularFile(path)) {
                LOGGER.warn("The file {} is not present in {}", path.toFile().getName(), exomiserDataDirectory.toAbsolutePath());
                error = true;
            }
        }
        if (error)
            throw new LiricalDataException("One or more Exomiser resource files is missing in " + exomiserDataDirectory.toAbsolutePath());
    }

    public Path mvStorePath() {
        return exomiserDataDirectory.resolve(String.format("%s_%s_variants.mv.db", version, assembly));
    }

    public Path refseqTranscriptCache() {
        return exomiserDataDirectory.resolve(String.format("%s_%s_transcripts_refseq.ser", version, assembly));
    }

    public Path ucscTranscriptCache() {
        return exomiserDataDirectory.resolve(String.format("%s_%s_transcripts_ucsc.ser", version, assembly));
    }
}
