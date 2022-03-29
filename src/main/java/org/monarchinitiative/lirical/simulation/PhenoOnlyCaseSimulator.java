package org.monarchinitiative.lirical.simulation;

import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.HtmlTemplate;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.lirical.output.TsvTemplate;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.core.Disease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PhenoOnlyCaseSimulator {
    private static final Logger logger = LoggerFactory.getLogger(PhenoOnlyCaseSimulator.class);

    private final Path phenopacketPath;

    private final LiricalFactory factory;

    private final Disease simulatedDiagnosis;

    private final TermId simulatedDiseaseId;

    private final Ontology ontology;

    private final Map<TermId, HpoDisease> diseaseMap;

    private final List<TermId> hpoIdList;
    // List of excluded HPO terms in the subject.
    private final List<TermId> negatedHpoIdList;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lirical.output. */
    private final Map<String,String> metadata;


    private HpoCase hpocase=null;
    /** Rank of simulated disease */
    private int rank_of_disease;



    public PhenoOnlyCaseSimulator(Path phenopacketPath, LiricalFactory factory) {
        this.phenopacketPath = phenopacketPath;
        this.metadata = new HashMap<>();
        this.factory = factory;
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath);
        String sampleName = importer.getSampleId();
        Optional<Disease> diseaseDiagnosis = importer.getDiagnosis();
        if (diseaseDiagnosis.isEmpty())
            throw new LiricalRuntimeException("Disease diagnosis should not be empty here"); // TODO(pnr) is this true?
        simulatedDiagnosis = diseaseDiagnosis.get();
        String disId = simulatedDiagnosis.getTerm().getId(); // should be an ID such as OMIM:600102
        this.simulatedDiseaseId = TermId.of(disId);
        // TODO - sanitize with HpoTermSanitizer
        hpoIdList = importer.getHpoTerms();
        negatedHpoIdList = importer.getNegatedHpoTerms();
        this.ontology = factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", phenopacketPath.toAbsolutePath().toString());
        metadata.put("sample_name", sampleName);
        logger.trace("Running phenotype-only simulation from phenopacket {} ", phenopacketPath.toAbsolutePath());
        this.metadata.put("phenopacket.diagnosisId", simulatedDiseaseId.getValue());
        this.metadata.put("phenopacket.diagnosisLabel", simulatedDiagnosis.getTerm().getLabel());
    }


    public void run(){
        PhenotypeLikelihoodRatio phenoLr =  new PhenotypeLikelihoodRatio(ontology, diseaseMap);
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoIdList)
                .ontology(ontology)
                .negated(negatedHpoIdList)
                .diseaseMap(diseaseMap)
                .phenotypeLr(phenoLr);

        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        this.hpocase = evaluator.evaluate();


        Optional<Integer> optRank = this.hpocase .getRank(simulatedDiseaseId);
        if (optRank.isPresent()) {
            rank_of_disease = optRank.get();
            System.out.println(simulatedDiagnosis.getTerm().getLabel() + ": " + rank_of_disease);
        }
    }


    public void outputHtml(String prefix, double lrThreshold,int minDiff, Path outdir) {
        LiricalTemplate.Builder builder = LiricalTemplate.builder(hpocase,ontology,metadata)
                .threshold(factory.getLrThreshold())
                .mindiff(factory.getMinDifferentials())
                .outDirectory(outdir)
                .prefix(prefix);
        HtmlTemplate htemplate = builder.buildPhenotypeHtmlTemplate();
        htemplate.outputFile();
    }


    public void outputTsv(String prefix, double lrThreshold,int minDiff, Path outdir) {
        String outname=String.format("%s.tsv",prefix);
        LiricalTemplate.Builder builder = LiricalTemplate.builder(hpocase,ontology,metadata)
                .threshold(factory.getLrThreshold())
                .mindiff(factory.getMinDifferentials())
                .outDirectory(outdir)
                .prefix(prefix);
        TsvTemplate tsvtemplate = builder.buildPhenotypeTsvTemplate();
        tsvtemplate.outputFile(outname);

    }



    public String getDiagnosisLabel() {
        return this.simulatedDiagnosis.getTerm().getLabel();
    }

    public int getRank_of_disease() {
        return rank_of_disease;
    }


    public static String getHeader() {
        String [] fields = {"Phenopacket", "Diagnosis", "Diagnosis-ID", "Disease Rank"};
        return String.join("\t",fields);

    }

    public String getDetails() {
        return String.format("%s\t%s\t%s\t%d", phenopacketPath.toFile().getName(),
                simulatedDiagnosis.getTerm().getLabel(),
                simulatedDiagnosis.getTerm().getId(),
                rank_of_disease);
    }

}
