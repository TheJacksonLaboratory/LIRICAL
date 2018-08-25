package org.monarchinitiative.lr2pg.vcf;

import de.charite.compbio.jannovar.data.JannovarData;

public class VcfParser {

    private final String vcfPath;
    private final JannovarData jannovarData;


    public VcfParser(String path, JannovarData jdata) {
        this.vcfPath=path;
        this.jannovarData=jdata;
    }

    public void parse(String path) {

    }

}
