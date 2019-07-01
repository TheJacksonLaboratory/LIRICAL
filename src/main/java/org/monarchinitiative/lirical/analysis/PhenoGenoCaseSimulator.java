package org.monarchinitiative.lirical.analysis;

import com.google.common.collect.Multimap;
import org.json.simple.parser.ParseException;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.HtmlTemplate;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.lirical.output.TsvTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.core.Disease;
import org.phenopackets.schema.v1.core.HtsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PhenoGenoCaseSimulator {
    private static final Logger logger = LoggerFactory.getLogger(PhenoGenoCaseSimulator.class);

    private final File phenopacketFile;

    private final String templateVcfPath;

    private final LiricalFactory factory;

    private final GenomeAssembly genomeAssembly;

    private final Disease simulatedDiagnosis;

    private final TermId simulatedDiseaseId;

    private TermId simulatedDiseaseGene;

    private final String sampleName;

    private final Map<TermId, Gene2Genotype> genotypemap;

    private final Ontology ontology;

    private final Map<TermId, HpoDisease> diseaseMap;

    private final Multimap<TermId, TermId> disease2geneMultimap;

    private final  Multimap<TermId,TermId> gene2diseaseMultimap;

    private final  List<TermId> hpoIdList;
    // List of excluded HPO terms in the subject.
    private final List<TermId> negatedHpoIdList;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lirical.output. */
    private final Map<String,String> metadata;


    private HpoCase hpocase=null;
    /** Rank of simulated disease */
    private int rank_of_disease;
    /** Rank of simulated disease gene (best rank of any disease associated with the correct disease gene). */
    private int rank_of_gene;



    public PhenoGenoCaseSimulator(File phenopacket, String vcfpath, LiricalFactory factory) throws IOException, ParseException {
        phenopacketFile = phenopacket;
        templateVcfPath = vcfpath;
        this.metadata = new HashMap<>();
        this.factory = factory;
        this.genomeAssembly = factory.getAssembly();
        String phenopacketAbsolutePath = phenopacketFile.getAbsolutePath();
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketAbsolutePath,this.factory.hpoOntology());
        this.sampleName = importer.getSamplename();
        simulatedDiagnosis = importer.getDiagnosis();
        String disId = simulatedDiagnosis.getTerm().getId(); // should be an ID such as OMIM:600102
        this.simulatedDiseaseId = TermId.of(disId);
        hpoIdList = importer.getHpoTerms();
        negatedHpoIdList = importer.getNegatedHpoTerms();

        VcfSimulator vcfSimulator = new VcfSimulator(Paths.get(this.templateVcfPath));
        HtsFile simulatedVcf;
        try {
            simulatedVcf = vcfSimulator.simulateVcf(importer.getSamplename(), importer.getVariantList(), genomeAssembly.toString());
            //pp = pp.toBuilder().clearHtsFiles().addHtsFiles(htsFile).build();
        } catch (IOException e) {
            throw new LiricalRuntimeException("Could not simulate VCF for phenopacket");
        }
        String vcfPath = simulatedVcf.getFile().getPath();
        this.metadata.put("vcf_file", vcfPath);
        this.genotypemap = factory.getGene2GenotypeMap(vcfPath);
        this.ontology = factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        this.disease2geneMultimap = factory.disease2geneMultimap();
        this.gene2diseaseMultimap = factory.gene2diseaseMultimap();




        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", phenopacketAbsolutePath);
        metadata.put("sample_name", this.sampleName);
        if (!importer.qcPhenopacket() ){
            System.err.println("[ERROR] Could not simulate VCF for "+phenopacketFile.getName());
            return;
        }
        this.simulatedDiseaseGene = TermId.of(importer.getGene());
        logger.trace("Running simulation from phenopacket {} with template VCF {}",
                phenopacketFile.getAbsolutePath(),
                vcfPath);


       this.metadata.put("phenopacket.diagnosisId", simulatedDiseaseId.getValue());
       this.metadata.put("phenopacket.diagnosisLabel", simulatedDiagnosis.getTerm().getLabel());
    }



    /**
     * This method coordinates
     */
    public void run()  {


        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
        PhenotypeLikelihoodRatio phenoLr =  new PhenotypeLikelihoodRatio(ontology, diseaseMap);


        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoIdList)
                .ontology(factory.hpoOntology())
                .negated(negatedHpoIdList)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypemap)
                .phenotypeLr(phenoLr)
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        this.hpocase = evaluator.evaluate();

        Optional<Integer> optRank = this.hpocase.getRank(simulatedDiseaseId);
        this.rank_of_disease = optRank.orElseGet(() -> this.hpocase.getRankOfUnrankedDisease());
        this.rank_of_gene = this.rank_of_disease;
        for (TermId diseaseId : this.gene2diseaseMultimap.get(this.simulatedDiseaseGene)) {
            optRank = this.hpocase.getRank(diseaseId);
            int r = optRank.orElseGet(() -> this.hpocase.getRankOfUnrankedDisease());
            if (r < rank_of_gene) {
                rank_of_gene = r;
            }
        }



        System.out.println(simulatedDiagnosis.getTerm().getLabel() + ": " + rank_of_disease + " (disease rank)");
        System.out.println(simulatedDiagnosis.getTerm().getLabel() + ": " + rank_of_gene + " (gene rank)");


        this.metadata.put("genesWithVar", String.valueOf(genotypemap.size()));
        this.metadata.put("exomiserPath", factory.getExomiserPath());
        this.metadata.put("hpoVersion", factory.getHpoVersion());

    }


    public void outputHtml(String prefix, double lrThreshold,int minDiff, String outdir) {
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hpocase,ontology,metadata)
                .genotypeMap(genotypemap)
                .geneid2symMap(factory.geneId2symbolMap())
                .threshold(lrThreshold)
                .mindiff(minDiff)
                .outdirectory(outdir)
                .prefix(prefix);
        HtmlTemplate htemplate = builder.buildGenoPhenoHtmlTemplate();
        htemplate.outputFile();
    }


    public void outputTsv(String prefix, double lrThreshold,int minDiff, String outdir) {
        String outname=String.format("%s.tsv",prefix);
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(this.hpocase,ontology,metadata)
                .genotypeMap(genotypemap)
                .geneid2symMap(factory.geneId2symbolMap())
                .threshold(lrThreshold)
                .mindiff(minDiff)
                .outdirectory(outdir)
                .prefix(prefix);
        TsvTemplate tsvtemplate = builder.buildGenoPhenoTsvTemplate();
        tsvtemplate.outputFile(outname);

    }


    public String getDiagnosisLabel() {
        return this.simulatedDiagnosis.getTerm().getLabel();
    }

    public int getRank_of_disease() {
        return rank_of_disease;
    }

    public int getRank_of_gene() {
        return rank_of_gene;
    }
}
