package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Properties needed to initialize {@link Lirical}.
 */
public class LiricalProperties {

    private final Path liricalDataDirectory;
    private final Path exomiserDataDirectory; // nullable
    private final Path backgroundFrequencyFile; // nullable
    private final Set<DiseaseDatabase> diseaseDatabases;
    private final GenotypeLrProperties genotypeLrProperties;
    private final float defaultVariantFrequency;
    private final GenomeBuild genomeBuild;
    private final TranscriptDatabase transcriptDatabase;

    private LiricalProperties(Builder builder) {
        this.liricalDataDirectory = builder.liricalDataDirectory;
        this.exomiserDataDirectory = builder.exomiserDataDirectory; // nullable
        this.backgroundFrequencyFile = builder.backgroundFrequencyFile; // nullable
        this.diseaseDatabases = Set.copyOf(Objects.requireNonNull(builder.diseaseDatabases));
        this.genotypeLrProperties = new GenotypeLrProperties(builder.pathogenicityThreshold, builder.defaultBackgroundVariantFrequency, builder.strict);
        this.defaultVariantFrequency = builder.defaultVariantFrequency;
        this.genomeBuild = Objects.requireNonNull(builder.genomeBuild);
        this.transcriptDatabase = Objects.requireNonNull(builder.transcriptDatabase);
    }

    public Path liricalDataDirectory() {
        return liricalDataDirectory;
    }

    public Optional<Path> exomiserDataDirectory() {
        return Optional.ofNullable(exomiserDataDirectory);
    }

    public Optional<Path> backgroundFrequencyFile() {
        return Optional.ofNullable(backgroundFrequencyFile);
    }

    public Set<DiseaseDatabase> diseaseDatabases() {
        return diseaseDatabases;
    }

    public GenotypeLrProperties genotypeLrProperties() {
        return genotypeLrProperties;
    }

    public float defaultVariantFrequency() {
        return defaultVariantFrequency;
    }

    public GenomeBuild genomeBuild() {
        return genomeBuild;
    }

    public TranscriptDatabase transcriptDatabase() {
        return transcriptDatabase;
    }

    /**
     * Build {@link LiricalProperties} based on a single mandatory argument.
     * @param liricalDataDirectory path to Lirical data directory.
     */
    public static Builder builder(Path liricalDataDirectory) {
        return new Builder(liricalDataDirectory);
    }

    public static class Builder {

        private final Path liricalDataDirectory;
        private Path exomiserDataDirectory;
        private Path backgroundFrequencyFile;
        private Set<DiseaseDatabase> diseaseDatabases = Set.of(DiseaseDatabase.OMIM);
        private float pathogenicityThreshold = .8f;
        private double defaultBackgroundVariantFrequency = 0.1;
        private boolean strict = false;
        private float defaultVariantFrequency = 0.00001F;
        private GenomeBuild genomeBuild = GenomeBuild.HG38;
        private TranscriptDatabase transcriptDatabase = TranscriptDatabase.REFSEQ;

        private Builder(Path liricalDataDirectory) {
            this.liricalDataDirectory = Objects.requireNonNull(liricalDataDirectory);
        }

        public Builder exomiserDataDirectory(Path exomiserDataDirectory) {
            this.exomiserDataDirectory = exomiserDataDirectory;
            return this;
        }

        public Builder backgroundFrequencyFile(Path backgroundFrequencyFile) {
            this.backgroundFrequencyFile = backgroundFrequencyFile;
            return this;
        }

        public Builder diseaseDatabases(Set<DiseaseDatabase> diseaseDatabases) {
            this.diseaseDatabases = diseaseDatabases;
            return this;
        }

        public Builder genotypeLrProperties(boolean strict, double defaultBackgroundVariantFrequency, float pathogenicityThreshold) {
            this.strict = strict;
            this.defaultBackgroundVariantFrequency = defaultBackgroundVariantFrequency;
            this.pathogenicityThreshold = pathogenicityThreshold;
            return this;
        }

        public Builder defaultVariantFrequency(float defaultVariantFrequency) {
            this.defaultVariantFrequency = defaultVariantFrequency;
            return this;
        }

        public Builder genomeAssembly(GenomeBuild genomeBuild) {
            this.genomeBuild = genomeBuild;
            return this;
        }

        public Builder transcriptDatabase(TranscriptDatabase transcriptDatabase) {
            this.transcriptDatabase = transcriptDatabase;
            return this;
        }

        public LiricalProperties build() {
            return new LiricalProperties(this);
        }

    }
}