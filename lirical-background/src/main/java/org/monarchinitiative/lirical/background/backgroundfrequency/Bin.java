package org.monarchinitiative.lirical.background.backgroundfrequency;

/**
 * This class represents one of two bins associated with each gene. Bin A is for the "pathogenic" variants, i.e.,
 * with a pathogenicity score of 0.8-1, and bin B is for the "non-pathogenic" variants, i.e.,
 * with a pathogenicity score of 0-0.8. Note that we do not record the frequency ranges for the bin within
 * the bin itself -- this bookkeeping is done in the class {@link Gene2Bin}.
 * It is important to note that the Exomiser reports frequencies as percentages and not as proportions, and so
 * this class takes care of that.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class Bin {
    /** A small constant added to avoid division by zero. */
    private final static double EPSILON=1/(double)100_000;
    /** The sumOfPerc of the percentages (100*frequency) of all of the variants associated with this bin of this gene. */
    private double sumOfPerc;
    /** The total number (count) of variants associated with this bin of this gene. */
    private int count;

    /**
     *  Initialize this bin. Add a count of epsilon*100 as a pseudocount to avoid division by zero.
     */
      public Bin() {
        // Multiply epsilon by 100 because the data is percentage rather than frequency based.
        sumOfPerc=100*EPSILON;
        count=0;
    }

    /**
     * This function is called to add the data for one new variant to the bin.
     * We note that the Exomiser reports its frequency data as percentages.
     * @param percentage The percentage-reported population frequency of the variant
     */
    void addvar(double percentage) {
        sumOfPerc += percentage;
        count++;
    }

    /**
     * We return the frequency rather than the percentage. Note that this is the TOTAL frequency (sum of individual frequencies).
     * @return The sum of the frequency of all of the variants associated with this bin of this gene. */
    double getBinFrequency(){
        return sumOfPerc/100.0;
    }

    /** @return The total number of variants associated with this bin of this gene. */
    int getBinCount(){
        return count;
    }

}
