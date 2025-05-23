package org.monarchinitiative.lirical.exomiser_db_adapter.model.pathogenicity;

public class SpliceAiScore extends BasePathogenicityScore {

    public static SpliceAiScore of(float score) {
        return new SpliceAiScore(score);
    }

    private SpliceAiScore(float score) {
        super(PathogenicitySource.SPLICE_AI, score);
    }
}
