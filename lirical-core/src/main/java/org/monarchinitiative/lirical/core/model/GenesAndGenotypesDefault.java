package org.monarchinitiative.lirical.core.model;

import java.util.*;

class GenesAndGenotypesDefault {

    static GenesAndGenotypes empty() {
        return GenesAndGenotypesEmpty.INSTANCE;
    }

    public static GenesAndGenotypes of(Collection<Gene2Genotype> genes) {
        List<Gene2Genotype> geneList = List.copyOf(Objects.requireNonNull(genes, "Gene list must not be null"));
        return new GenesAndGenotypesFull(geneList);
    }

    record GenesAndGenotypesFull(List<Gene2Genotype> geneList) implements GenesAndGenotypes {

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
