package org.monarchinitiative.lirical.core.model;

import java.util.*;
import java.util.stream.Collectors;

class GenesAndGenotypesDefault {

    static GenesAndGenotypes empty() {
        return GenesAndGenotypesEmpty.INSTANCE;
    }

    public static GenesAndGenotypes of(Collection<String> sampleNames,
                                       Collection<Gene2Genotype> genes) {
        return new GenesAndGenotypesFull(
                List.copyOf(Objects.requireNonNull(sampleNames, "Sample names must not be null")),
                List.copyOf(Objects.requireNonNull(genes, "Gene list must not be null"))
        );
    }

    record GenesAndGenotypesFull(List<String> sampleNames,
                                 List<Gene2Genotype> geneList) implements GenesAndGenotypes {

        @Override
        public int size() {
            return geneList.size();
        }

        @Override
        public Iterator<Gene2Genotype> iterator() {
            return geneList.iterator();
        }
    }

    private static class GenesAndGenotypesEmpty implements GenesAndGenotypes {

        private static final GenesAndGenotypesEmpty INSTANCE = new GenesAndGenotypesEmpty();

        private GenesAndGenotypesEmpty() {
        }

        @Override
        public Collection<String> sampleNames() {
            return List.of();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<Gene2Genotype> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public String toString() {
            return "GenesAndGenotypesEmpty";
        }
    }
}
