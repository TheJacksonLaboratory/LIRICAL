package org.monarchinitiative.lr2pg.old;

//import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LRatio {
    private static final Logger logger = LogManager.getLogger();
    //static Logger logger = Logger.getLogger(LRatio.class.getName());
  //  private int x,y;

    /**
     *
     //* @param pretestprob
     //* @param b
     */
   /* LRatio(int pretestprob, int b) {
        this.x=pretestprob;
        this.y=b;
        logger.trace(String.format("x=%d,y=%d",x,y ));
    }


    public int sum() { return x+y; }

    public double odds() {
        return -1d;
    }*/
    private double x,y,z;
    LRatio(double pretestprob, double sensit, double specif) {
        this.x = pretestprob;
        this.y = sensit;
        this.z = specif;
       // logger.trace(String.format("x=%f,y=%f,z=%f",x,y,z ));
    }

    public double ratio() {return y/(1-z);}

    public double pretestodds() {return x/(1-x);}

    //public double posttestodds(){return (x/(1-x))*(y/(1-z));}

    //public double posttestprob(){return ((x/(1-x))*(y/(1-z)))/(1+(x/(1-x))*(y/(1-z)));}







}
