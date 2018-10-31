package org.monarchinitiative.lr2pg.gt2git;

/**
 * This class represents the collection of pathogenicity values that are observed for a specific gene. The pathogenicity
 * values are divided up into two bins: 0-80% (benign) and 80-100% (predicted pathogenic).
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Gene2Bin {
    /** The HGNC gene symbol for the current gene. */
    private final String genesymbol;
    /** The Entrez Gene id for the current gene. */
    private final String geneid;
    /** The {@link Bin} for pathogenicity scores from 0-80% .*/
    private final Bin predictedBenignBin;
    /** The {@link Bin} for pathogenicity scores from 80-100% .*/
    private final Bin predictedPathogenicBin;
    private final static double PATHOGENICITY_THRESHOLD=0.80;

    /**
     * Set the symbol and id and initialize the two bins.
     * @param symbol a gene symbol
     * @param id the corresponding (Entrez Gene) Id.
     */
    public Gene2Bin(String symbol, String id) {
        this.genesymbol=symbol;
        this.geneid=id;
        predictedBenignBin = new Bin(0.0, PATHOGENICITY_THRESHOLD);
        predictedPathogenicBin = new Bin(PATHOGENICITY_THRESHOLD, 1.0);
    }


    public void addVar(double frequency, double pathogenicity) {
        if (pathogenicity >= 0.0 && pathogenicity < PATHOGENICITY_THRESHOLD) {
            predictedBenignBin.addvar(frequency);
        } else if (pathogenicity >= PATHOGENICITY_THRESHOLD && pathogenicity <= 1.0) {
            predictedPathogenicBin.addvar(frequency);
        } else {
            // Should never happen, but if it does, we want to know right away!
            System.err.println("Error! Pathogenicity score is not between 0 and 1!");
            System.exit(1);
        }
    }

    public String getGeneid() {
        return geneid;
    }

    /** @return the header that will be used to write the results for all Gene2Bin objects to file. */
    public static String header() {
        return "#symbol\tgeneID\tfreqsum-benign\tcount-benign\tfreqsum-path\tcount-path";
    }

    public double getPathogenicBinFrequency() {
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
