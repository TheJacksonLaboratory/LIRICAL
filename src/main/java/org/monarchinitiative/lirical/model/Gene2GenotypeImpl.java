package org.monarchinitiative.lirical.model;

import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class Gene2GenotypeImpl {

    static Gene2Genotype of(GeneIdentifier id, Collection<LiricalVariant> variants) {
        Objects.requireNonNull(id, "ID must not be null");
        Objects.requireNonNull(variants, "Variants must not be null");
        if (variants.isEmpty()) {
            return new Gene2GenotypeNoVariants(id);
        } else {
            return new Gene2GenotypeDefault(id, variants);
        }
    }

    private static class Gene2GenotypeDefault implements Gene2Genotype {

        private final GeneIdentifier id;
        private final List<LiricalVariant> variants;

        private Gene2GenotypeDefault(GeneIdentifier id, Collection<LiricalVariant> variants) {
            this.id = id;
            this.variants = List.copyOf(variants);
        }

        @Override
        public GeneIdentifier geneId() {
            return id;
        }

        @Override
        public Stream<LiricalVariant> variants() {
            return variants.stream();
        }

        @Override
        public int variantCount() {
            return variants.size();
        }
    }

    private static class Gene2GenotypeNoVariants implements Gene2Genotype {

        private final GeneIdentifier id;

        private Gene2GenotypeNoVariants(GeneIdentifier id) {
            this.id = id;
        }

        @Override
        public GeneIdentifier geneId() {
            return id;
        }

        @Override
        public Stream<LiricalVariant> variants() {
            return Stream.empty();
        }

        @Override
        public int variantCount() {
            return 0;
        }

        @Override
        public String toString() {
            return "Gene2GenotypeNoVariants{" +
                    "id=" + id.id() +
                    ", symbol='" + id.symbol() + '\'' +
                    '}';
        }
    }


}
