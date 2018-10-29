package org.monarchinitiative.lr2pg.cmd;

import com.google.common.collect.Multimap;
import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.analysis.Vcf2GenotypeMap;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class VcfCommand extends Lr2PgCommand {
    private static final Logger logger = LoggerFactory.getLogger(VcfCommand.class);
    /** An object that contains parameters from the YAML file for configuration. */
    private final Lr2PgFactory factory;
    /** Directory where various files are downloaded/created. */
    private final String datadir;
    /** Default name of the background frequency file. */
    private final String BACKGROUND_FREQUENCY_FILE="background-freq.txt";

    /**
     * @param fact An object that contains parameters from the YAML file for configuration
     * @param data Path to the data download directory that has hp.obo and other files.
     */
    public VcfCommand(Lr2PgFactory fact, String data) {
        this.factory = fact;
        this.datadir=data;
    }

    /**
     * Identify the variants and genotypes from the VCF file.
     * @return a map with key: An NCBI Gene Id, and value: corresponding {@link Gene2Genotype} object.
     * @throws Lr2pgException upon error parsing the VCF file or creating the Jannovar object
     */
    private Map<TermId, Gene2Genotype> getVcf2GenotypeMap() throws Lr2pgException {
        String vcf = factory.vcfPath();
        MVStore mvstore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcf, jannovarData, mvstore, GenomeAssembly.HG19);
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        return genotypeMap;
    }

    private GenotypeLikelihoodRatio getGenotypeLR() throws Lr2pgException {
        String backgroundFile = String.format("%s%s%s",datadir, File.separator,BACKGROUND_FREQUENCY_FILE);
        File f = new File(backgroundFile);
        if (!f.exists()) {
            throw new Lr2pgException(String.format("Could not find %s",BACKGROUND_FREQUENCY_FILE));
        }
        GenotypeDataIngestor ingestor = new GenotypeDataIngestor(backgroundFile);
        Map<TermId,Double> gene2back = ingestor.parse();
        return new GenotypeLikelihoodRatio(gene2back);
    }



    public void run() throws Lr2pgException {
        Map<TermId, Gene2Genotype> genotypeMap = getVcf2GenotypeMap();
        //debugPrintGenotypeMap(genotypeMap);
        GenotypeLikelihoodRatio genoLr = getGenotypeLR();
        List<TermId> observedHpoTerms = factory.observedHpoTerms();
        HpoOntology ontology = factory.hpoOntology();
        Map<TermId,HpoDisease> diseaseMap = factory.diseaseMap(ontology);

        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        Multimap<TermId,TermId> disease2geneMultimap = factory.disease2geneMultimap();
        Map<TermId,String> geneId2symbol = factory.geneId2symbolMap();
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(observedHpoTerms)
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypeMap)
                .phenotypeLr(phenoLr)
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        HpoCase hcase = evaluator.evaluate();
        hcase.outputTopResults(5,ontology,geneId2symbol);
    }




    private void debugPrintGenotypeMap(Map<TermId, Gene2Genotype> genotypeMap) {
        logger.error("debug print");
        int i=0;
        int N=genotypeMap.size();
        for (TermId geneId : genotypeMap.keySet()) {
            Gene2Genotype g2g = genotypeMap.get(geneId);
            double path = g2g.getSumOfPathBinScores();
            String symbol = g2g.getSymbol();
            String s = String.format("%s [%s] path: %.3f",symbol,geneId.getIdWithPrefix(),path);
            if (g2g.hasPredictedPathogenicVar()) {
                System.out.println(++i +"/"+N+") "+s);
            }
        }
    }
}
