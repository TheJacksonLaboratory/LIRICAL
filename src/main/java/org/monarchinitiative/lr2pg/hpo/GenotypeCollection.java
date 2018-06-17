package org.monarchinitiative.lr2pg.hpo;

/**
 * THis is a convenience class that will store the relevant values for the genotypes as observed in a VCF file.
 * For the LR2PG algorithm, we need to know the number of disease-bin variants and their mean Exomiser pathogenicity score.
 * This class will store such a collection of values.
 */
public class GenotypeCollection {


    public GenotypeCollection() {

    }

    /**
     * Each Genotype object represents the Exomiser results for one gene -- the number of alleles in the
     * pathogenic bin (homozygous variants count as 2), and their mean pathogenicity score.
     */
    static class Genotype {
        int count;
        double meanPathogenicityScore;

        public Genotype(int c, double m) {
            this.count=c;
            this.meanPathogenicityScore=m;
        }
    }
}
