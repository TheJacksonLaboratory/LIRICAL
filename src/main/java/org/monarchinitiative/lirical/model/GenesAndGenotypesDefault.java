package org.monarchinitiative.lirical.model;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

record GenesAndGenotypesDefault(List<Gene2Genotype> geneList) implements GenesAndGenotypes {

    GenesAndGenotypesDefault(List<Gene2Genotype> geneList) {
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
