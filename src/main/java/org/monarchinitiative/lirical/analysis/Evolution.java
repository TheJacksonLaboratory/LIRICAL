package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An evolution algorithm that chooses the best set of terms from the input with which to calculate the
 * posterior probability. It is a heuristic that is based on the assumption that some unknown proportion of
 * the input terms is false positive (or is not used to annotate the disease model etc) and that by removing
 * these terms, we can maximize the posterior probability.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Evolution {
    private static final Logger logger = LoggerFactory.getLogger(Evolution.class);

    private List<TermId> observedTerms;
    private List<TermId> negatedHpoTerms;

    private int chromosome_size;

    private List<Chromosome> chromosomes;

    private int N_CHROMOSOMES=10;

    private int N_PARENTS = 5;

    private static double MUTATION_PROB = 0.20;

    private static final Random rand = new Random();

    private final int  MAXIMUM_GENERATION = 20;

    private final double SCORE_THRESHOLD = 90.0d;

    private int currentGeneration = 0;




    public Evolution(List<TermId> hpoTerms, List<TermId> negatedTerms) {
        this.observedTerms =hpoTerms;
        this.negatedHpoTerms=negatedTerms;
        chromosome_size=observedTerms.size()+negatedHpoTerms.size();
        chromosomes = new ArrayList<>();
        int len = observedTerms.size() + negatedTerms.size();
        for (int i=0;i<N_CHROMOSOMES;++i) {
            Chromosome chr = new Chromosome(len);
            chromosomes.add(chr);
        }
    }


    public boolean nextGeneration() {
        double currentBestScore = chromosomes.stream().
                max(Comparator.comparing(Chromosome::getScore)).
                get().getScore();
        logger.error("Current best score {}, generation {}",currentBestScore,currentGeneration);
        return currentBestScore<SCORE_THRESHOLD && currentGeneration<MAXIMUM_GENERATION;
    }


    public int getNumberOfChromosomes() { return chromosomes.size();}
    public List<TermId> getObservedTermsForChromosomeN(int n) {
        Chromosome chr = chromosomes.get(n);
        List<TermId> observed = new ArrayList<>();
        for (int i=0;i<observedTerms.size();i++) {
            if (chr.values[i]) observed.add(observedTerms.get(i));
        }
        return observed;
    }
    public List<TermId> getNegatedTermsForChromosomeN(int n) {
        if (negatedHpoTerms.isEmpty()) return negatedHpoTerms;
        Chromosome chr = chromosomes.get(n);
        List<TermId> excluded = new ArrayList<>();
        int N = observedTerms.size(); //
        for (int i=0; i<negatedHpoTerms.size(); i++) {
            if (chr.values[i+N]) excluded.add(negatedHpoTerms.get(i));
        }
        return excluded;
    }

    public List<TermId>  getBestChromosomeObserved() {
        Chromosome c = chromosomes.stream().
                max(Comparator.comparing(Chromosome::getScore)).get();
        List<TermId> observed = new ArrayList<>();
        for (int i=0;i<observedTerms.size();i++) {
            if (c.values[i]) observed.add(observedTerms.get(i));
        }
        return observed;
    }


    public List<TermId>  getBestChromosomeNegated() {
        Chromosome chr = chromosomes.stream().
                max(Comparator.comparing(Chromosome::getScore)).get();
        List<TermId> excluded = new ArrayList<>();
        int N = observedTerms.size(); //
        for (int i=0; i<negatedHpoTerms.size(); i++) {
            if (chr.values[i+N]) excluded.add(negatedHpoTerms.get(i));
        }
        return excluded;
    }


    public void setScoreForChromosomeN(int n,double score) {
        Chromosome chr = chromosomes.get(n);
        chr.setScore(score);
    }

    public void createNextGeneration() {
        List<Chromosome> parents = select();
        this.chromosomes = recombineAndMutate(parents);
        this.currentGeneration++;
    }


    private List<Chromosome> select() {
        Collections.sort(chromosomes);
        return chromosomes.subList(0,N_PARENTS);
    }

    private List<Chromosome> recombineAndMutate(List<Chromosome> parents){
        List<Chromosome> children = new ArrayList<>();
        for (int i=0;i<N_CHROMOSOMES; i++) {
            int p1 = rand.nextInt(parents.size());
            int p2 = rand.nextInt(parents.size());
            int breakpoint = rand.nextInt(chromosome_size);
            // note that p1 and o2 can be equal by chance.
            // in this case, then we will only apply the mutation operator
            Chromosome child = new Chromosome(parents.get(p1),parents.get(p2),breakpoint);
            children.add(child);
        }
        return children;
    }



    private static class Chromosome implements Comparable<Chromosome> {

        private boolean[] values;

        private double score;

        public Chromosome(int length) {
            values = new boolean[length];
            for (int i=0;i<length;i++) {
                double r = rand.nextDouble();
                values[i] = r>MUTATION_PROB;
            }
            score = 0.0;
        }

        /**
         * Recombine two parent chromosomes and mutate them.
         * @param p1
         * @param p2
         * @param breakpoint
         */
        public Chromosome(Chromosome p1, Chromosome p2, int breakpoint) {
            int length = p1.size();
            values = new boolean[length];
            for (int i=0;i<length;i++) {
                if (i<breakpoint) {
                    values[i] = p1.getValue(i);
                } else {
                    values[i] = p2.getValue(i);
                }
                double r = rand.nextDouble();
                if (r<MUTATION_PROB) { values[i]=!values[i]; }
            }
        }


        public boolean getValue(int i) {
            return values[i];
        }

        public int size() { return values.length; }

        public void setScore(double s) { score=s;}

        public double getScore() { return score;}

        public int compareTo(Chromosome other) {
            if (this.score < other.score) return -1;
            else if (this.score > other.score) return 1;
            else return 0;
        }

    }




}
