package org.monarchinitiative.lirical.output;


/**
 * This class encapsulates information about the result of LIRICAL analysis, roughly corresponding
 * to one line of the TSV output file with the rank of the originally simulated disease. This
 * class is intended to be used only for the Vcf/Phenopacket simulations.
 */
public class LiricalRanking {

    private final int rank;
    private final String line;

    public LiricalRanking(int r,String line) {
        rank=r;
        this.line = line;
    }


    @Override
    public String toString() {
        return line;
    }

    public int getRank() { return rank; }

}
