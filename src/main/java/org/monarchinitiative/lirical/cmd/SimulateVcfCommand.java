package org.monarchinitiative.lirical.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.analysis.VcfSimulator;
import org.monarchinitiative.lirical.configuration.Lr2PgFactory;
import org.monarchinitiative.lirical.exception.Lr2PgRuntimeException;
import org.monarchinitiative.lirical.exception.Lr2pgException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.HtmlTemplate;
import org.monarchinitiative.lirical.output.Lr2pgTemplate;
import org.monarchinitiative.lirical.output.TsvTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.Disease;
import org.phenopackets.schema.v1.core.HtsFile;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Phenotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Parameters(commandDescription = "Simulate VCF analysis from phenopacket",hidden = false)
public class SimulateVcfCommand  extends PrioritizeCommand {
    private static final Logger logger = LoggerFactory.getLogger(SimulateVcfCommand.class);


    @Parameter(names = {"-p","--phenopacket"}, description = "path to phenopacket file", required = true)
    private String phenopacketPath;
    @Parameter(names={"-e","--exomiser"}, description = "path to the Exomiser data directory")
    private String exomiserDataDirectory;
    @Parameter(names = {"-v", "--template-vcf"}, description = "path to template VCF file", required = true)
    private String templateVcfPath;
    @Parameter(names={"-b","--background"}, description = "path to non-default background frequency file")
    private String backgroundFrequencyFile;

    /** If true, the phenopacket contains the path of a VCF file. */
    private boolean hasVcf;

    private String simulatedDisease=null;

    /** No-op constructor meant to demo the phenotype LR2PG algorithm by simulating some case based on
     * a phenopacket and a "normal" VCF file.
     */
    public SimulateVcfCommand(){
    }

