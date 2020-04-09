package org.monarchinitiative.lirical.backgroundfrequency;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class BinTest {

    private final static double EPSILON=0.0001;

    /** Test frequencies. Also, note that the bin gets precentages as input (0-100)
     * but it returns the overall freuqencies (divided by 100).
     */
    @Test
    void testTotalFreqeuncyAndCount() {
        Bin bin = new Bin();
        double x=0.01;
        double y=0.02;
        double z=0.03;
        bin.addvar(x);
        bin.addvar(y);
        bin.addvar(z);
        double expectedTotalFrequency=0.06/100;
        assertEquals(expectedTotalFrequency,bin.getBinFrequency(),EPSILON);
        int expectedCount=3;
        assertEquals(expectedCount,bin.getBinCount());
    }
}
