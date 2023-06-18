package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class Gene2GenotypeDefault {

    static class Gene2GenotypeFull implements Gene2Genotype {

        private final GeneIdentifier geneId;
        private final List<LiricalVariant> variants;
        private final int filteredOutVariantCount;

        Gene2GenotypeFull(GeneIdentifier geneId, Collection<LiricalVariant> variants, int filteredOutVariantCount) {
            this.geneId = geneId;
            this.variants = List.copyOf(variants);
            this.filteredOutVariantCount = filteredOutVariantCount;
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
        public int filteredOutVariantCount() {
            return filteredOutVariantCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Gene2GenotypeFull that = (Gene2GenotypeFull) o;
            return filteredOutVariantCount == that.filteredOutVariantCount && Objects.equals(geneId, that.geneId) && Objects.equals(variants, that.variants);
        }

        @Override
        public int hashCode() {
            return Objects.hash(geneId, variants, filteredOutVariantCount);
        }

        @Override
        public String toString() {
            return "Gene2GenotypeFull{" +
                    "geneId=" + geneId +
                    ", variants=" + variants +
                    ", filteredOutVariantCount=" + filteredOutVariantCount
                    + '}';
        }
    }

    record Gene2GenotypeNoVariants(GeneIdentifier geneId, int filteredOutVariantCount) implements Gene2Genotype {

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
