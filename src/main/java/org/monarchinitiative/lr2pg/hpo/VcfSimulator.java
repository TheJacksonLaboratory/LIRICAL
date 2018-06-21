package org.monarchinitiative.lr2pg.hpo;

import com.google.common.collect.ImmutableMap;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

/**
 * THis is a convenience class that will store the relevant values for the genotypes as observed in a VCF file.
 * For the LR2PG algorithm, we need to know the number of disease-bin variants and their mean Exomiser pathogenicity score.
 * This class will store such a collection of values.
 * NOTE THIS IS JUST FOR DEVLEOPMENT/TESTING/DEBUGGING, in the real application we get this dataq from a VCF file
 */
public class VcfSimulator {
    /** Key: the termId of a gene; double -- count of variants in pathogenic bin
     * multiplied by average pathogenicity score. */
    private ImmutableMap<TermId,Double> genotypeMap;

    private static final double meanLambda=0.05;

    private static final double meanPath=0.95;

    private static final double EPSILON=0.000001;


    public VcfSimulator(Collection<TermId> geneIds, TermId geneId, int varcount, double path) {
        ImmutableMap.Builder<TermId,Double> builder = new ImmutableMap.Builder<>();
        double lambda = varcount*path;
        lambda=lambda>0.0 ? lambda : EPSILON;
        builder.put(geneId,lambda);
        Random rand = new Random();
        for (TermId tid : geneIds) {
            if (tid.equals(geneId)) {
                continue; // we take the 'real' value for geneId, and simulate the rest
            }
            double r = rand.nextDouble();
            int count=0;
            if (r<meanLambda) {
                count=1;
                double r2 = rand.nextDouble();
                if (r2<meanLambda) {
                    count=2;
                }
            }
            double delta = (rand.nextDouble() - 0.5)/20;
            double p2=meanPath+delta;
            p2=Math.min(1.0,Math.max(p2,0.80));
            lambda = count*p2;
            lambda=lambda>0.0 ? lambda : EPSILON;
            builder.put(tid,lambda);
        }
        // now add the data for our gene
        // this will overwrite the value from above.

        genotypeMap=builder.build();
    }


    public Map<TermId, Double> getGenotypeMap() {
        return genotypeMap;
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

        public double adjustedLambda() {
            double lambda=count*meanPathogenicityScore;
            return lambda>0.0 ? lambda : EPSILON; }
    }
}
