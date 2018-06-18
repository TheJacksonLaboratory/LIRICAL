package org.monarchinitiative.lr2pg.command;

import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.io.Disease2GeneDataIngestor;
import org.monarchinitiative.lr2pg.io.HpoDataIngestor;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulatePhenoGeneCaseCommand implements Command {
    private static final Logger logger = LogManager.getLogger();
    /**
     * Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    private final String dataDirectoryPath;

    private final String geneSymbol;

    private final int variantCount;

    private final double meanVariantPathogenicity;

    private TermId diseaseCurie;
    /** List of HPO terms representing phenoytpic abnormalities. */
    private final List<TermId> hpoTerms;


    /**
     * @param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    public SimulatePhenoGeneCaseCommand(String datadir, String gene, int varcount, double varpath, String disease, String HpoTermList) {
        dataDirectoryPath = datadir;
        this.geneSymbol = gene;
        this.variantCount = varcount;
        this.meanVariantPathogenicity = varpath;
        this.diseaseCurie=TermId.constructWithPrefix(disease);
        String a[] = HpoTermList.split(",");
        hpoTerms=new ArrayList<>();
        for (String hpo : a) {
            TermId tid = TermId.constructWithPrefix(hpo);
            hpoTerms.add(tid);
        }
    }

    public void execute() {
        logger.trace("Executing HpoCase simulation");
        HpoDataIngestor ingestor = new HpoDataIngestor(this.dataDirectoryPath);
        HpoOntology ontology=ingestor.getOntology();
        Map<TermId,HpoDisease> diseaseMap=ingestor.getDiseaseMap();
        Disease2GeneDataIngestor d2gIngestor = new Disease2GeneDataIngestor(this.dataDirectoryPath);
        Multimap<TermId,TermId> gene2diseaseMultimap=d2gIngestor.getGene2diseaseMultimap();
        HpoPhenoGenoCaseSimulator simulator = new HpoPhenoGenoCaseSimulator(ontology,
                diseaseMap,
                gene2diseaseMultimap,
                geneSymbol,
                variantCount,
                meanVariantPathogenicity,
                hpoTerms);
        simulator.debugPrint();
        try {
            simulator.evaluateCase(diseaseCurie);
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }
    }

}
