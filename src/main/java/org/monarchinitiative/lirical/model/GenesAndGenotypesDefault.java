package org.monarchinitiative.lirical.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class GenesAndGenotypesDefault {

    static GenesAndGenotypes empty() {
        return GenesAndGenotypesEmpty.INSTANCE;
    }

    public static GenesAndGenotypes of(List<Gene2Genotype> genes) {
        return new GenesAndGenotypesFull(genes);
    }

    record GenesAndGenotypesFull(List<Gene2Genotype> geneList) implements GenesAndGenotypes {

        GenesAndGenotypesFull(List<Gene2Genotype> geneList) {
            this.geneList = Objects.requireNonNull(geneList, "Gene list must not be null");
        }

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
