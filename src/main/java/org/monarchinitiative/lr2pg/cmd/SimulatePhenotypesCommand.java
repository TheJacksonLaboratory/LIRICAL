package org.monarchinitiative.lr2pg.cmd;

import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpOboParser;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

public class SimulatePhenotypesCommand extends Lr2PgCommand {

    private final String hpoOboPath;
    /** path to phenotype.hpoa file. */
    private final String phenotypeAnnotationPath;

    private final String geneInfoPath;

    private final String mim2genemedgenPath;

    private final int DEFAULT_CASES_TO_SIMULATE=25;
    private final int DEFAULT_TERMS_PER_CASE=5;
    private final int DEFAULT_NOISE_TERMS=1;
    private final boolean DEFAULT_IMPRECISION=false;

    private int n_cases_to_simulate = DEFAULT_CASES_TO_SIMULATE;
    private int n_terms_per_case = DEFAULT_TERMS_PER_CASE;
    private int n_noise_terms = DEFAULT_NOISE_TERMS;
    private boolean imprecise_phenotype = DEFAULT_IMPRECISION;



    public SimulatePhenotypesCommand(String dataDirPath){
        File dirpath = new File(dataDirPath);
        String absDirPath=dirpath.getAbsolutePath();
        this.hpoOboPath=String.format("%s%s%s",absDirPath,File.separator,"hp.obo");
        this.phenotypeAnnotationPath=String.format("%s%s%s",absDirPath,File.separator,"phenotype.hpoa");
        this.geneInfoPath=String.format("%s%s%s",absDirPath,File.separator,"Homo_sapiens_gene_info.gz");
        this.mim2genemedgenPath=String.format("%s%s%s",absDirPath,File.separator,"mim2gene_medgen");
    }

    /** Initialize the HpoOnotlogy object from the hp.obo file. */
    private HpoOntology initializeOntology() throws Lr2pgException{
        try {
            HpOboParser parser = new HpOboParser(new File(this.hpoOboPath));
            HpoOntology ontology = parser.parse();
            return ontology;
        } catch (PhenolException | FileNotFoundException ioe) {
            throw new Lr2pgException("Could not parse hp.obo file: " + ioe.getMessage());
        }
    }



     private Map<TermId, HpoDisease> parseHpoAnnotations(HpoOntology ontology) throws Lr2pgException {
        if (ontology==null) {
            throw new Lr2pgException("HpoOntology object not intitialized");
        }
        if (this.geneInfoPath==null) {
            throw new Lr2pgException("Path to Homo_sapiens_gene_info.gz file not found");
        }
        if (this.mim2genemedgenPath==null) {
            throw new Lr2pgException("Path to mim2genemedgen file not found");
        }

        File geneInfoFile = new File(geneInfoPath);
        if (!geneInfoFile.exists()) {
            throw new Lr2pgException("Could not find gene info file at " + geneInfoPath + ". Run download!");
        }
        File mim2genemedgenFile = new File(this.mim2genemedgenPath);
        if (!mim2genemedgenFile.exists()) {
            System.err.println("Could not find medgen file at " + this.mim2genemedgenPath + ". Run download!");
            System.exit(1);
        }
        File orphafilePlaceholder = null;//we do not need this for now
        HpoAssociationParser assocParser = new HpoAssociationParser(geneInfoFile,
                mim2genemedgenFile,
                orphafilePlaceholder,
                ontology);
        assocParser.parse();
        assocParser.getDiseaseToGeneIdMap();

        return assocParser.getTermToDisease();
    }



    public void run() throws Lr2pgException {
        HpoOntology ontology = initializeOntology();
        Map<TermId, HpoDisease> diseaseMap = parseHpoAnnotations(ontology);
        PhenotypeOnlyHpoCaseSimulator phenotypeOnlyHpoCaseSimulator = new PhenotypeOnlyHpoCaseSimulator(ontology,
                diseaseMap,
                n_cases_to_simulate,
                n_terms_per_case,
                n_noise_terms,
                imprecise_phenotype);

        phenotypeOnlyHpoCaseSimulator.simulateCases();

    }
}
