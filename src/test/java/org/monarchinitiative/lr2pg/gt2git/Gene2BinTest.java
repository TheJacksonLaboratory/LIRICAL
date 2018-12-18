package org.monarchinitiative.lr2pg.gt2git;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class Gene2BinTest {
    private final static double EPSILON=0.0001;

    @Test
    void testAddVariants() {
        String symbol="FAKE_SYMBOL";
        String id="123";
        Gene2Bin g2bin = new Gene2Bin(symbol,id);
        // add one benign and two pathogenic variants
        g2bin.addVar(0.01,0.05); // benign (path=0.05)
        g2bin.addVar(0.01,0.99); // pathogenic
        g2bin.addVar(0.02,0.95); // pathogenic
        double expectedPathogenicBinFrequency=0.03/100; // sum of the two pathogenic variants divided by 100
        assertEquals(expectedPathogenicBinFrequency,g2bin.getPathogenicBinFrequency(),EPSILON);
    }

    @Test
    void testBorderOfBin() {
        // the lower pathogenic threshold is 0.8 -- inclusive
        String symbol="FAKE_SYMBOL";
        String id="123";
        Gene2Bin g2bin = new Gene2Bin(symbol,id);
        g2bin.addVar(0.01,0.80); // should be included in pathogenic bin
        double expectedPathogenicBinFrequency=0.01/100; // sum of the two pathogenic variants divided by 100
        assertEquals(expectedPathogenicBinFrequency,g2bin.getPathogenicBinFrequency(),EPSILON);
    }

    @Test
    void testBorderOfBin2() {
        // the lower pathogenic threshold is 0.8 -- inclusive
        String symbol="FAKE_SYMBOL";
        String id="123";
        Gene2Bin g2bin = new Gene2Bin(symbol,id);
        g2bin.addVar(0.01,0.79); // should be included in bening bin
        double expectedPathogenicBinFrequency=0.00/100; // sum of the two pathogenic variants divided by 100
        assertEquals(expectedPathogenicBinFrequency,g2bin.getPathogenicBinFrequency(),EPSILON);
    }
}
