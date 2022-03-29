package org.monarchinitiative.lirical.model;

import java.util.Collections;
import java.util.Iterator;

class GenesAndGenotypesEmpty implements GenesAndGenotypes {

    private static final GenesAndGenotypesEmpty INSTANCE = new GenesAndGenotypesEmpty();

    static GenesAndGenotypesEmpty instance() {
        return INSTANCE;
    }

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
