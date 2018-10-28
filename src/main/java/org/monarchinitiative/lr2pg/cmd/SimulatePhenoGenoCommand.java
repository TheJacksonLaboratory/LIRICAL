package org.monarchinitiative.lr2pg.cmd;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpOboParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulatePhenoGenoCommand extends Lr2PgCommand {

    private final String datadir;
    private final String hpoOboPath;
    private final String phenotypeAnnotationPath;

    private final String BACKGROUND_FREQUENCY_FILE="background-freq.txt";



    public SimulatePhenoGenoCommand(String data) {
        this.datadir=data;
        File dirpath = new File(datadir);
        String absDirPath=dirpath.getAbsolutePath();
        this.hpoOboPath=String.format("%s%s%s",absDirPath,File.separator,"hp.obo");
        this.phenotypeAnnotationPath=String.format("%s%s%s",absDirPath,File.separator,"phenotype.hpoa");
    }

    public List<TermId> getRandomTermList(HpoDisease disease) {
        List<HpoAnnotation> annots = disease.getPhenotypicAbnormalities();
        List<TermId> terms=new ArrayList<>();
        for (HpoAnnotation ann : annots) {
            double r = Math.random();
            if (r > 0.6) {
                terms.add(ann.getTermId());
            }
        }
        return terms;
    }

    private  Map<TermId,Double> getGenotypeLR() throws Lr2pgException {
        String backgroundFile = String.format("%s%s%s",datadir, File.separator,BACKGROUND_FREQUENCY_FILE);
        File f = new File(backgroundFile);
        if (!f.exists()) {
            throw new Lr2pgException(String.format("Could not find %s",BACKGROUND_FREQUENCY_FILE));
        }
        GenotypeDataIngestor ingestor = new GenotypeDataIngestor(backgroundFile);
        return ingestor.parse();
        //return new GenotypeLikelihoodRatio(gene2back);

    }


    /**
     * TODO -- parse termId2gene symbol with HpoAssociation REFACGTOR
     * @throws Lr2pgException
     */
    public void run() throws Lr2pgException {
        HpoOntology ontology = initializeOntology();
        Map<TermId, HpoDisease> diseasemap =  parseHpoAnnotations(ontology);
        String geneSymbol="fake"; // todo
        Integer variantCount=42;//Integer.parseInt(varcount);
        Double meanVarPathogenicity=0.42;//Double.parseDouble(varpath);
        //Multimap<TermId, TermId> disease2geneMultimap,
        //List<TermId> termIdList,
        //Map<TermId, Double> backgroundfreq
        TermId diseaseCurie = TermId.constructWithPrefix("OMIM:154700");
        List<TermId> termlist = getRandomTermList(diseasemap.get(diseaseCurie));
        Map<TermId,Double> glr = getGenotypeLR();
         Multimap<TermId,TermId> disease2geneMultimap = ArrayListMultimap.create();
            HpoPhenoGenoCaseSimulator simulator = new

          HpoPhenoGenoCaseSimulator(ontology,
                    diseasemap,
                    disease2geneMultimap,
                    geneSymbol,
                    variantCount,
                    meanVarPathogenicity,
                    termlist,
                    glr);

        HpoCase hpocase = simulator.evaluateCase();
        System.err.println(hpocase.toString());
        HpoDisease disease = diseasemap.get(diseaseCurie);
        String diseaseName = disease.getName();
        simulator.outputSvg(diseaseCurie, diseaseName, ontology, null);
        System.err.println(simulator.toString());

    }

    /** Initialize the HpoOnotlogy object from the hp.obo file. */
    protected HpoOntology initializeOntology() throws Lr2pgException{
        try {
            HpOboParser parser = new HpOboParser(new File(this.hpoOboPath));
            HpoOntology ontology = parser.parse();
            return ontology;
        } catch (PhenolException | FileNotFoundException ioe) {
            throw new Lr2pgException("Could not parse hp.obo file: " + ioe.getMessage());
        }
    }



    protected Map<TermId, HpoDisease> parseHpoAnnotations(HpoOntology ontology) throws Lr2pgException {
        if (ontology==null) {
            throw new Lr2pgException("HpoOntology object not intitialized");
        }
        HpoDiseaseAnnotationParser parser = new HpoDiseaseAnnotationParser(phenotypeAnnotationPath,ontology);
        try {
            return parser.parse();
        } catch (PhenolException pe ) {
            throw new Lr2pgException("Could not parse disease associations: " + pe.getMessage());
        }
    }

    /*
    HpoPhenoGenoCaseSimulator hpoPhenoGenoCaseSimulator(HpoOntology ontology,
                                                        Map<TermId, HpoDisease> diseaseMap,
                                                        Multimap<TermId, TermId> disease2geneMultimap,
                                                        List<TermId> termIdList,
                                                         Map<TermId, Double> backgroundfreq
                                                        ){


       return new HpoPhenoGenoCaseSimulator(ontology,
                diseaseMap,
                disease2geneMultimap,
                geneSymbol,
                variantCount,
                meanVarPathogenicity,
                termIdList,
                backgroundfreq);
    }

     */
}
