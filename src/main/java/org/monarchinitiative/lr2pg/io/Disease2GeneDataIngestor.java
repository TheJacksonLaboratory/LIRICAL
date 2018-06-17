package org.monarchinitiative.lr2pg.io;

import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.phenol.io.gene2phen.HpoDisease2GeneParser;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;

public class Disease2GeneDataIngestor {
    private static final Logger logger = LogManager.getLogger();
    /** The directory (default: "data") where we have downloaded hp.obo and phenotype.hpoa. */
    private final String dataDirectoryPath;

    private final String GENE_INFO_FILENAME="Homo_sapiens_gene_info.gz";
    private final String MIM2GENE_MEDGEN_FILENAME="mim2gene_medgen";
    /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
    private Multimap<TermId,TermId> gene2diseaseMultimap;

    public Disease2GeneDataIngestor(String datadir) {
        this.dataDirectoryPath=datadir;
        inputData();
    }

    private void inputData() {
        String geneinfopath=String.format("%s%s%s",
                this.dataDirectoryPath,
                File.separator,
                GENE_INFO_FILENAME);
        String mim2gene_medgenPath=String.format("%s%s%s",
                this.dataDirectoryPath,
                File.separator,
                MIM2GENE_MEDGEN_FILENAME);
        //Homo_sapiens_gene_info.gz
        HpoDisease2GeneParser parser = new HpoDisease2GeneParser(geneinfopath,mim2gene_medgenPath);
        this.gene2diseaseMultimap = parser.getGene2DiseaseIdMap();
    }

    public Multimap<TermId,TermId> getGene2diseaseMultimap() {
        return this.gene2diseaseMultimap;
    }

}
