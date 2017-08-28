package org.monarchinitiative.lr2pg.prototype;

import org.junit.Assert;
import org.junit.Test;

public class LRatioTest {


    @Test
    public void testSum() {
        int x=10;
        int y=12;
        LRatio lr = new LRatio(x,y);
        int expected = 22;
        Assert.assertEquals(expected,lr.sum());
    }


    @Test
    public void testOdds() {
        LRatio lr = new LRatio(2,3);
        double expected= 0.03; /* pretest odds */
        double epsilon=0.01;
        Assert.assertEquals(expected,lr.odds(),epsilon);
    }

}
