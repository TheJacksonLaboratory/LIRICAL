package org.monarchinitiative.lirical.model;

import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class Gene2GenotypeDefault {

    static Gene2Genotype of(GeneIdentifier geneId, Collection<LiricalVariant> variants) {
        Objects.requireNonNull(geneId, "Gene ID must not be null");
        Objects.requireNonNull(variants, "Variants must not be null");
        if (variants.isEmpty()) {
            return new Gene2GenotypeNoVariants(geneId);
        } else {
            return new Gene2GenotypeFull(geneId, variants);
        }
    }

    private static class Gene2GenotypeFull implements Gene2Genotype {

        private final GeneIdentifier geneId;
        private final List<LiricalVariant> variants;

        private Gene2GenotypeFull(GeneIdentifier geneId, Collection<LiricalVariant> variants) {
            this.geneId = geneId;
            this.variants = List.copyOf(variants);
        }

        @Override
        public GeneIdentifier geneId() {
            return geneId;
        }

        @Override
        public Stream<LiricalVariant> variants() {
            return variants.stream();
        }

        @Override
        public int variantCount() {
            return variants.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Gene2GenotypeFull that = (Gene2GenotypeFull) o;
            return Objects.equals(geneId, that.geneId) && Objects.equals(variants, that.variants);
        }

        @Override
        public int hashCode() {
            return Objects.hash(geneId, variants);
        }

        @Override
        public String toString() {
            return "Gene2GenotypeFull{" +
                    "geneId=" + geneId +
                    ", variants=" + variants +
                    '}';
        }
    }

    private record Gene2GenotypeNoVariants(GeneIdentifier geneId) implements Gene2Genotype {

        @Override
        public GeneIdentifier geneId() {
            return geneId;
        }

        @Override
        public Stream<LiricalVariant> variants() {
            return Stream.empty();
        }

        @Override
        public int variantCount() {
            return 0;
        }

    }


}
