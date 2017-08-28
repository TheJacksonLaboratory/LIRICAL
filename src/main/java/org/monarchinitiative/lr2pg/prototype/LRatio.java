package org.monarchinitiative.lr2pg.prototype;

import org.apache.log4j.Logger;

public class LRatio {
    static Logger logger = Logger.getLogger(LRatio.class.getName());
    private int x,y;

    /**
     *
     * @param pretestprob
     * @param b
     */
    LRatio(int pretestprob, int b) {
        this.x=pretestprob;
        this.y=b;
        logger.trace(String.format("x=%d,y=%d",x,y ));
    }


    public int sum() { return x+y; }

    public double odds() {
        return -1d;
    }



}
