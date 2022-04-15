package org.monarchinitiative.lirical.core.output;

public class AnalysisResultsMetadata {
    private String liricalVersion;
    private String hpoVersion;
    private String transcriptDatabase;
    private String liricalPath;
    private String exomiserPath;
    private String analysisDate;
    private String sampleName;
    private long nGoodQualityVariants;
    private long nFilteredVariants;
    private int genesWithVar;
    private boolean globalMode;

    private AnalysisResultsMetadata(String liricalVersion,
                                    String hpoVersion,
                                    String transcriptDatabase,
                                    String liricalPath,
                                    String exomiserPath,
                                    String analysisDate,
                                    String sampleName,
                                    long nGoodQualityVariants,
                                    long nFilteredVariants,
                                    int genesWithVar,
                                    boolean globalMode) {
        this.liricalVersion = liricalVersion;
        this.hpoVersion = hpoVersion;
        this.transcriptDatabase = transcriptDatabase;
        this.liricalPath = liricalPath;
        this.exomiserPath = exomiserPath;
        this.analysisDate = analysisDate;
        this.sampleName = sampleName;
        this.nGoodQualityVariants = nGoodQualityVariants;
        this.nFilteredVariants = nFilteredVariants;
        this.genesWithVar = genesWithVar;
        this.globalMode = globalMode;
    }

    public String getLiricalVersion() {
        return liricalVersion;
    }

    public String getHpoVersion() {
        return hpoVersion;
    }

    public void setHpoVersion(String hpoVersion) {
        this.hpoVersion = hpoVersion;
    }

    public String getTranscriptDatabase() {
        return transcriptDatabase;
    }

    public void setTranscriptDatabase(String transcriptDatabase) {
        this.transcriptDatabase = transcriptDatabase;
    }

    public String getLiricalPath() {
        return liricalPath;
    }

    public void setLiricalPath(String liricalPath) {
        this.liricalPath = liricalPath;
    }

    public String getExomiserPath() {
        return exomiserPath;
    }

    public void setExomiserPath(String exomiserPath) {
        this.exomiserPath = exomiserPath;
    }

    public String getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(String analysisDate) {
        this.analysisDate = analysisDate;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public long getnGoodQualityVariants() {
        return nGoodQualityVariants;
    }

    public void setnGoodQualityVariants(long nGoodQualityVariants) {
        this.nGoodQualityVariants = nGoodQualityVariants;
    }

    public long getnFilteredVariants() {
        return nFilteredVariants;
    }

    public void setnFilteredVariants(long nFilteredVariants) {
        this.nFilteredVariants = nFilteredVariants;
    }

    public int getGenesWithVar() {
        return genesWithVar;
    }

    public void setGenesWithVar(int genesWithVar) {
        this.genesWithVar = genesWithVar;
    }

    public boolean getGlobalMode() {
        return globalMode;
    }

    public void setGlobalMode(boolean globalMode) {
        this.globalMode = globalMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AnalysisResultsMetadata{" +
                "liricalVersion='" + liricalVersion + '\'' +
                ", hpoVersion='" + hpoVersion + '\'' +
                ", transcriptDatabase='" + transcriptDatabase + '\'' +
                ", liricalPath='" + liricalPath + '\'' +
                ", exomiserPath='" + exomiserPath + '\'' +
                ", analysisDate='" + analysisDate + '\'' +
                ", sampleName='" + sampleName + '\'' +
                ", nGoodQualityVariants=" + nGoodQualityVariants +
                ", nFilteredVariants=" + nFilteredVariants +
                ", genesWithVar=" + genesWithVar +
                ", globalMode=" + globalMode +
                '}';
    }

    public static class Builder {
        private String liricalVersion;
        private String hpoVersion;
        private String transcriptDatabase;
        private String liricalPath;
        private String exomiserPath;
        private String analysisDate;
        private String sampleName = "SAMPLE_ID";
        private long nGoodQualityVariants;
        private long nFilteredVariants;
        private int genesWithVar;
        private boolean globalMode;

        private Builder() {
        }

        public Builder setLiricalVersion(String liricalVersion) {
            this.liricalVersion = liricalVersion;
            return this;
        }

        public Builder setHpoVersion(String hpoVersion) {
            this.hpoVersion = hpoVersion;
            return this;
        }

        public Builder setTranscriptDatabase(String transcriptDatabase) {
            this.transcriptDatabase = transcriptDatabase;
            return this;
        }

        public Builder setLiricalPath(String liricalPath) {
            this.liricalPath = liricalPath;
            return this;
        }

        public Builder setExomiserPath(String exomiserPath) {
            this.exomiserPath = exomiserPath;
            return this;
        }

        public Builder setAnalysisDate(String analysisDate) {
            this.analysisDate = analysisDate;
            return this;
        }

        public Builder setSampleName(String sampleName) {
            this.sampleName = sampleName;
            return this;
        }

        public Builder setnGoodQualityVariants(long nGoodQualityVariants) {
            this.nGoodQualityVariants = nGoodQualityVariants;
            return this;
        }

        public Builder setnFilteredVariants(long nFilteredVariants) {
            this.nFilteredVariants = nFilteredVariants;
            return this;
        }

        public Builder setGenesWithVar(int genesWithVar) {
            this.genesWithVar = genesWithVar;
            return this;
        }

        public Builder setGlobalMode(boolean globalMode) {
            this.globalMode = globalMode;
            return this;
        }

        public AnalysisResultsMetadata build() {
            return new AnalysisResultsMetadata(liricalVersion,
                    hpoVersion,
                    transcriptDatabase,
                    liricalPath,
                    exomiserPath,
                    analysisDate,
                    sampleName,
                    nGoodQualityVariants,
                    nFilteredVariants,
                    genesWithVar,
                    globalMode);
        }
    }
}
