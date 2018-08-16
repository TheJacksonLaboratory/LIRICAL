package org.monarchinitiative.lr2pg.command;

import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.io.Disease2GeneDataIngestor;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.lr2pg.io.HpoDataIngestor;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulatePhenoGenoCaseCommand implements Command {
    private static final Logger logger = LogManager.getLogger();
    /**
     * Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    private final String dataDirectoryPath;

    private final String geneSymbol;

    private final int variantCount;

    private final double meanVariantPathogenicity;

    private final TermId diseaseCurie;
    /** List of HPO terms representing phenoytpic abnormalities. */
    private final List<TermId> hpoTerms;

    private final String backgroundFreqPath;


    /**
     * @param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    public SimulatePhenoGenoCaseCommand(String datadir,
                                        String gene,
                                        int varcount,
                                        double varpath,
                                        String disease,
                                        String HpoTermList,
                                        String backgroundFreq) {
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
        this.backgroundFreqPath=backgroundFreq;
    }

    public void execute() {
        logger.trace("Executing HpoCase simulation");
        HpoDataIngestor ingestor = new HpoDataIngestor(this.dataDirectoryPath);
        HpoOntology ontology=ingestor.getOntology();
        Map<TermId,HpoDisease> diseaseMap=ingestor.getDiseaseMap();
        Disease2GeneDataIngestor d2gIngestor = new Disease2GeneDataIngestor(this.dataDirectoryPath, ontology);
        Map<TermId, String> geneId2SymbolMap = d2gIngestor.getGeneId2SymbolMap();
        Multimap<TermId,TermId> disease2geneMultimap=d2gIngestor.getDisease2geneMultimap();
        GenotypeDataIngestor gdingestor = new GenotypeDataIngestor(backgroundFreqPath);
        Map<TermId,Double> gene2backgroundFrequency= gdingestor.parse();
        HpoPhenoGenoCaseSimulator simulator = new HpoPhenoGenoCaseSimulator(ontology,
                diseaseMap,
                disease2geneMultimap,
                geneSymbol,
                variantCount,
                meanVariantPathogenicity,
                hpoTerms,
                gene2backgroundFrequency);

        HpoCase hpocase = simulator.evaluateCase();
        HpoDisease disease = diseaseMap.get(diseaseCurie);
        String diseaseName = disease.getName();
        simulator.outputSvg(diseaseCurie,diseaseName,ontology, geneId2SymbolMap);
    }



}
