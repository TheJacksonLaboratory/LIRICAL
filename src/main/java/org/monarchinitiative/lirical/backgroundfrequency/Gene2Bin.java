package org.monarchinitiative.lirical.backgroundfrequency;

/**
 * This class represents the collection of pathogenicity values that are observed for a specific gene. The pathogenicity
 * values are divided up into two bins: 0-80% (benign) and 80-100% (predicted pathogenic).
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class Gene2Bin {
    /** The HGNC gene symbol for the current gene. */
    private final String genesymbol;
    /** The Entrez Gene id for the current gene. */
    private final String geneid;
    /** The {@link Bin} for pathogenicity scores from 0-80% .*/
    private final Bin predictedBenignBin;
    /** The {@link Bin} for pathogenicity scores from 80-100% .*/
    private final Bin predictedPathogenicBin;
    /** Lower limit of pathogenic bin--80 percent pathogenicity score */
    private final static double PATHOGENICITY_THRESHOLD=0.80;

    /**
     * Set the symbol and id and initialize the two bins.
     * @param symbol a gene symbol
     * @param id the corresponding (Entrez Gene) Id.
     */
    Gene2Bin(String symbol, String id) {
        this.genesymbol=symbol;
        this.geneid=id;
        predictedBenignBin = new Bin();
        predictedPathogenicBin = new Bin();
    }

    /**
     * Add the population freqeuncy (as percentage) and predicted pathogenicity for one variant
     * @param percentage population frequency expressed as percentage
     * @param pathogenicity predicted pathogenicity of some variant.
     */
    void addVar(double percentage, double pathogenicity) {
        if (pathogenicity >= 0.0 && pathogenicity < PATHOGENICITY_THRESHOLD) {
            predictedBenignBin.addvar(percentage);
        } else if (pathogenicity >= PATHOGENICITY_THRESHOLD && pathogenicity <= 1.0) {
            predictedPathogenicBin.addvar(percentage);
        } else {
            // Should never happen, but if it does, we want to know right away!
            System.err.println("Error! Pathogenicity score is not between 0 and 1!");
            System.exit(1);
        }
    }

    /** @return the gene ID (e.g., EntrezGene number) of the gene. */
    String getGeneid() {
        return geneid;
    }

    /** @return the header that will be used to write the results for all Gene2Bin objects to file. */
    static String header() {
        return "#symbol\tgeneID\tfreqsum-benign\tcount-benign\tfreqsum-path\tcount-path";
    }
    /** @return the sum of the frequencies of all variants in the pathogenic bin for the current gene. */
    double getPathogenicBinFrequency() {
        return this.predictedPathogenicBin.getBinFrequency();
    }



    @Override
    public String toString() {
        return String.format("%s\t%s\t%f\t%d\t%f\t%d",
                genesymbol,
                geneid,
                predictedBenignBin.getBinFrequency(),
                predictedBenignBin.getBinCount(),
                predictedPathogenicBin.getBinFrequency(),
                predictedPathogenicBin.getBinCount()
        );
    }
}
