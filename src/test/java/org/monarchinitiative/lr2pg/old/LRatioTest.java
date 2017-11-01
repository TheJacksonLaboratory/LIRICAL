package org.monarchinitiative.lr2pg.old;

import org.junit.Assert;
import org.junit.Test;

public class LRatioTest {


   /* @Test
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
       /* double epsilon=0.01;
        Assert.assertEquals(expected,lr.odds(),epsilon);
    }*/

/* Added by Vida*/

    @Test
    public void testRatio(){
        double pretesprob = 0.01;
        double sensit = 0.4;
        double specif = 0.9;
        double epsilon = 0.001;
        LRatio lr = new LRatio(pretesprob,sensit,specif);
        Assert.assertEquals(4,lr.ratio(),epsilon);
    }

    @Test
    public void testPretestOdds(){
        double pretesprob = 0.01;
        double sensit = 0.4;
        double specif = 0.9;
        double epsilon = 0.001;
        LRatio lr = new LRatio(pretesprob,sensit,specif);
        Assert.assertEquals(0.0101,lr.pretestodds(),epsilon);
    }
    @Test
    public void testPosttestOdds(){
        double pretesprob = 0.01;
        double sensit = 0.4;
        double specif = 0.9;
        double epsilon = 0.001;
        LRatio lr = new LRatio(pretesprob,sensit,specif);
        Assert.assertEquals(0.040,lr.ratio()*lr.pretestodds(),epsilon);
    }

    @Test
    public void testPosttestProb() {
        double pretesprob = 0.01;
        double sensit = 0.4;
        double specif = 0.9;
        double epsilon = 0.001;
        LRatio lr = new LRatio(pretesprob, sensit, specif);
        Assert.assertEquals(0.038, (lr.ratio()*lr.pretestodds())/(1+lr.ratio()*lr.pretestodds()), epsilon);
    }
    /* all above tests in one test*/
     @Test
     public void testlikelihoodratio(){
            double pretesprob = 0.01;
            double sensit = 0.4;
            double specif = 0.9;
            double epsilon = 0.001;
            LRatio lr = new LRatio(pretesprob,sensit,specif);
            Assert.assertEquals(4,lr.ratio(),epsilon);
            Assert.assertEquals(0.0101,lr.pretestodds(),epsilon);
            Assert.assertEquals(0.04,lr.ratio()*lr.pretestodds(),epsilon);
            Assert.assertEquals(0.038, (lr.ratio()*lr.pretestodds())/(1+lr.ratio()*lr.pretestodds()), epsilon);
           // Assert.assertEquals(0.44,lr.posttestodds(),epsilon);
           // Assert.assertEquals(0.30, lr.posttestprob(), epsilon);
    }

}