    @Override
    public void run() throws Lr2pgException {
        this.metadata=new HashMap<>();
        Phenopacket pp  = readPhenopacket(phenopacketPath);
        //TODO -- Get this from the VCF file or from the Phenopacket or from command line
        String genomeAssembly = "GRCh37";

        VcfSimulator vcfSimulator = new VcfSimulator(Paths.get(templateVcfPath));
        try {
            HtsFile htsFile = vcfSimulator.simulateVcf(pp.getSubject().getId(), pp.getVariantsList(), genomeAssembly);
            pp = pp.toBuilder().clearHtsFiles().addHtsFiles(htsFile).build();
        } catch (IOException e) {
            throw new Lr2PgRuntimeException("Could not simulate VCF for phenopacket");
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", this.phenopacketPath);
        metadata.put("sample_name", pp.getSubject().getId());
        hasVcf = pp.getHtsFilesList().stream().anyMatch(hf -> hf.getHtsFormat().equals(HtsFile.HtsFormat.VCF));
        if (pp.getDiseasesCount()!=1) {
            System.err.println("[ERROR] to run this simulation a phenoopacket must have exactly one disease diagnosis");
            System.err.println("[ERROR] terminating program because " + pp.getSubject().getId() + " had " + pp.getDiseasesCount());
            System.exit(1);
        }
        Disease diagnosis = pp.getDiseases(0);
        simulatedDisease = diagnosis.getTerm().getId(); // should be an ID such as OMIM:600102
        this.metadata.put("phenopacket.diagnosisId",simulatedDisease);
        this.metadata.put("phenopacket.diagnosisLabel",diagnosis.getTerm().getLabel());

        if (hasVcf) {
            HtsFile htsFile = pp.getHtsFilesList().stream()
                    .filter(hf -> hf.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                    .findFirst()
                    .orElseThrow(() -> new Lr2PgRuntimeException("Phenopacket has and has not VCF file in the same time... \uD83D\uDE15"));
            String vcfPath = htsFile.getFile().getPath();
           // this.metadata.put("vcf_file", this.vcfPath);

            logger.trace("Running simulation from phenopacket {} with template VCF {}");
             List<TermId> hpoIdList=pp.getPhenotypesList() // copied from PhenopacketImporter
                     .stream()
                     .distinct()
                     .filter(((Predicate<Phenotype>) Phenotype::getNegated).negate()) // i.e., just take non-negated phenotypes
                     .map(Phenotype::getType)
                     .map(OntologyClass::getId)
                     .map(TermId::of)
                     .collect(ImmutableList.toImmutableList());
            // List of excluded HPO terms in the subject.
             List<TermId> negatedHpoIdList= pp.getPhenotypesList() // copied from PhenopacketImporter
                     .stream()
                     .filter(Phenotype::getNegated) // i.e., just take negated phenotypes
                     .map(Phenotype::getType)
                     .map(OntologyClass::getId)
                     .map(TermId::of)
                     .collect(ImmutableList.toImmutableList());
            Lr2PgFactory factory = new Lr2PgFactory.Builder()
                    .datadir(this.datadir)
                    .genomeAssembly(genomeAssembly)
                    .exomiser(this.exomiserDataDirectory)
                    .vcf(vcfPath)
                    .transcriptdatabase(this.transcriptDb)
                    .backgroundFrequency(this.backgroundFrequencyFile)
                    .build();
            factory.qcHumanPhenotypeOntologyFiles();
            factory.qcExternalFilesInDataDir();
            factory.qcExomiserFiles();
            factory.qcGenomeBuild();
            factory.qcVcfFile();

            Map<TermId, Gene2Genotype> genotypemap = factory.getGene2GenotypeMap();

            GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
            Ontology ontology = factory.hpoOntology();
            Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
            PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
            Multimap<TermId, TermId> disease2geneMultimap = factory.disease2geneMultimap();
            Map<TermId, String> geneId2symbol = factory.geneId2symbolMap();
            CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoIdList)
                    .ontology(ontology)
                    .negated(negatedHpoIdList)
                    .diseaseMap(diseaseMap)
                    .disease2geneMultimap(disease2geneMultimap)
                    .genotypeMap(genotypemap)
                    .phenotypeLr(phenoLr)
                    .genotypeLr(genoLr);

            CaseEvaluator evaluator = caseBuilder.build();
            HpoCase hcase = evaluator.evaluate();

            String outdir=".";
            int n_genes_with_var=factory.getGene2GenotypeMap().size();
            this.metadata.put("genesWithVar",String.valueOf(n_genes_with_var));
            this.metadata.put("exomiserPath",factory.getExomiserPath());
            this.metadata.put("hpoVersion",factory.getHpoVersion());


            if (outputTSV) {
                Lr2pgTemplate template = new TsvTemplate(hcase,ontology,genotypemap,this.geneId2symbol,this.metadata);
                template.outputFile(this.outfilePrefix,outdir);
                String outname=String.format("%s.tsv",outfilePrefix );
                int rank = extractRank(outname);
            } else {
                HtmlTemplate caseoutput = new HtmlTemplate(hcase,
                        ontology,
                        genotypemap,
                        this.geneId2symbol,
                        this.metadata,
                        this.LR_THRESHOLD,
                        minDifferentialsToShow);
                caseoutput.outputFile(this.outfilePrefix, outdir);
            }
        }
    }


    private static Phenopacket readPhenopacket(String phenopacketPath) {
        Path ppPath = Paths.get(phenopacketPath);
        Phenopacket.Builder ppBuilder = Phenopacket.newBuilder();
        try (BufferedReader reader = Files.newBufferedReader(ppPath)) {
            JsonFormat.parser().merge(reader, ppBuilder);
        } catch (IOException e) {
            logger.warn("Unable to read/decode file '{}'", ppPath);
            throw new Lr2PgRuntimeException(String.format("Unable to read/decode file '%s'", ppPath));
        }
        return ppBuilder.build();
    }


    private int extractRank(String path) {
        int rank=-1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line=br.readLine())!= null) {
                if (line.startsWith("!")) continue;
                if (line.startsWith("rank")) continue;
                String []fields = line.split("\t");
                rank = Integer.parseInt(fields[0]);
                String diseaseName = fields[1];
                String diseaseCurie = fields[2];
                if (diseaseCurie.equals(this.simulatedDisease)) {
                    logger.info("Got rank of {} for simulated disease {}", rank,simulatedDisease);
                }
                return rank;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rank;
    }


}
